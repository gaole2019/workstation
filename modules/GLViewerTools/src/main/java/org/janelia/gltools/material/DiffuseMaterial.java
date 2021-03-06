
package org.janelia.gltools.material;

import javax.media.opengl.GL2ES2;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderStep;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class DiffuseMaterial extends BasicMaterial
{
    public DiffuseMaterial() {
        shaderProgram = new DiffuseShader();
        setShadingStyle(Shading.FLAT);
    }

    @Override
    public boolean usesNormals() {
        return true;
    }

    private static class DiffuseShader extends BasicShaderProgram {
        public DiffuseShader() {
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_VERTEX_SHADER, ""
                    + "#version 330 \n"
                    + " \n"
                    + "uniform mat4 modelViewMatrix = mat4(1); \n"
                    + "uniform mat4 projectionMatrix = mat4(1); \n"
                    + " \n"
                    + "in vec3 position; \n"
                    + "in vec3 normal; \n"
                    + " \n"
                    + "out vec3 fragNormal; \n"
                    + "out vec4 fragPosition; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  fragPosition = modelViewMatrix * vec4(position, 1); \n"
                    + "  gl_Position = projectionMatrix * fragPosition; \n"
                    + "  // TODO - for more generality, use the NormalMatrix \n"
                    + "  fragNormal = (modelViewMatrix * vec4(normal, 0)).xyz; \n"
                    + "} \n")
            );
            getShaderSteps().add(new ShaderStep(
                    GL2ES2.GL_FRAGMENT_SHADER, ""
                    // TODO - expose lighting parameters as uniform variables
                    // TODO - permit per-vertex lighting
                    + "#version 330 \n"
                    + " \n"
                    + "in vec3 fragNormal; \n"
                    + "in vec4 fragPosition; \n"
                    + " \n"
                    + "const vec4 lightPosition = vec4(10, 10, 10, 1); \n"
                    + "const vec3 lightColor = vec3(1, 1, 1); \n"
                    + "const float ambientScale = 0.3; \n"
                    + "const float diffuseScale = 0.7; \n"
                    + "const vec3 ambientColor = vec3(0.6, 0.6, 1.0); \n"
                    + "const vec3 diffuseColor = vec3(1.0, 0.8, 0.5); \n"
                    + " \n"
                    + "out vec4 fragColor; \n"
                    + " \n"
                    + "void main(void) \n"
                    + "{ \n"
                    + "  vec3 n = normalize(fragNormal); \n"
                    + "  vec3 L = lightPosition.xyz - lightPosition.w * fragPosition.xyz; \n"
                    + "  L = normalize(L); \n"
                    + "  vec3 ambient = ambientScale * ambientColor; \n"
                    + "  vec3 diffuse = diffuseScale * max(dot(n, L), 0) * diffuseColor; \n"
                    + "  fragColor = vec4(diffuse + ambient, 1); \n"
                    + "} \n")
            );
        }
    }
}
