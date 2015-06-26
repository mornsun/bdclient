/**
 * 
 */
package org.mornsun.client.framework;

import org.jboss.netty.channel.Channel;
import org.mornsun.client.Machine;
import org.mornsun.client.util.ICallbackable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chauncey
 *
 */
public class ClientEvent implements ICallbackable
{
    protected static final Logger log = LoggerFactory.getLogger(ClientEvent.class);

    public static final int STATUS_INIT = 0;
    public static final int STATUS_SENDED = 1;
    public static final int STATUS_RECEIVED = 2;
    public static final int STATUS_ERROR = 3;

    protected int m_status;
    protected Talk m_talk;
    protected long m_starttime;
    protected Machine m_machine;
    protected Channel m_channel;

    // private long m_timediff;
    // private long m_rtimeout;
    // private long m_wtimeout;

    /**
	 * 
	 */
    public ClientEvent(Talk talk, long starttime, Machine machine, Channel channel)
    {
        initialize(talk, starttime, machine, channel);
    }

    /**
     * Initialize method for reusing this instance
     * 
     * @param talk
     * @param starttime
     */
    public void initialize(Talk talk, long starttime, Machine machine, Channel channel)
    {
        m_machine = machine;
        m_channel = channel;
        m_talk = talk;
        m_starttime = starttime;
        setStatus(STATUS_INIT);
        // m_rtimeout = readtimeout;
        // m_wtimeout = writetimeout;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.ICallbackable#processException(java.lang.Throwable, java.lang.Object)
     */
    @Override
    public void processException(Throwable cause, Object attachment)
    {
        setStatus(STATUS_ERROR);
        if (attachment == m_channel) { // attachment must be same with m_channel
            m_machine.processException(cause, attachment);
        } else {
            log.error("callback channel must accord with event channel");
        }
        m_talk.processException(cause, attachment);
    }

    /**
     * @return the m_status
     */
    public int getStatus()
    {
        return m_status;
    }

    /**
     * @param m_status
     *            the m_status to set
     */
    public void setStatus(int status) throws IllegalArgumentException
    {
        if (status < STATUS_INIT && status > STATUS_ERROR) {
            throw new IllegalArgumentException("status out of range:" + status);
        }
        this.m_status = status;
    }

    /**
     * @return the m_starttime
     */
    public long getStarttime()
    {
        return m_starttime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.ICallbackable#callback(java.lang.Object)
     */
    @Override
    public void callback(Object response)
    {
        m_talk.callback(response);
        m_machine.callback(m_channel);
    }
}
