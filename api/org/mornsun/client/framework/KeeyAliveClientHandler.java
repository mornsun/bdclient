package org.mornsun.client.framework;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.mornsun.client.util.ICallbackable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Chauncey
 *
 */
@Sharable
public class KeeyAliveClientHandler extends SimpleChannelUpstreamHandler
{
    private static final Logger log = LoggerFactory.getLogger(KeeyAliveClientHandler.class);

    /**
     * Creates a new instance.
     *
     * @param timeoutMilliseconds
     *            read timeout in milliseconds
     */
    public KeeyAliveClientHandler()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
        log.debug("Channel:" + e.getChannel().getId() + " has connected to:"
                + e.getChannel().getRemoteAddress());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelDisconnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception
    {
        log.debug("Channel:" + e.getChannel().getId() + " has been disconnected:"
                + e.getChannel().getRemoteAddress());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {
        ICallbackable event = (ICallbackable) e.getChannel().getAttachment();
        if (null != event) {
            event.callback(e.getMessage());
        } else {
            log.error("Channel:" + e.getChannel().getId() + " attachment is null: "
                    + e.getChannel().getRemoteAddress());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        // Relay this exception to its callback instances
        if (null != e.getChannel().getAttachment()) {
            ICallbackable event = (ICallbackable) e.getChannel().getAttachment();
            event.processException(e.getCause(), e.getChannel());
        }
    }
}
