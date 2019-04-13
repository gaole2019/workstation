package org.janelia.workstation.browser.api.services;

import org.janelia.workstation.integration.framework.system.ProgressHandler;
import org.janelia.workstation.browser.gui.components.ProgressTopComponent;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

@ServiceProvider(service = ProgressHandler.class, path=ProgressHandler.LOOKUP_PATH)
public class ConsoleProgressHandler implements ProgressHandler {

    @Override
    public void showProgressPanel() {
        TopComponent tc = WindowLocator.getByName(ProgressTopComponent.PREFERRED_ID);
        if (tc!=null) {
            tc.open();
            tc.requestVisible();
        }
    }
    
}
