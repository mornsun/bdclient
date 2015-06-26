package org.mornsun.client.config;

import java.util.Arrays;

import org.mornsun.client.util.IPostprocessable;

/**
 * @author Chauncey
 *
 */
public class ServiceCfg implements IPostprocessable
{
    public static final long DEFAULT_CONNECT_TIMEOUT = 1000L; // 1s
    public static final long DEFAULT_WRITE_TIMEOUT = 1000L; // 1s
    public static final long DEFAULT_READ_TIMEOUT = 1000L; // 1s
    public static final int DEFAULT_CONNECT_NUM = 50;

    public String Name;
    public long ConnectTimeout;
    public long WriteTimeout;
    public long ReadTimeout;
    public int MaxConnect;
    public int Retry;
    public int Linger;
    public String Pipeline;
    public String Machines[];

    protected Class<?> m_clientPipelineFactoryClass;

    /**
     * Private constructor. Prevents instantiation from other classes.
     */
    public ServiceCfg()
    {
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.IPostprocessable#postprocess()
     */
    @Override
    public void postprocess() throws IllegalArgumentException
    {
        // prepare
        if (null == Name || "".equals(Name)) {
            throw new IllegalArgumentException("service name is null");
        }
        if (null == Pipeline || "".equals(Pipeline)) {
            throw new IllegalArgumentException("pipeline is null");
        }
        try {
            m_clientPipelineFactoryClass = Class.forName(Pipeline);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("pipeline class not found:", e.getCause());
        }
        if (null == Machines || 0 == Machines.length) {
            throw new IllegalArgumentException("no machine");
        }
        for (String machine : Machines) {
            if (null == machine || "".equals(machine)) {
                throw new IllegalArgumentException("machine address is null");
            }
        }
        if (0 == ConnectTimeout) {
            ConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (0 == WriteTimeout) {
            WriteTimeout = DEFAULT_WRITE_TIMEOUT;
        }
        if (0 == ReadTimeout) {
            ReadTimeout = DEFAULT_READ_TIMEOUT;
        }
        if (0 == MaxConnect) {
            MaxConnect = DEFAULT_CONNECT_NUM;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "ServiceCfg [Name=" + Name + ", ConnectTimeout=" + ConnectTimeout
                + ", WriteTimeout=" + WriteTimeout + ", ReadTimeout=" + ReadTimeout
                + ", MaxConnect=" + MaxConnect + ", Retry=" + Retry + ", Linger=" + Linger
                + ", Machines=" + Arrays.toString(Machines) + "]";
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return Name;
    }

    /**
     * @return the connectTimeout
     */
    public long getConnectTimeout()
    {
        return ConnectTimeout;
    }

    /**
     * @return the writeTimeout
     */
    public long getWriteTimeout()
    {
        return WriteTimeout;
    }

    /**
     * @return the readTimeout
     */
    public long getReadTimeout()
    {
        return ReadTimeout;
    }

    /**
     * @return the maxConnect
     */
    public int getMaxConnect()
    {
        return MaxConnect;
    }

    /**
     * @return the retry
     */
    public int getRetry()
    {
        return Retry;
    }

    /**
     * @return the linger
     */
    public int getLinger()
    {
        return Linger;
    }

    /**
     * @return the machines
     */
    public String[] getMachines()
    {
        return Machines;
    }

    /**
     * @return the m_clientPipelineFactory
     */
    public Class<?> getClientPipelineFactoryClass()
    {
        return m_clientPipelineFactoryClass;
    }

}
