package org.janelia.console.viewerapi.commands;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;

/**
 * Seeds a new neuron with a single root anchor
 * @author brunsc
 */
public class SelectPrimaryAnchorCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command, Notifier
{
    private final NeuronVertex newPrimary;
    private final NeuronSet workspace;
    private NeuronVertex oldPrimary;
    private boolean doNotify = true;
    
    public SelectPrimaryAnchorCommand(NeuronSet workspace, NeuronVertex primary)
    {
        this.workspace = workspace;
        newPrimary = primary;
    }

    @Override
    public boolean execute() {
        if (workspace == null)
            return false;
        oldPrimary = workspace.getPrimaryAnchor();
        if (oldPrimary == newPrimary)
            return false;
        workspace.setPrimaryAnchor(newPrimary);
        if (workspace.getPrimaryAnchor() != newPrimary)
            return false;
        if (doesNotify()) {
            workspace.getPrimaryAnchorObservable().notifyObservers();
        }
        return true;
    }

    @Override
    public String getPresentationName() {
        if (newPrimary == null)
            return "Clear Parent Anchor";
        else 
            return "Set Parent Anchor";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        if (! execute())
            die(); // Something went wrong. This Command object is no longer useful.
    }

    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        if (workspace == null) {
            die();
            return;
        }
        try {
            workspace.setPrimaryAnchor(oldPrimary);
            if (workspace.getPrimaryAnchor() != oldPrimary) {
                die();
                return;
            }
            if (doesNotify()) {
                workspace.getPrimaryAnchorObservable().notifyObservers();
            }
        } catch (Exception exc) {
            die(); // This Command object is no longer useful
        }
    }

    @Override
    public void setNotify(boolean doNotify) {
        this.doNotify = doNotify;
    }

    @Override
    public boolean doesNotify() {
        return doNotify;
    }
}
