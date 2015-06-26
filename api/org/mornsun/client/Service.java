package org.mornsun.client;

import java.util.ArrayList;
import java.util.List;

import org.mornsun.client.config.ServiceCfg;
import org.mornsun.client.framework.ConnectionRequest;
import org.mornsun.client.framework.Talk;
import org.mornsun.client.framework.TimeoutHandler;
import org.mornsun.client.pipeline.ClientPipelineFactory;
import org.mornsun.client.strategy.DefaultStrategy;
import org.mornsun.client.strategy.IStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that serve for a client to a server cluster
 * 
 * @author Chauncey
 *
 */
public class Service
{
    private static final Logger log = LoggerFactory.getLogger(Service.class);
    protected final String m_name;
    protected long m_connTimeout;
    protected long m_writeTimeout;
    protected long m_readTimeout;
    protected int m_nRetry;
    protected int m_nConn;
    protected List<Machine> m_machines;
    protected IStrategy m_strategy;
    protected int m_maxPendingQueueSize;

    protected ClientPipelineFactory m_clientPipelineFactory;

    /**
     * 
     * @param serviceCfg
     * @return
     */
    public boolean configure(ServiceCfg serviceCfg)
    {
        // NOTE: If any error occurs, return as soon as possible, so keep probable dirty data at least
        // Cache the updated attributes
        long connTimeout = serviceCfg.getConnectTimeout();
        long writeTimeout = serviceCfg.getWriteTimeout();
        long readTimeout = serviceCfg.getReadTimeout();
        int nConn = serviceCfg.getMaxConnect();
        int nRetry = serviceCfg.getRetry(); // If not set, it will be 0
        int maxPendingQueueSize = serviceCfg.getLinger(); // If not set, it will be 0
        if (null == m_clientPipelineFactory
                || m_clientPipelineFactory.getClass() != serviceCfg.getClientPipelineFactoryClass()) {
            try {
                m_clientPipelineFactory = (ClientPipelineFactory) serviceCfg
                        .getClientPipelineFactoryClass().newInstance();
                TimeoutHandler timeoutHandler = new TimeoutHandler(serviceCfg.getWriteTimeout(),
                        serviceCfg.getReadTimeout());
                m_clientPipelineFactory.setTimeoutHandler(timeoutHandler);
            } catch (InstantiationException e) {
                log.error("pipeline class cannot new instance:" + e.getMessage(), e.getCause());
            } catch (IllegalAccessException e) {
                log.error("pipeline class cannot access:" + e.getMessage(), e.getCause());
            }
        } else {
            TimeoutHandler timeoutHandler = m_clientPipelineFactory.getTimeoutHandler();
            timeoutHandler.setTimeout(writeTimeout, readTimeout);
        }

        // Update machines
        List<Machine> currMachines = m_machines;
        String newMachinesAddr[] = serviceCfg.getMachines();
        // Get result: record indexes of the common elements [common1 in x, common1 in y, common2 in x, common2 in y, ...]
        int iCommon[] = getLCS(currMachines, newMachinesAddr);
        List<Machine> newMachines = new ArrayList<Machine>(newMachinesAddr.length); // Allocated for storing updated machines
        // Iterate the array newMachinesAddr
        int offset = 0;
        for (int i = 1; i < iCommon.length; i += 2) {
            // Add new machines
            for (int k = offset; k < iCommon[i]; ++k) {
                Machine machine = new Machine(newMachinesAddr[k], nConn, connTimeout, writeTimeout,
                        readTimeout, m_clientPipelineFactory);
                newMachines.add(machine);
            }
            // Add reused machines that have been bootstrap in array currMachines
            Machine reusedMachine = currMachines.get(iCommon[i - 1]);
            newMachines.add(reusedMachine);
            boolean res = reusedMachine.configure(nConn, connTimeout, writeTimeout, readTimeout,
                    m_clientPipelineFactory);
            if (false == res) {
                log.error("Reconfigure machine error:" + reusedMachine.toString()
                        + ", has correctly configure machines:" + iCommon[i]);
                return false;
            }
            offset = iCommon[i] + 1;
        }
        // Add new machines at the tail of newMachinesAddr
        for (int k = offset; k < newMachinesAddr.length; ++k) {
            Machine machine = new Machine(newMachinesAddr[k], nConn, connTimeout, writeTimeout,
                    readTimeout, m_clientPipelineFactory);
            newMachines.add(machine);
        }
        // Iterate the array currMachines
        offset = 0;
        for (int i = 0; i < iCommon.length; i += 2) {
            // Free discarded machines
            for (int k = offset; k < iCommon[i]; ++k) {
                Machine machine = currMachines.get(k);
                machine.close();
            }
            offset = iCommon[i] + 1;
        }
        currMachines.clear();
        m_machines = newMachines;

        m_connTimeout = connTimeout;
        m_writeTimeout = writeTimeout;
        m_readTimeout = readTimeout;
        m_nConn = nConn;
        m_nRetry = nRetry; // If not set, it will be 0
        m_maxPendingQueueSize = maxPendingQueueSize; // If not set, it will be 0

        return true;
    }

    /**
     * 
     * @param serviceCfg
     */
    public Service(ServiceCfg serviceCfg)
    {
        m_name = serviceCfg.getName();
        m_machines = new ArrayList<Machine>();
        m_strategy = new DefaultStrategy();
        configure(serviceCfg);
    }

    /**
     * 
     * @param name
     */
    public Service(String name)
    {
        m_name = name;
        m_machines = new ArrayList<Machine>();
        m_strategy = new DefaultStrategy();
        m_connTimeout = ServiceCfg.DEFAULT_CONNECT_TIMEOUT;
        m_writeTimeout = ServiceCfg.DEFAULT_WRITE_TIMEOUT;
        m_readTimeout = ServiceCfg.DEFAULT_READ_TIMEOUT;
        m_nConn = ServiceCfg.DEFAULT_CONNECT_NUM;
        m_nRetry = 0;
        m_maxPendingQueueSize = 0;
    }

    /**
     * 
     * @param name
     * @param nConn
     */
    public Service(String name, int nConn)
    {
        m_name = name;
        m_machines = new ArrayList<Machine>();
        m_strategy = new DefaultStrategy();
        m_connTimeout = ServiceCfg.DEFAULT_CONNECT_TIMEOUT;
        m_writeTimeout = ServiceCfg.DEFAULT_WRITE_TIMEOUT;
        m_readTimeout = ServiceCfg.DEFAULT_READ_TIMEOUT;
        m_nConn = nConn;
        m_nRetry = 0;
        m_maxPendingQueueSize = 0;
    }

    /**
     * 
     * @param addr
     */
    public void deleteMachine(String addr)
    {
        // TODO: //O(2n) -> O(n)
        for (Machine machine : m_machines) {
            if (machine.getAddress().equals(addr)) {
                machine.close();
                m_machines.remove(machine);
                break;
            }
        }
    }

    /**
     * @return the m_machines
     */
    public List<Machine> getMachines()
    {
        return m_machines;
    }

    /**
     * 
     * @param packet
     * @param talk
     * @return
     */
    public boolean send(Object packet, Talk talk)
    {
        ConnectionRequest req = new ConnectionRequest(this);
        // TODO: Set req for advanced balancing. Now use its default value
        Machine machine = m_strategy.schedule(req);
        if (null == machine) {
            return false;
        }
        boolean res = machine.send(packet, talk);
        if (true == res) { // succeed to send the packet
            return true;
        }
        // If fail to send the packet to the scheduled machine, reschedule until retry times
        int nRetry = m_nRetry;
        while (nRetry != 0) {
            machine = m_strategy.schedule(req);
            if (null == machine) {
                return false; // no machine to be scheduled, return directly
            }
            res = machine.send(packet, talk);
            if (true == res) { // succeed to send the packet
                return true;
            }
            log.warn("fail to send the packet, retry:" + nRetry);
            --nRetry;
        }
        return false;
    }

    /**
	 * 
	 */
    public void close()
    {
        for (Machine machine : m_machines) {
            machine.close();
        }
        m_machines.clear();
    }

    /**
     * 
     * @param x
     * @param y
     * @return
     */
    public final static int[] getLCS(List<Machine> x, String[] y)
    {
        int i, j;
        final int xSize = x.size();
        final int ySize = y.length;
        int[][] memo = new int[xSize + 1][ySize + 1];
        // DP
        for (i = 0; i < xSize; i++) {
            for (j = 0; j < ySize; j++) {
                if (x.get(i).getAddress().equals(y[j])) {
                    memo[i + 1][j + 1] = memo[i][j] + 1;
                } else if (memo[i][j + 1] >= memo[i + 1][j]) {
                    memo[i + 1][j + 1] = memo[i][j + 1];
                } else {
                    memo[i + 1][j + 1] = memo[i + 1][j];
                }
            }
        }
        // Get result: record indexes of the common elements [memo1 in x, memo1 in y, memo2 in x, memo2 in y, ...]
        int lenLCS = memo[xSize][ySize];
        int idx[] = new int[lenLCS << 1];
        int k = (lenLCS << 1) - 1;
        i = xSize;
        j = ySize;
        while (i != 0 && j != 0) {
            if (memo[i][j] == memo[i - 1][j]) {
                --i;
            } else if (memo[i][j] == memo[i][j - 1]) {
                --j;
            } else {
                idx[k--] = --j;
                idx[k--] = --i;
            }
        }
        return idx;
    }

    /*
     * static void init()
     * {
     * new Timer().schedule(new timeTask(), 0, 20 * 1000);
     * }
     * 
     * class timeTask extends java.util.TimerTask
     * {
     * 
     * @Override
     * public void run()
     * {
     * try
     * {
     * // write all packets in pktQueue
     * while (m_pendingQueue.size() > 0) {
     * send(m_pendingQueue.remove());
     * }
     * 
     * // send the newest packet
     * channel.write(packet);
     * }
     * catch (Exception e)
     * {
     * log.error("CachedChannelPoolWrapper.writePackets error:" + e.getCause(), e);
     * pktQueue.clear();
     * }
     * }
     * }
     */
}
