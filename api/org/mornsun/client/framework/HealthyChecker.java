/**
 * 
 */
package org.mornsun.client.framework;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.jboss.netty.channel.Channel;
import org.mornsun.client.ClientMgr;
import org.mornsun.client.Machine;
import org.mornsun.client.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chauncey
 *
 */
public class HealthyChecker
{
    private static final Logger log = LoggerFactory.getLogger(HealthyChecker.class);
    private static Timer m_timer = null;
    private static HealthyChecker INSTANCE = null;

    protected int m_period;

    /**
     * Retrieve the singleton instance
     * NOTE: It is strongly recommended to invoke this at the period of loading configurations or initialization.
     * 
     * @return
     */
    public static HealthyChecker getInstance()
    {
        if (INSTANCE == null) {
            synchronized (HealthyChecker.class) {
                if (INSTANCE == null) {
                    HealthyChecker instance = new HealthyChecker();
                    instance.init();
                    INSTANCE = instance;
                }
            }
        }
        return INSTANCE;
    }

    /**
	 * 
	 */
    private HealthyChecker()
    {
        // TODO Auto-generated constructor stub
    }

    /**
	 * 
	 */
    public void healthyCheck()
    {
        ClientMgr mgr = ClientMgr.getInstance();
        Iterator<Entry<String, Service>> iterService = mgr.getServiceMap().entrySet().iterator();
        while (iterService.hasNext()) {
            Entry<String, Service> entry = (Entry<String, Service>) iterService.next();
            String serviceName = entry.getKey();
            Service service = entry.getValue();
            if (null != service) {
                for (Machine machine : service.getMachines()) {
                    if (null != machine && machine.isEnabled() && !machine.isHealthy()) {
                        Channel channel = null;
                        try {
                            channel = machine.fetchChannel();
                            if (null != channel) {
                                machine.setHealthy(true);
                                machine.freeChannel(channel);
                                log.warn("The unhealthy machine:" + machine.getAddress()
                                        + " of server:" + serviceName + " is back to health.");
                            }
                        } catch (Exception e) {
                            // TODO: log.warn(""The unhealthy machine:" + machine.getAddress() + " of server:" + serviceName + " keep unhealthy")
                            machine.freeChannel(channel);
                        }
                    }
                }
            }
        }
    }

    /**
	 * 
	 */
    public void startHealthyChecker(int period)
    {
        stopHealthyChecker(); // in case of re-entrance of this function
        if (null == m_timer) {
            synchronized (HealthyChecker.class) {
                if (m_timer == null) {
                    m_timer = new Timer();
                }
            }
        }
        m_timer.schedule(new HealthyCheckerTask(), period * 1000, period * 1000); // call task per period seconds
    }

    /**
	 * 
	 */
    public void stopHealthyChecker()
    {
        if (m_timer != null) {
            m_timer.cancel();
            m_timer.purge();
            m_timer = null;
        }
    }

    /**
     * Initialize this instance
     */
    protected void init()
    {
    }

    /**
     * 
     * @author Chauncey
     *
     */
    protected class HealthyCheckerTask extends TimerTask
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run()
        {
            healthyCheck();
        }
    }

}
