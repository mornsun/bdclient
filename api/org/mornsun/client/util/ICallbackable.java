/**
 * 
 */
package org.mornsun.client.util;

/**
 * @author Chauncey
 *
 */
public interface ICallbackable
{
    /**
     * 
     * @param response
     */
    public void callback(Object response);

    /**
     * 
     * @param cause
     * @param attachment
     */
    public void processException(Throwable cause, Object attachment);

}
