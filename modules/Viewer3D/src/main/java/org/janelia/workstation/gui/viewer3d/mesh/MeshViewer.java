package org.janelia.workstation.gui.viewer3d.mesh;

import org.janelia.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.workstation.gui.viewer3d.Viewer3d;
import org.janelia.workstation.gui.viewer3d.VolumeModel;

/**
 * Special viewer to support carrying around extra information needed for mesh.
 *
 * @author fosterl
 */
public class MeshViewer extends Viewer3d {
    private MeshViewContext context;

    // Setup oversampling to get smoother rendering.
    static {
        capabilities.setSampleBuffers(true);
        capabilities.setNumSamples(4);
    }
    
	public MeshViewer() {
        final MeshRenderer meshRenderer = new MeshRenderer();
        setActorRenderer( meshRenderer);
        context = new MeshViewContext();
        super.setVolumeModel(context);
        meshRenderer.setMeshViewContext(context);
    }
    
    @Override
    public VolumeModel getVolumeModel() {
        return context;
    }
    
    public MeshViewContext getMeshViewContext() {
        return context;
    }
}
