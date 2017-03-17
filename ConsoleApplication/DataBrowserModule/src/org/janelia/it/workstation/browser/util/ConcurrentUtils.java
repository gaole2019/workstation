package org.janelia.it.workstation.browser.util;

import java.util.concurrent.Callable;

import org.janelia.it.workstation.browser.ConsoleApp;

/**
 * Utilities for dealing with the Java concurrent library.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConcurrentUtils {

    public static void invokeAndHandleExceptions(Callable<?> callback) {
        if (callback!=null) {
            try {
                callback.call();
            }
            catch (Exception e) {
                ConsoleApp.handleException(e);
            }
        }
    }
    
    public static void invoke(Callable<?> callback) throws Exception {
        if (callback!=null) {
            callback.call();
        }
    }
}