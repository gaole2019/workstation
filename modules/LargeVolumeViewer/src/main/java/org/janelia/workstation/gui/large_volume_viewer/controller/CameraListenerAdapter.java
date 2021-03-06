package org.janelia.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Extend this with a listener that does not need to hear all information.
 * Convenience class only.
 * @author fosterl
 */
public class CameraListenerAdapter implements CameraListener {

    @Override
    public void viewChanged() {}

    @Override
    public void zoomChanged(Double zoom) {}

    @Override
    public void focusChanged(Vec3 focus) {}

}
