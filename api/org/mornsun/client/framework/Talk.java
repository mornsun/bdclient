/**
 * 
 */
package org.mornsun.client.framework;

import org.mornsun.client.util.ICallbackable;

/**
 * @author Chauncey
 *
 */
public abstract class Talk implements ICallbackable
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mornsun.client.util.ICallbackable#processException(java.lang.Throwable, java.lang.Object)
     */
    public void processException(Throwable cause, Object attachment)
    {
        // TODO: provisionally does nothing
    }

}
