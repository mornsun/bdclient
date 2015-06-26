package org.mornsun.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.mornsun.client.framework.ChannelFactory;
import org.mornsun.client.framework.ClientEvent;
import org.mornsun.client.framework.Talk;
import org.mornsun.client.pipeline.ClientPipelineFactory;
import org.mornsun.client.util.ICallbackable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A machine serve for a client to a server machine
 * 
 * @author Chauncey
 *
 */
public class Machine implements ICallbackable
{
    private static final Logger log = LoggerFactory.getLogger(Machine.class);
    protected String m_address; // IP:Port
    protected boolean m_isEnable = true; // Whether it is enabled, mainly controlled by logic
    protected boolean m_isHealthy = true; // Whether it is enabled, mainly controlled by the automatic healthy check mechanism
    protected int m_timeoutTimes = 0;
    protected final GenericObjectPoolConfig m_poolConfig = new GenericObjectPoolConfig();
    protected GenericObjectPool<Channel> m_channelPool; // connection pool for keep alive channels
    protected ClientBootstrap m_bootstrap;

    protected int m_nMaxConnNum;
    protected long m_connTimeout;
    protected long m_writeTimeout;
    protected long m_readTimeout;
    protected AtomicInteger m_nConnected;

    /**
     * 
     * @param addr
     * @param connNum
     * @param connTimeout
     */
    public Machine(String addr, int connNum, long connTimeout, long writeTimeout, long readTimeout,
            ClientPipelineFactory pipelineFactory)
    {
        m_address = addr;
        m_nMaxConnNum = connNum;
        m_connTimeout = connTimeout;
        m_writeTimeout = writeTimeout;
        m_readTimeout = readTimeout;
        m_nConnected = new AtomicInteger(0);

        m_poolConfig.setMaxTotal(connNum); // DEFAULT_MAX_TOTAL = 8
        m_poolConfig.setMaxIdle(connNum); // DEFAULT_MAX_IDLE = 8
        m_poolConfig.setNumTestsPerEvictionRun(connNum); // DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 3
        m_poolConfig.setMinIdle(0); // DEFAULT_MIN_IDLE = 0
        m_poolConfig.setLifo(false); // DEFAULT_LIFO = true
        m_poolConfig.setMaxWaitMillis(connTimeout); // DEFAULT_MAX_WAIT_MILLIS = -1L
        m_poolConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 1L); // DEFAULT 1000L * 60L * 30L
        m_poolConfig.setTimeBetweenEvictionRunsMillis(1000L * 60L * 1L); // DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = -1
        m_poolConfig.setTestOnBorrow(true); // DEFAULT_TEST_ON_BORROW = false
        m_poolConfig.setTestOnReturn(true); // DEFAULT_TEST_ON_RETURN = false
        m_poolConfig.setTestWhileIdle(true); // DEFAULT_TEST_WHILE_IDLE = false
        // poolConfig.setSoftMinEvictableIdleTimeMillis( -1 ); // DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS = -1
        // poolConfig.setEvictionPolicyClassName(evictionPolicyClassName) ; //
        // poolConfig.setBlockWhenExhausted(blockWhenExhausted) ; // DEFAULT_BLOCK_WHEN_EXHAUSTED = true
        // poolConfig.setJmxEnabled(jmxEnabled) ; // DEFAULT_JMX_ENABLE = true
        // poolConfig.setJmxNamePrefix(jmxNamePrefix) ; // DEFAULT_JMX_NAME_PREFIX = "pool"
        // m_channelMap = new ConcurrentHashMap<Channel, ClientEvent>((int)(poolSize*1.6));

        m_bootstrap = getNewBootstrap(pipelineFactory);
        m_channelPool = new GenericObjectPool<Channel>(new ChannelFactory(this), m_poolConfig);
    }

    /**
     * 
     * @param connNum
     * @param connTimeout
     * @param writeTimeout
     * @param readTimeout
     * @param pipelineFactory
     * @return
     */
    public boolean configure(int connNum, long connTimeout, long writeTimeout, long readTimeout,
            ClientPipelineFactory pipelineFactory)
    {
        // NOTE: If any error occurs, return as soon as possible, so keep probable dirty data at least
        if (m_connTimeout != connTimeout) {
            ChannelFactory factory = (ChannelFactory) m_channelPool.getFactory();
            if (!factory.configure(connTimeout)) {
                return false;
            }
            m_connTimeout = connTimeout;
            m_bootstrap.setOption("connectTimeoutMillis", connTimeout);
            if (m_bootstrap.getPipelineFactory() != pipelineFactory) {
                m_bootstrap.setPipelineFactory(pipelineFactory);
            }
            m_poolConfig.setMaxWaitMillis(connTimeout); // DEFAULT_MAX_WAIT_MILLIS = -1L
        }
        if (m_nMaxConnNum != connNum) {
            m_channelPool.setMaxTotal(connNum);
            m_channelPool.setMaxIdle(connNum);
            m_channelPool.setMinIdle(connNum);
            m_channelPool.setNumTestsPerEvictionRun(connNum);
            m_nMaxConnNum = connNum;
        }
        m_writeTimeout = writeTimeout;
        m_readTimeout = readTimeout;
        return true;
    }

    /**
     * 
     * @param tcpAddress
     * @return
     */
    public ClientBootstrap getNewBootstrap(ClientPipelineFactory pipelineFactory)
    {
        InetSocketAddress inetAddress = parseAddress(getAddress());
        ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory());
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.setOption("remoteAddress", inetAddress);
        bootstrap.setOption("reuseAddress", false);
        bootstrap.setOption("sendBufferSize", 16384);
        bootstrap.setOption("receiveBufferSize", 131072);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setOption("connectTimeoutMillis", getConnTimeout());
        return bootstrap;
    }

    /**
     * 
     * @param packet
     * @param talk
     * @return
     */
    public boolean send(Object packet, Talk talk)
    {
        Channel channel = null;
        try {
            long currTime = System.currentTimeMillis();
            ClientEvent event = null;
            channel = fetchChannel();
            // Get the reused event of the channel
            if (channel.getAttachment() == null) {
                // It may occur at the first time.
                // Here without lock mechanism since the problem of re-entry of internal channel has been handled by pool
                event = new ClientEvent(talk, currTime, this, channel);
                channel.setAttachment(event);
            } else {
                event = (ClientEvent) channel.getAttachment();
                event.initialize(talk, currTime, this, channel);
            }
            ChannelFuture future = channel.write(packet);
            future.addListener(new ChannelFutureListener()
            {
                public void operationComplete(ChannelFuture future)
                {
                    Channel channel = future.getChannel();
                    if (!future.isSuccess()) { // Fail to write
                        log.warn("Fail to send a message, close the socket.");
                        ClientEvent event = (ClientEvent) channel.getAttachment();
                        event.setStatus(ClientEvent.STATUS_ERROR);
                        freeChannel(channel);
                        return;
                    }
                    // Send successful
                    ClientEvent event = (ClientEvent) channel.getAttachment();
                    if (event.getStatus() == ClientEvent.STATUS_INIT) {
                        event.setStatus(ClientEvent.STATUS_SENDED);
                    } else {
                        // In an asynchronous write, it might have been received and been processed previously, so just log this possibility
                        log.warn("the event status is not excepted:" + event.getStatus());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();// test
            if (channel != null) {
                ClientEvent event = (ClientEvent) channel.getAttachment();
                event.setStatus(ClientEvent.STATUS_ERROR);
                freeChannel(channel);
                channel = null;
            }
            if (e instanceof NoSuchElementException) {
                log.warn("Timeout waiting for idle object:" + getAddress());
                timeout();
            } else {
                setHealthy(false);
                log.warn("Send fail, set not healthy:" + getAddress());
            }
            return false;
        }
        return true;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public Channel fetchChannel() throws Exception
    {
        return m_channelPool.borrowObject();
    }

    /**
     * 
     * @param channel
     */
    public void freeChannel(Channel channel)
    {
        if (null != channel) {
            m_channelPool.returnObject(channel);
        }
    }

    /**
	 * 
	 */
    public void timeout()
    {
        ++m_timeoutTimes;
        if (m_timeoutTimes >= 5) {
            setHealthy(false);
        }
    }

    /**
	 * 
	 */
    public void received()
    {
        m_timeoutTimes = 0;
        setHealthy(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.ICallbackable#callback(java.lang.Object)
     */
    @Override
    public void callback(Object attachment)
    {
        Channel channel = (Channel) attachment;
        received();
        freeChannel(channel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.ICallbackable#processException(java.lang.Throwable, java.lang.Object)
     */
    @Override
    public void processException(Throwable cause, Object attachment)
    {
        Channel channel = (Channel) attachment;
        if (cause instanceof IOException) {
            log.debug("Channel:" + channel.getId() + " connect failed.");
        } else if (cause instanceof IOException) {
            log.warn("Channel:" + channel.getId() + " IOException: " + getAddress(), cause);
        } else if (cause instanceof org.jboss.netty.handler.timeout.ReadTimeoutException) {
            log.warn("Channel:" + channel.getId() + " ReadTimeoutException: " + getAddress());
            timeout();
        } else if (cause instanceof org.jboss.netty.handler.timeout.WriteTimeoutException) {
            log.warn("Channel:" + channel.getId() + " WriteTimeoutException: " + getAddress());
            timeout();
        } else {
            log.warn("Channel:" + channel.getId() + " ClientExceptionCaught(" + getAddress() + ")",
                    cause);
        }
        channel.close();
        freeChannel(channel);
    }

    /**
	 * 
	 */
    public void close()
    {
        disable();
        if (null != m_channelPool) {
            synchronized (this) {
                if (null != m_channelPool) {
                    m_channelPool.close();
                    log.warn("Close machine: " + m_bootstrap.getOption("remoteAddress"));
                    if (m_channelPool.getNumActive() == 0) {
                        m_bootstrap.releaseExternalResources();
                    } else {
                        LazyReleaseThread lazyRelease = new LazyReleaseThread();
                        lazyRelease.start();
                    }
                }
            }
        }
    }

    /**
     * 
     * @author Chauncey
     *
     */
    protected class LazyReleaseThread extends Thread
    {
        /**
         * Lazy release thread
         * After approximate 10s, it will release resources regardless of existing active objects.
         */
        public void run()
        {
            long timeout = 10 * 1000L;
            try {
                while (timeout > 0 && m_channelPool.getNumActive() != 0) {
                    Thread.sleep(100L);
                    timeout -= 100L;
                }
                m_bootstrap.releaseExternalResources();
            } catch (Exception e) {
                log.error("LazyReleaseThread error:" + e.getMessage(), e.getCause());
                e.printStackTrace();
            }
        }
    };

    /**
	 * 
	 */
    public void decrementConn()
    {
        m_nConnected.decrementAndGet();
    }

    /**
	 * 
	 */
    public void incrementConn()
    {
        // System.out.println(m_nConnected);
        m_nConnected.incrementAndGet();
    }

    /**
     * @return the m_channelPool
     */
    public GenericObjectPool<Channel> getChannelPool()
    {
        return m_channelPool;
    }

    /**
     * @return the m_address
     */
    public String getAddress()
    {
        return m_address;
    }

    /**
     * @return the isEnabled
     */
    public boolean isEnabled()
    {
        // return m_failTimes < 5; // After retry 5 times, this machine will be set black
        return m_isEnable;
    }

    /**
     * @param m_isEnable
     *            the m_isEnable to set
     */
    public void enable()
    {
        this.m_isEnable = true;
    }

    /**
     * @param m_isEnable
     *            the m_isEnable to set
     */
    public void disable()
    {
        this.m_isEnable = false;
    }

    /**
     * @return the m_isHealthy
     */
    public boolean isHealthy()
    {
        return m_isHealthy;
    }

    /**
     * @param m_isHealthy
     *            the m_isHealthy to set
     */
    public void setHealthy(boolean m_isHealthy)
    {
        this.m_isHealthy = m_isHealthy;
    }

    /**
     * @return the m_bootstrap
     */
    public ClientBootstrap getBootstrap()
    {
        return m_bootstrap;
    }

    /**
     * @return the m_nConn
     */
    public int getMaxConnnNum()
    {
        return m_nMaxConnNum;
    }

    /**
     * @return the m_connTimeout
     */
    public long getConnTimeout()
    {
        return m_connTimeout;
    }

    /**
     * @return the m_writeTimeout
     */
    public long getWriteTimeout()
    {
        return m_writeTimeout;
    }

    /**
     * @return the m_readTimeout
     */
    public long getReadTimeout()
    {
        return m_readTimeout;
    }

    /**
     * Parse a string "ip:port" to an instance of InetSocketAddress
     * 
     * @param address
     * @return
     */
    public static InetSocketAddress parseAddress(String address)
    {
        if (address == null) {
            return null;
        }
        String[] strs = address.split("[:]");
        if (strs == null || strs.length != 2) {
            return null;
        }
        String ip = strs[0];
        try {
            int port = Integer.parseInt(strs[1]);
            return new InetSocketAddress(ip, port);
        } catch (Exception e) {
            return null;
        }
    }
}
