/**
 * 
 */
package org.mornsun.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.ho.yaml.Yaml;
import org.mornsun.client.config.ClientCfg;
import org.mornsun.client.config.ServiceCfg;
import org.mornsun.client.framework.HealthyChecker;
import org.mornsun.client.framework.Talk;
import org.mornsun.client.util.IUpdatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Chauncey
 *
 */
public class ClientMgr implements IUpdatable
{
    private static final Logger log = LoggerFactory.getLogger(ClientMgr.class);
    private static ClientMgr INSTANCE = null;

    protected static Timer m_timer = null;
    protected static long m_lastUpdateTime = -1;
    protected static final Object updateLock = new Object();

    protected int m_healthyCheckerPeriod;
    protected int m_updatePeriod;

    protected final ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<String, Service>(
            16);

    /**
     * Atomic: if you read or write it, you must assure that it is an atomic operation
     */
    // private ClientCfg m_clientCfg = null;

    /**
     * Retrieve the singleton instance
     * NOTE: It is strongly recommended to invoke this at the period of loading configurations or initialization.
     * 
     * @return
     */
    public static ClientMgr getInstance()
    {
        if (INSTANCE == null) {
            synchronized (ClientMgr.class) {
                if (INSTANCE == null) {
                    ClientMgr instance = new ClientMgr();
                    instance.init();
                    INSTANCE = instance;
                }
            }
        }
        return INSTANCE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.IUpdatable#update()
     */
    @Override
    public boolean update()
    {
        // sample configuration
        String cfgFolder = System.getProperty("config", "./config/");
        String cfgFn = cfgFolder + File.separator + "client.yaml";
        File cfgFile = new File(cfgFn);
        long time = cfgFile.lastModified();
        try {
            if (time != 0 && time != m_lastUpdateTime) { // the configuration file exists and has been modified
                boolean bNeedUpdate = false;
                synchronized (updateLock) {
                    if (time != 0 && time != m_lastUpdateTime) { // the configuration file exists and has been modified
                        m_lastUpdateTime = time; // If set m_lastUpdateTime after successfully reconfigure, a incorrect configuration will be loaded repeatedly
                        bNeedUpdate = true;
                    }
                }
                if (bNeedUpdate) {
                    // update the configuration structure with the mechanism like double buffers
                    // lock free to update the configuration
                    ClientCfg cfg = Yaml.loadType(cfgFile, ClientCfg.class);
                    cfg.postprocess();
                    if (!configure(cfg)) {
                        throw new Exception(
                                "Configure error, please check your latest configuration.");
                    }
                    if (m_healthyCheckerPeriod != cfg.getHealthyCheckerPeriod()) {
                        m_healthyCheckerPeriod = cfg.getHealthyCheckerPeriod();
                        if (m_healthyCheckerPeriod != 0) {
                            HealthyChecker checker = HealthyChecker.getInstance();
                            checker.startHealthyChecker(m_healthyCheckerPeriod);
                        }
                    }
                    if (m_updatePeriod != cfg.getUpdatePeriod()) {
                        m_updatePeriod = cfg.getUpdatePeriod();
                        if (m_updatePeriod != 0) {
                            startAutoUpdate(m_updatePeriod);
                        }
                    }
                    log.warn("updated " + cfgFile);
                    return true;
                }
            }
            if (0 == time) {
                log.error("the configuration file does not be found: " + cfgFile);
            } else {
                log.debug("the configuration file does not modified: " + cfgFile);
            }
        } catch (FileNotFoundException e) { // will not enter this block in normal case because of the anterior assertion of time
            log.error("init Exception: config file error: file not found" + cfgFile);
        } catch (Exception e) {
            log.error("init Exception: config file error: " + cfgFile, e.getCause());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 
     * @param cfg
     * @return NOTE: If return false, services may be dirty so cannot be rolled back
     */
    public boolean configure(ClientCfg cfg)
    {
        // Add new services
        Iterator<Entry<String, ServiceCfg>> iterCfg = cfg.getServiceCfgMap().entrySet().iterator();
        while (iterCfg.hasNext()) {
            Entry<String, ServiceCfg> entry = (Entry<String, ServiceCfg>) iterCfg.next();
            String serviceName = entry.getKey();
            ServiceCfg serviceCfg = entry.getValue();
            if (!serviceMap.containsKey(serviceName)) {
                Service service = new Service(serviceCfg);
                serviceMap.put(serviceName, service);
            }
        }
        // Delete discarded services
        Iterator<Entry<String, Service>> iterService = serviceMap.entrySet().iterator();
        while (iterService.hasNext()) {
            Entry<String, Service> entry = (Entry<String, Service>) iterService.next();
            String serviceName = entry.getKey();
            Service service = entry.getValue();
            if (!cfg.getServiceCfgMap().containsKey(serviceName)) {
                service.close();
                serviceMap.remove(serviceName);
            }
        }
        // Update existing services
        iterCfg = cfg.getServiceCfgMap().entrySet().iterator();
        while (iterCfg.hasNext()) {
            Entry<String, ServiceCfg> entry = (Entry<String, ServiceCfg>) iterCfg.next();
            String serviceName = entry.getKey();
            ServiceCfg serviceCfgl = entry.getValue();
            Service service = serviceMap.get(serviceName);
            if (null == service) {
                log.error("After merging, service must not be null");
                return false;
            }
            service.configure(serviceCfgl);
        }
        return true;
    }

    /**
     * @return the serviceMap
     */
    public ConcurrentHashMap<String, Service> getServiceMap()
    {
        return serviceMap;
    }

    /**
     * 
     * @param packet
     * @param talk
     * @return
     */
    public Service getService(String name)
    {
        return serviceMap.get(name);
    }

    /**
	 * 
	 */
    protected ClientMgr()
    {
        // TODO Auto-generated constructor stub
    }

    /**
     * Initialize this instance
     */
    protected void init()
    {
        boolean res = update();
        if (false == res) { // at the 1st time, it must be successful
            throw new RuntimeException("fail to init the configuration");
        }
    }

    /**
     * 
     * @param period
     */
    public void startAutoUpdate(long period)
    {
        stopAutoUpdate(); // in case of re-entrance of this function
        if (null == m_timer) {
            synchronized (ClientMgr.class) {
                if (m_timer == null) {
                    m_timer = new Timer();
                }
            }
        }
        m_timer.schedule(new UpdateTask(), period * 1000, period * 1000); // call task per period seconds
    }

    /**
	 * 
	 */
    public void stopAutoUpdate()
    {
        if (m_timer != null) {
            m_timer.cancel();
            m_timer.purge();
            m_timer = null;
        }
    }

    /**
     * API function: execute the handling info progression
     * 
     * @param reqdata
     * @return
     */
    public boolean send(String serviceName, Object packet, Talk talk)
    {
        if (null == serviceName || null == packet || null == talk) {
            String errorMsg = new String("serviceName[" + (serviceName != null) + "] " + "packet["
                    + (packet != null) + "] " + "talk[" + (talk != null) + "]");
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        Service service = getService(serviceName);
        return service.send(packet, talk);
    }

    /**
     * 
     * @author Chauncey
     *
     */
    protected class UpdateTask extends TimerTask
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run()
        {
            update();
        }
    }
}
