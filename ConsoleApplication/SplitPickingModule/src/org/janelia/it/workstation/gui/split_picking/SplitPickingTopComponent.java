package org.janelia.it.workstation.gui.split_picking;

import java.awt.BorderLayout;
import java.util.Properties;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component for the split picking workflow panel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.split_picking//SplitPicking//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = SplitPickingTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false, position = 300)
@ActionID(category = "Window", id = "SplitPickingTopComponent")
@ActionReference(path = "Menu/Window", position = 300)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SplitPickingAction",
        preferredID = SplitPickingTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_SplitPickingAction=Split Picking Tool",
    "CTL_SplitPickingTopComponent=Split Picking Workflow",
    "HINT_SplitPickingTopComponent=Choosing from two lines to combine"
})
public final class SplitPickingTopComponent extends TopComponent {
    
    private Logger log = LoggerFactory.getLogger( SplitPickingTopComponent.class );

    public static final String PREFERRED_ID = "SplitPickingTopComponent";
    
    private final SplitPickingPanel splitPickingPanel;
    
    public SplitPickingTopComponent() {
        initComponents();
        splitPickingPanel = new SplitPickingPanel();
        setName(Bundle.CTL_SplitPickingTopComponent());
        setToolTipText(Bundle.HINT_SplitPickingTopComponent());
    }

    private void initMyComponents() {
        jPanel1.add( splitPickingPanel, BorderLayout.CENTER );
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new JPanel();

        jPanel1.setLayout(new BorderLayout());

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        initMyComponents();
        splitPickingPanel.refresh();
    }

    @Override
    public void componentClosed() {
         // Closing the group doesn't seem to work, so we close the lanes explicitely
         TopComponent tc = WindowLocator.getByName(SplitPickingLanesTopComponent.PREFERRED_ID);
         if (tc!=null) tc.close();
         
    }

    void writeProperties(Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
