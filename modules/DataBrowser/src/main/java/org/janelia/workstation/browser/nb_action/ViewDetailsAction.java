package org.janelia.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "ViewDetailsAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewDetailsAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1537, separatorBefore = 1531),
    @ActionReference(path = "Shortcuts", name = "D-I")
})

@Messages("CTL_ViewDetailsAction=View Details")
public final class ViewDetailsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            Reference lastSelected = GlobalDomainObjectSelectionModel.getInstance().getLastSelectedId();
            if (lastSelected!=null) {
                DomainObject domainObject = model.getDomainObject(lastSelected);
                if (domainObject!=null) {
                    new DomainDetailsDialog().showForDomainObject(domainObject);
                }
            }
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }
}
