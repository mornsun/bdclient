package org.mornsun.client.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.mornsun.client.framework.TimeoutHandler;

/**
 * 
 * @author Chauncey
 *
 */
public abstract class ClientPipelineFactory implements ChannelPipelineFactory
{
    protected TimeoutHandler m_timeoutHandler; // Reused in SERVICE scale

    /**
	 * 
	 */
    public ClientPipelineFactory()
    {
    }

    /**
     * 
     * @param serviceCfg
     */
    public void setTimeoutHandler(TimeoutHandler timeoutHandler)
    {
        m_timeoutHandler = timeoutHandler;
    }

    /**
	 * 
	 */
    public TimeoutHandler getTimeoutHandler()
    {
        return m_timeoutHandler;
    }

    /**
	 * 
	 */
    @Override
    public abstract ChannelPipeline getPipeline() throws Exception;
}
