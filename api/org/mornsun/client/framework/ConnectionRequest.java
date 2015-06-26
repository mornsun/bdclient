/**
 * 
 */
package org.mornsun.client.framework;

import java.util.ArrayList;

import org.mornsun.client.Machine;
import org.mornsun.client.Service;

/**
 * @author Chauncey
 *
 */
public class ConnectionRequest
{
	protected int key;                        // Balance factor used for HASH reference of specific request
	protected int nthRetry;             // Retry times, 0 indicate the normal scheduling (the first time selection)
	protected ArrayList<Machine> resMachines;
	protected Service m_service;

	/**
	 * 
	 */
	public ConnectionRequest(Service service)
	{
		key = -1;
		nthRetry = 0;
		m_service = service;
		resMachines = new ArrayList<Machine>(m_service.getMachines().size());
	}

	/**
	 * @return the resMachines
	 */
	public ArrayList<Machine> getResMachines()
	{
		return resMachines;
	}
	
	/**
	 * @return the m_service
	 */
	public Service getService()
	{
		return m_service;
	}
}
