package org.janelia.horta.actions;

/**
 *
 * @author schauderd
 */

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "StopPlayReviewAction"
)
@ActionRegistration(
        displayName = "Stop Playback",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "C-S")
})
public class StopPlayReviewAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(StopPlayReviewAction.class);
    public StopPlayReviewAction() {
        super("Stop Play Review");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
       NeuronTracerTopComponent nttc = NeuronTracerTopComponent.findThisComponent();
       nttc.stopPlaybackReview();
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
