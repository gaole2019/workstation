package org.janelia.workstation.gui.large_volume_viewer.action;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.workstation.gui.camera.Camera3d;
import org.janelia.workstation.gui.large_volume_viewer.MouseModalWidget;
import org.janelia.workstation.gui.viewer3d.interfaces.VolumeImage3d;

import java.awt.event.MouseWheelEvent;

public class ZScanMode 
implements WheelMode
{
	BasicMouseMode mode = new BasicMouseMode();
	SliceScanAction sliceScanAction;

	public ZScanMode(VolumeImage3d image) {
		this.sliceScanAction = new SliceScanAction(image, mode.getCamera(), 1);
	}
	
	@Override
	public MouseModalWidget getWidget() {
		return mode.getWidget();
	}

	@Override
	public Camera3d getCamera() {
		return mode.getCamera();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) 
	{
		Camera3d camera = getCamera();
		if (camera == null)
			return;
		int notches = event.getWheelRotation();
		if (notches == 0)
			return;
		
		int deltaZ = 1;

		sliceScanAction.setSliceCount(notches*deltaZ);
		sliceScanAction.actionPerformed(null);
	}

	@Override
	public void setCamera(Camera3d camera) {
		mode.setCamera(camera);
		sliceScanAction.setCamera(camera);
	}

	@Override
	public void setWidget(MouseModalWidget widget, boolean updateCursor) {
		mode.setWidget(widget, updateCursor);
	}
	
	public void setTileFormat(TileFormat tileFormat) {
		sliceScanAction.setTileFormat(tileFormat);
	}
	
	public void setSliceAxis(CoordinateAxis axis) {
	    sliceScanAction.setSliceAxis(axis);
	}
}
