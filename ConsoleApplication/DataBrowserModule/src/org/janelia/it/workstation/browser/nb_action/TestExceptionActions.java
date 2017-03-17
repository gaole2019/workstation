package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Tools",
        id = "TestExceptionActions"
)
@ActionRegistration(
        displayName = "#CTL_TestExceptionActions",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Tools", position = 5000, separatorBefore=4999)
})
@Messages("CTL_TestExceptionActions=Test Exceptions")
public final class TestExceptionActions extends AbstractAction implements Presenter.Menu {

    private JMenu subMenu;
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Do nothing. Action is performed by menu presenter.
    }

    private JMenuItem getSubMenu() {
        if (subMenu==null) {
            subMenu = new JMenu("Test Exceptions");

            JMenuItem edtItem = new JMenuItem("Unhandled EDT Exception");
            edtItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new IllegalStateException("Test Unhandled EDT Exception");
                }
            });
            subMenu.add(edtItem);

            JMenuItem edtBarrageItem = new JMenuItem("Unhandled EDT Exception (10)");
            edtBarrageItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for(int i=0; i<11; i++) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                throw new IllegalStateException("Test Unhandled EDT Exception");
                            }
                        });
                    }
                }
            });
            subMenu.add(edtBarrageItem);
            
            JMenuItem unexpectedItem = new JMenuItem("Unexpected Exception");
            unexpectedItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        try {
                            exceptionTest(); // Generate some causes to test the "Caused:" logging
                        }
                        catch (Exception ex2) {
                            throw new Exception("Exception wrapper", ex2);
                        }
                    }
                    catch (Exception ex) {
                        FrameworkImplProvider.handleException("Testing Unexpected Exception", ex);
                    }
                }
            });
            subMenu.add(unexpectedItem);
        }
        return subMenu;
    }

    private void exceptionTest() {
        exceptionTestInner();
    }
    
    private void exceptionTestInner() {
        throw new IllegalStateException("Test Unexpected Exception");
    }
    
    @Override
    public JMenuItem getMenuPresenter() {
        if (isAccessible()) {
            return getSubMenu();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return isAccessible();
    }

    public static boolean isAccessible() {
        return AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
    }
}