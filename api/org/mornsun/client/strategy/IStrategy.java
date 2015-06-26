/**
 * 
 */
package org.mornsun.client.strategy;

import org.mornsun.client.Machine;
import org.mornsun.client.framework.ConnectionRequest;

/**
 * @author Chauncey
 *
 */
public interface IStrategy
{

    /**
	 * 
	 */
    public Machine schedule(ConnectionRequest req);

}
