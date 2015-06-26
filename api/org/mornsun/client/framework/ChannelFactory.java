/**
 * 
 */
package org.mornsun.client.framework;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.mornsun.client.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chauncey
 *
 */
public class ChannelFactory extends BasePooledObjectFactory<Channel>
{
    private static final Logger log = LoggerFactory.getLogger(ChannelFactory.class);
    private Machine m_machine;

    /**
     * 
     * @param bootstrap
     */
    public ChannelFactory(Machine machine)
    {
        m_machine = machine;
    }

    /**
     * 
     * @param connTimeout
     * @return always true
     */
    public boolean configure(long connTimeout)
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool2.BasePooledObjectFactory#create()
     */
    @Override
    public Channel create() throws RuntimeException
    {
        ChannelFuture future = null;
        try {
            future = m_machine.getBootstrap().connect();
            final CountDownLatch channelLatch = new CountDownLatch(1);
            future.addListener(new ChannelFutureListener()
            {
                public void operationComplete(ChannelFuture future) throws Exception
                {
                    channelLatch.countDown();
                }
            });
            channelLatch.await();
            // channelLatch.await(m_machine.getConnTimeout()*2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("makeObject exception: "
                    + m_machine.getBootstrap().getOption("remoteAddress"), e.getCause());
        }
        if (future == null || !future.isSuccess()) {
            if (future.getCause() instanceof java.net.ConnectException) {
                log.warn("Cannot connect to: "
                        + m_machine.getBootstrap().getOption("remoteAddress"));
            } else if (future.getCause() instanceof java.net.NoRouteToHostException) {
                log.warn("Cannot route to: " + m_machine.getBootstrap().getOption("remoteAddress"));
            } else if (future.getCause() instanceof java.nio.channels.ClosedChannelException) {
                log.warn("Cannot close: " + m_machine.getBootstrap().getOption("remoteAddress"));
            } else {
                log.error(
                        "failed to create a channel: "
                                + m_machine.getBootstrap().getOption("remoteAddress"),
                        future.getCause());
            }
            if (future.getChannel() != null) {
                future.getChannel().close();
            }
            throw new RuntimeException("failed to create a channel: "
                    + m_machine.getBootstrap().getOption("remoteAddress"), future.getCause());
        }
        if (log.isDebugEnabled()) {
            log.debug("ChannelFactory.create: " + future.getChannel());
        }
        return future.getChannel();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool2.BasePooledObjectFactory#wrap(java.lang.Object)
     */
    @Override
    public PooledObject<Channel> wrap(Channel channel)
    {
        return new DefaultPooledObject<Channel>(channel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool2.BasePooledObjectFactory#validateObject(org.apache.commons.pool2.PooledObject)
     */
    @Override
    public boolean validateObject(final PooledObject<Channel> pooledChannel)
    {
        final Channel channel = pooledChannel.getObject();
        return channel.isConnected();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool2.BasePooledObjectFactory#destroyObject(org.apache.commons.pool2.PooledObject)
     */
    @Override
    public void destroyObject(final PooledObject<Channel> pooledChannel) throws Exception
    {
        final Channel channel = pooledChannel.getObject();
        if (log.isDebugEnabled()) {
            log.debug("ChannelFactory.destroyObject: " + channel);
        }
        channel.close();
    }
}