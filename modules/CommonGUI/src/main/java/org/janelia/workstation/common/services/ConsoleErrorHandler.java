package org.janelia.workstation.common.services;

import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.hibernate.exception.ExceptionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.integration.api.ErrorHandler;
import org.janelia.workstation.core.model.ErrorType;
import org.janelia.workstation.common.gui.dialogs.LoginDialog;
import org.janelia.workstation.core.logging.CustomLoggingLevel;
import org.openide.util.lookup.ServiceProvider;

/**
 * This error-handler impl defers to the NetBeans logging framework.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ErrorHandler.class, path=ErrorHandler.LOOKUP_PATH)
public class ConsoleErrorHandler implements ErrorHandler {

    private static final Logger logger = Logger.getLogger(ConsoleErrorHandler.class.getName());
    
    @Override
    public void handleException(Throwable t) {
        handleException(null, t);
    }

    @Override
    public void handleException(String message, Throwable t) {
        Throwable rootCause = ExceptionUtils.getRootCause(t);

        if (rootCause instanceof java.net.ConnectException 
                || rootCause instanceof java.net.SocketTimeoutException) {
            
            handleExceptionQuietly(message, t);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "<html>The server is currently unreachable. There may be a <br>"
                        + "network issue, or the system may be down for maintenance.</html>", 
                        "Network error", JOptionPane.ERROR_MESSAGE);
            });
            
        }
        else if ("HTTP 401 Unauthorized".equalsIgnoreCase(t.getMessage())) {
            // These happen if the token expires and cannot be refreshed. 
            handleExceptionQuietly(message, t);
            // Show the login dialog and allow the user to re-authenticate.
            SwingUtilities.invokeLater(() -> {
                LoginDialog.getInstance().showDialog(ErrorType.TokenExpiredError);
            });
        }
        else {
            logger.log(CustomLoggingLevel.USER_ERROR, message, t);
        }
    }

    @Override
    public void handleExceptionQuietly(Throwable t) {
        handleExceptionQuietly(null, t);
    }

    @Override
    public void handleExceptionQuietly(String message, Throwable t) {
        logger.log(CustomLoggingLevel.USER_WARN, message, t);
    }
}
