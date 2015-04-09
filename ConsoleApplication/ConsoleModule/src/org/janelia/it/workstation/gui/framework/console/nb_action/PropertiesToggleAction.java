package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.BooleanStateAction;

@ActionID(
        category = "View",
        id = "PropertiesToggleAction"
)
@ActionRegistration(
        displayName = "#CTL_PropertiesToggleAction",
        lazy = true
)
@ActionReference(path = "Menu/View", position = 50)
@Messages("CTL_PropertiesToggleAction=Right Panel")
public final class PropertiesToggleAction extends BooleanStateAction {

    private static final String PROPERTIES_PANEL_SHOWN = "Right Panel";
    
    public PropertiesToggleAction() {
        setBooleanState( true );
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        setBooleanState( ! getBooleanState() );
        ViewActionDelegate viewActionDelegate = new ViewActionDelegate();
        viewActionDelegate.toggleOntology( getBooleanState() );
    }

    @Override
    public String getName() {
        return PROPERTIES_PANEL_SHOWN;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }
}
