/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.nodes;

import org.janelia.horta.modelapi.HortaWorkspace;
import java.awt.BorderLayout;
import java.util.Collection;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.OutlineView;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
@TopComponent.Description(
        preferredID = "HortaSceneEditorTopComponent", 
        iconBase = "org/janelia/horta/images/brain-icon2.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS) 
@TopComponent.Registration( 
        mode = "properties", 
        openAtStartup = false) 
@ActionID( 
        category = "Window", id = "org.janelia.horta.nodes.HortaSceneEditorTopComponent") 
@ActionReference( 
        path = "Menu/Window/Horta") 
@TopComponent.OpenActionRegistration( 
        displayName = "#CTL_HortaSceneEditorAction",
        preferredID = "HortaSceneEditorTopComponent"
) 
@NbBundle.Messages({ 
    "CTL_HortaSceneEditorAction=Scene Editor",
    "CTL_HortaSceneEditorTopComponent=Scene Editor",
    "HINT_HortaSceneEditorTopComponent=Horta Scene Editor"
})
public class HortaSceneEditorTopComponent extends TopComponent
implements ExplorerManager.Provider,  LookupListener
{
    // private final InstanceContent content = new InstanceContent();
    private final ExplorerManager mgr = new ExplorerManager();

    // private HortaWorkspace workspace = null;
    private Lookup.Result<HortaWorkspace> workspaceResult = null;
    private HortaWorkspace cachedWorkspace = null;
    
    // https://platform.netbeans.org/tutorials/74/nbm-selection-2.html
    // private final BeanTreeView treeView = new BeanTreeView();
    // child actions may reveal better with OutlineView than with BeanTreeView
    private final OutlineView treeView = new OutlineView("Scene Items"); 

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates new form HortaWorkspaceEditorTopComponent
     */
    public HortaSceneEditorTopComponent()
    {
        initComponents();
        setName(Bundle.CTL_HortaSceneEditorTopComponent());
        setToolTipText(Bundle.HINT_HortaSceneEditorTopComponent());
        setDisplayName("Horta Scene Editor");
        
        treeView.addPropertyColumn("visible", "Visible?");
        treeView.addPropertyColumn("size", "Size");
        treeView.addPropertyColumn("color", "Color");

        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));
        
        setLayout(new BorderLayout());
        add(
               treeView, 
               BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    @Override
    public ExplorerManager getExplorerManager()
    {
        return mgr;
    }
    
    @Override
    public void componentOpened() {
        workspaceResult = Utilities.actionsGlobalContext().lookupResult(HortaWorkspace.class);
        workspaceResult.addLookupListener(this);
    }
    
    @Override 
    public void componentClosed() {
        workspaceResult.removeLookupListener(this);
    }
    
    @Override 
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends HortaWorkspace> allWorkspaces = workspaceResult.allInstances();
        if (allWorkspaces.isEmpty()) {
            // mgr.setRootContext(null); 
            return;
        }
        HortaWorkspace workspace = allWorkspaces.iterator().next();
        if (workspace != cachedWorkspace) {
            logger.info("Creating new scene root");
            cachedWorkspace = workspace;
            mgr.setRootContext( new HortaWorkspaceNode(workspace) );
        }
    }

}
