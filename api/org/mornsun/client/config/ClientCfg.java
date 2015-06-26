package org.mornsun.client.config;

import java.util.Arrays;
import java.util.HashMap;

import org.mornsun.client.util.IPostprocessable;

/**
 * @author Chauncey
 *
 */
public class ClientCfg implements IPostprocessable
{
    protected static final int DEFAULT_UPDATE_PERIOD = 0; // in second
    protected static final int DEFAULT_HEALTHY_CHECKER_PERIOD = 0; // in second

    public ServiceCfg Services[]; // It will be freed after post-process
    public int UpdatePeriod; // in second
    public int HealthyCheckerPeriod; // in second

    protected HashMap<String, ServiceCfg> serviceCfgMap = new HashMap<String, ServiceCfg>();

    /**
     * Private constructor. Prevents instantiation from other classes.
     */
    public ClientCfg()
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
        for (ServiceCfg service : Services) {
            service.postprocess();
            serviceCfgMap.put(service.getName(), service);
        }
        if (0 == UpdatePeriod) {
            UpdatePeriod = DEFAULT_UPDATE_PERIOD;
        }
        if (0 == HealthyCheckerPeriod) {
            HealthyCheckerPeriod = DEFAULT_HEALTHY_CHECKER_PERIOD;
        }
        Services = null; // This can be freed
    }

    /**
     * @return the serviceCfgMap
     */
    public HashMap<String, ServiceCfg> getServiceCfgMap()
    {
        return serviceCfgMap;
    }

    /**
     * @return the updatePeriod
     */
    public int getUpdatePeriod()
    {
        return UpdatePeriod;
    }

    /**
     * @return the healthyCheckerPeriod
     */
    public int getHealthyCheckerPeriod()
    {
        return HealthyCheckerPeriod;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "ClientCfg [Services=" + Arrays.toString(Services) + "]";
    }
}
