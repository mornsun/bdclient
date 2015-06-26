/**
 * 
 */
package org.mornsun.client.strategy;

import java.util.List;
import java.util.Random;

import org.mornsun.client.Machine;
import org.mornsun.client.framework.ConnectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chauncey
 *
 */
public class DefaultStrategy implements IStrategy
{
    private static final Logger log = LoggerFactory.getLogger(DefaultStrategy.class);
    protected final Random m_random;

    /**
	 * 
	 */
    public DefaultStrategy()
    {
        m_random = new Random(System.currentTimeMillis() ^ Thread.currentThread().getId());
    }

    /**
     * 
     * @param req
     * @param resMachines
     */
    protected void filter(ConnectionRequest req)
    {
        List<Machine> machines = req.getService().getMachines();
        for (Machine machine : machines) {
            if (null != machine && machine.isEnabled() && machine.isHealthy()) {
                req.getResMachines().add(machine);
            }
        }
    }

    /**
     * 
     * @param req
     * @param resMachines
     * @return
     */
    protected Machine balance(ConnectionRequest req)
    {
        int hashIndex = m_random.nextInt(req.getResMachines().size());
        return req.getResMachines().get(hashIndex);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.strategy.IStrategy#schedule(org.mornsun.client.framework.ConnectionRequest)
     */
    @Override
    public Machine schedule(ConnectionRequest req)
    {
        if (req.getService().getMachines().size() == 0) {
            log.error("No machine can be scheduled: you may not configure properly.");
            return null;
        }
        filter(req);
        if (0 == req.getResMachines().size()) {
            log.warn("No machine can be scheduled: all the machines are disabled");
            return null;
        }
        Machine machine = balance(req);
        req.getResMachines().clear();

        return machine;
    }
}
