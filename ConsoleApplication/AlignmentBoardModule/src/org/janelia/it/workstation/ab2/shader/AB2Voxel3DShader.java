package org.janelia.it.workstation.ab2.shader;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;

public class AB2Voxel3DShader extends GLShaderProgram {
    @Override
    public String getVertexShaderResourceName() { return "AB2Voxel3DShader_vertex.glsl"; }

    @Override
    public String getGeometryShaderResourceName() { return "AB2Voxel3DShader_geometry.glsl"; }

    @Override
    public String getFragmentShaderResourceName() {
        return "AB2Voxel3DShader_fragment.glsl";
    }

    public void setMVP(GL4 gl, Matrix4 mvp) {
        setUniformMatrix4fv(gl, "mvp", false, mvp.asArray());
        checkGlError(gl, "AB2Voxel3DShader setMVP() error");
    }

//    public void setColor(GL4 gl, Vector4 color) {
//        setUniform4v(gl, "color0", 1, color.toArray());
//        checkGlError(gl, "AB2Voxel3DShader setColor() error");
//    }

    public void setDimXYZ(GL4 gl, int x, int y, int z) {
        setUniform3v(gl, "dimXYZ", 1, new float[] { x*1f, y*1f, z*1f });
        checkGlError(gl, "AB2Voxel3DShader setDimXYZ() error");
    }

    public void setVoxelSize(GL4 gl, Vector3 voxelSize) {
        setUniform3v(gl, "voxelSize", 1, voxelSize.toArray());
        checkGlError(gl, "AB2Voxel3DShader setVoxelSize() error");
    }
}
