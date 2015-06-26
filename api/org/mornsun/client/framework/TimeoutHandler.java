/**
 * 
 */
package org.mornsun.client.framework;

import static org.jboss.netty.channel.Channels.fireExceptionCaught;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.LifeCycleAwareChannelHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.WriteTimeoutException;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

/**
 * @author Chauncey
 *
 */
@Sharable
public class TimeoutHandler extends SimpleChannelHandler
							implements LifeCycleAwareChannelHandler,
										ExternalResourceReleasable 
{
    public static final WriteTimeoutException WRITE_EXCEPTION = new WriteTimeoutException();
    public static final ReadTimeoutException READ_EXCEPTION = new ReadTimeoutException();

	protected static final Timer g_timer = new HashedWheelTimer(); // Reused in PROCESS scale

	protected long m_writeTimeout;
	protected long m_readTimeout;

    /**
     * Creates a new instance.
     *
     * @param timer
     *        the {@link Timer} that is used to trigger the scheduled event.
     *        The recommended {@link Timer} implementation is {@link HashedWheelTimer}.
     * @param timeoutMilliseconds
     *        read timeout in milliseconds
     */
    public TimeoutHandler(long writeTimeout, long readTimeout)
    {
    	m_writeTimeout = writeTimeout;
    	m_readTimeout = readTimeout;
    }
    
    /**
     * 
     * @param writeTimeout
     * @param readTimeout
     */
    public void setTimeout(long writeTimeout, long readTimeout)
    {
    	m_writeTimeout = writeTimeout;
    	m_readTimeout = readTimeout;
    }

    /**
     * Stops the {@link Timer} which was specified in the constructor of this
     * handler.  You should not call this method if the {@link Timer} is in use
     * by other objects.
     */
    public void releaseExternalResources()
    {
        g_timer.stop();
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.LifeCycleAwareChannelHandler#beforeAdd(org.jboss.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception
    {
        // NOOP
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.LifeCycleAwareChannelHandler#afterAdd(org.jboss.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void afterAdd(ChannelHandlerContext ctx) throws Exception
    {
        // NOOP
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.LifeCycleAwareChannelHandler#beforeRemove(org.jboss.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void beforeRemove(ChannelHandlerContext ctx) throws Exception
    {
        destroy(ctx);
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.LifeCycleAwareChannelHandler#afterRemove(org.jboss.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void afterRemove(ChannelHandlerContext ctx) throws Exception
    {
        // NOOP
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception
    {
        ctx.sendUpstream(e);
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception
    {
        destroy(ctx);
        ctx.sendUpstream(e);
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception
    {
    	restore(ctx);
        ctx.sendUpstream(e);
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {
    	startWrite(ctx); // initialize write timeout
        ctx.sendDownstream(e);
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelHandler#writeComplete(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.WriteCompletionEvent)
     */
    @Override
    public void writeComplete(
            ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception
    {
        startRead(ctx);
        ctx.sendUpstream(e);
    }

    /**
     * 
     * @param ctx
     */
    private void startRead(ChannelHandlerContext ctx)
    {
        State state = state(ctx);

        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        synchronized (state) {
        	if (1 == state.state) { // If initialized
        		state.state = 2; // Set to sent
        	}
        }
        switch (state.state) {
        case 2:
            if (state.timeout != null) {
                state.timeout.cancel();
                state.timeout = null;
            }
            if (m_readTimeout > 0) {
                state.timeout = g_timer.newTimeout(new TimeoutTask(ctx), m_readTimeout, TimeUnit.MILLISECONDS);
            }
            break;
        case 3: // It is normal since the state is labeled destroyed after a message probably is received, 
        	return;
    	default:
    		fireExceptionCaught(ctx, new Exception("state error:" + state.state));
    		break;
        }
    }

    /**
     * 
     * @param ctx
     */
    private void startWrite(ChannelHandlerContext ctx)
    {
        State state = state(ctx);

        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        synchronized (state) {
        	if (0 == state.state) { // Set to none
        		state.state = 1;
        	}
        }
        switch (state.state) {
        case 1:
            if (m_writeTimeout > 0) {
                state.timeout = g_timer.newTimeout(new TimeoutTask(ctx), m_writeTimeout, TimeUnit.MILLISECONDS);
            }
            break;
        case 3: // It is normal since the state is labeled destroyed after a message probably is received, 
        	return;
    	default:
    		fireExceptionCaught(ctx, new Exception("state error:" + state.state));
    		break;
        }
    }

    /**
     * 
     * @param ctx
     */
    private void destroy(ChannelHandlerContext ctx)
    {
        State state = state(ctx);
        synchronized (state) {
            if (state.state == 0) { // none
                return;
            }
            state.state = 3; // destroyed
        }

        if (state.timeout != null) {
            state.timeout.cancel();
            state.timeout = null;
        }
    }

    /**
     * 
     * @param ctx
     */
    private void restore(ChannelHandlerContext ctx)
    {
        State state = state(ctx);
        synchronized (state) {
            if (state.state == 0) { // none
                return;
            }
            state.state = 0; // restored
        }

        if (state.timeout != null) {
            state.timeout.cancel();
            state.timeout = null;
        }
    }
    
    /**
     * 
     * @param ctx
     * @return
     */
    private static State state(ChannelHandlerContext ctx)
    {
        State state;
        synchronized (ctx) {
            // FIXME: It could have been better if there is setAttachmentIfAbsent().
            state = (State) ctx.getAttachment();
            if (state != null) {
                return state;
            }
            state = new State();
            ctx.setAttachment(state);
        }
        return state;
    }

    /**
     * 
     * @param ctx
     * @throws Exception
     */
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception
    {
        fireExceptionCaught(ctx, READ_EXCEPTION);
    }

    /**
     * 
     * @param ctx
     * @throws Exception
     */
    protected void writeTimedOut(ChannelHandlerContext ctx) throws Exception
    {
        fireExceptionCaught(ctx, WRITE_EXCEPTION);
    }
    
    /**
     * 
     * @author Chauncey
     *
     */
    private final class TimeoutTask implements TimerTask
    {
        private final ChannelHandlerContext ctx;

        /**
         * 
         * @param ctx
         */
        TimeoutTask(ChannelHandlerContext ctx)
        {
            this.ctx = ctx;
        }

        /*
         * (non-Javadoc)
         * @see org.jboss.netty.util.TimerTask#run(org.jboss.netty.util.Timeout)
         */
        @Override
        public void run(Timeout timeout) throws Exception
        {
            if (timeout.isCancelled()) {
                return;
            }
            if (!ctx.getChannel().isOpen()) {
                return;
            }
            fireTimedOut(ctx);
        }

        /**
         * 
         * @param ctx
         * @throws Exception
         */
        private void fireTimedOut(final ChannelHandlerContext ctx) throws Exception
        {
            ctx.getPipeline().execute(new Runnable()
            {
                public void run()
                {
                    try {
                    	State state = state(ctx);
                    	if (1 == state.state) { // initialized
                    		writeTimedOut(ctx);
                    	} else if (2 == state.state) { // sent
                    		readTimedOut(ctx);
                    	}
                    } catch (Throwable t) {
                        fireExceptionCaught(ctx, t);
                    }
                }
            });
        }
    }

    /**
     * 
     * @author Chauncey
     *
     */
    private static final class State
    {
        // 0 - none, 1 - initialized, 2 - sent, 3 - destroyed
        int state;
        volatile Timeout timeout;

        State()
        {
        }
    }
}
