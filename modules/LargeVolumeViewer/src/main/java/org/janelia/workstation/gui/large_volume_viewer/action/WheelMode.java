package org.janelia.workstation.gui.large_volume_viewer.action;

import org.janelia.workstation.gui.camera.Camera3d;
import org.janelia.workstation.gui.large_volume_viewer.MouseModalWidget;

import java.awt.event.MouseWheelListener;

public interface WheelMode
extends MouseWheelListener
{
    enum Mode {
        ZOOM,
        SCAN
    }
	public MouseModalWidget getWidget();
	public void setWidget(MouseModalWidget widget, boolean updateCursor);
	Camera3d getCamera();
	void setCamera(Camera3d camera);
}
