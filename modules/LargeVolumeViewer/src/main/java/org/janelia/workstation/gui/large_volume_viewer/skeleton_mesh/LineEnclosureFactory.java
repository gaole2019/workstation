package org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoKey;
import org.janelia.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can build up mesh enclosures over lines.  Lines are defined as pairs
 * of endpoints.
 * 
 * @author fosterl
 */
public class LineEnclosureFactory implements TriangleSource {
    
    private static final Logger logger = LoggerFactory.getLogger(LineEnclosureFactory.class);
	public static final double ZERO_TOLERANCE = 0.3;
    private static final int X = 0, Y = 1, Z = 2;
	private static final int PERPENDICULAR_ALIGNMENT = 100;
    private static final double PI_DIV_4 = Math.PI / 4.0;

    private final List<VertexInfoBean> vertices = new ArrayList<>();
    private final List<Triangle> triangles = new ArrayList<>();
    private final ViewMatrixSupport matrixUtils = new ViewMatrixSupport();
    
    private int endPolygonSides = -1;
    
    private final Map<Integer,double[][]> axisAlignedPrototypePolygons = new HashMap<>();
    private double[][] zAxisAlignedPrototypePolygon;
    
    private final List<double[][]> endCapPolygonsHolder = new ArrayList<>();
    
    private PolygonSource polygonSource;
    private boolean includeIDs = true;
    private final VertexNumberGenerator vertexNumberGenerator;
    
    public LineEnclosureFactory(int endPolygonSides, double endPolygonRadius, VertexNumberGenerator vertexNumberGenerator) {
        setCharacteristics(endPolygonSides, endPolygonRadius);
        this.vertexNumberGenerator = vertexNumberGenerator;
    }
    
    public final void setCharacteristics( int endPolygonSides, double endPolygonRadius ) {
        polygonSource = new PolygonSource(endPolygonSides, endPolygonRadius);
        axisAlignedPrototypePolygons.clear();
        
        this.endPolygonSides = endPolygonSides;
        this.zAxisAlignedPrototypePolygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
        axisAlignedPrototypePolygons.put(2, this.zAxisAlignedPrototypePolygon);
    }
    
    /** Override the default 'include ids in buffers' behavior. */
    public void setIncludeIDs( boolean value ) {
        this.includeIDs = value;
    }
    
    public int addEnclosure(double[] startingCoords, double[] endingCoords) {
        return addEnclosure(startingCoords, endingCoords, null);
    }
    
    /**
     * Makes a set of triangles (geometry) to make a "pipe" around
     * the pair of starting/ending coords.  May make overhangs for "elbow
     * convenience."
     * 
     * @param startingCoords one end.
     * @param endingCoords other end.
     * @param color applied to each vertex.
     * @return number of coordinates created here.
     */
    public synchronized int addEnclosure( double[] startingCoords, double[] endingCoords, float[] color ) {
        if ( startingCoords.length != 3 ) {
            throw new IllegalArgumentException("3-D starting coords only.");
        }
        if (endingCoords.length != 3) {
            throw new IllegalArgumentException("3-D ending coords only.");
        }
        List<double[][]> endCaps = getCachedEndcaps(startingCoords, endingCoords);
        // End caps can be null, if start/end are identical.
        if (endCaps == null) {
            return 0;
        }
        
        //List<double[][]> endCaps = makeEndPolygonsNoTrig( startingCoords, endingCoords );
        int coordCount = 0;
        List<VertexInfoBean> startVertices = addVertices(endCaps.get(0), color);
        
		coordCount += startVertices.size();
        List<VertexInfoBean> endVertices = addVertices(endCaps.get(1), color);
        
		coordCount += endVertices.size();
        createCCWTriangles(startVertices, endVertices);
        
        return coordCount;
    }

    //--------------------------------------------IMPLEMENT TriangleSource
    @Override
    public List<VertexInfoBean> getVertices() {
        logger.debug("Returning {} vertices.", vertices.size());
        return vertices;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    //--------------------------------------------HELPERS
	/**
	 * Adds vertices to the overall collection, and to the bean collection
	 * that represents this polygon.
	 * 
	 * @param poly simple array of coord triples.
     * @param color dimension 3; applied to every vertex created from polygon.
	 * @return list of coord beans.
	 */
    protected List<VertexInfoBean> addVertices(double[][] poly, float[] color) {
		List<VertexInfoBean> polyBeans = new ArrayList<>();
        for (int i = 0; i < poly.length; i++) {
            VertexInfoBean bean = new VertexInfoBean();
            VertexInfoKey key = new VertexInfoKey();
            key.setPosition(poly[i]);
            bean.setKey(key);
            // Color is optional, depending on application/caller.
            if ( color != null ) {
                bean.setAttribute(
                        VertexInfoBean.KnownAttributes.b_color.name(), color, 3
                );

                logger.debug("Color attribute = [{},{},{}].", color[0], color[1], color[2]);
            }
            // Must setup a dummy value, so that all vertices have same-sized data in buffer.
            if (includeIDs) {
                bean.setAttribute(
                        NeuronTraceVtxAttribMgr.ID_VTX_ATTRIB, new float[]{0,0,0}, 3
                );
            }
            addVertex(bean);
			polyBeans.add(bean);
            assert !(Double.isNaN(key.getPosition()[X]) || Double.isNaN(key.getPosition()[Y]) || Double.isNaN(key.getPosition()[Z])) : "Not-a-number in coordinate.";
            logger.debug("Adding vertex {},{},{}", key.getPosition()[X], key.getPosition()[Y], key.getPosition()[Z]);
        }
        return polyBeans;
    }
	
    private List<double[][]> getCachedEndcaps(double[] startingCoords, double[] endingCoords) {
        String endPointsKey = createEndPointsKey(startingCoords, endingCoords);
        List<double[][]> endCaps = LineEnclosurePrecomputes.getEndCaps(endPointsKey);
        if (endCaps == null) {
            endCaps = makeEndPolygons(startingCoords, endingCoords);
            if (endCaps != null && endCaps.size() == 2) {
                // The end-polygon-maker keeps a single list for all time.
                // Therefore, must re-copy that list.
                endCaps = new ArrayList<>(endCaps);
                LineEnclosurePrecomputes.putEndCaps(endPointsKey, endCaps);
            }
        }
        return endCaps;
    }

    private void createCCWTriangles(List<VertexInfoBean> startingVertices, List<VertexInfoBean> endingVertices) {			
		int vertsPerPoly = startingVertices.size();
		for ( int polygonVertex = 0; polygonVertex < vertsPerPoly; polygonVertex++ ) {
			// Add a triangle that demarks leading corner of rectangle, to
			// distant edge of rectangle, and back to starting corner.
			
			// 1xxxxxxxxx3
			// |    xxxxx|
			// |        x|
			// +---------2
			final int nextEdgeOffset = (polygonVertex + 1) % vertsPerPoly;
			Triangle triangle = new Triangle();

            final VertexInfoBean topLeft = startingVertices.get( polygonVertex );
			triangle.addVertex(topLeft);
            
            final VertexInfoBean bottomRight = endingVertices.get( nextEdgeOffset );
            triangle.addVertex( bottomRight );

            final VertexInfoBean topRight = endingVertices.get( polygonVertex );
			triangle.addVertex( topRight );
            
			triangles.add( triangle );
			
			// Add a triangle that demarks the leading edge of the rectangle,
			// to the farthest corner.
			
			// 4---------+
			// |x        |
			// |xxxxx    |
			// 5xxxxxxxxx6
			triangle = new Triangle();
			triangle.addVertex( topLeft );
            
            final VertexInfoBean bottomLeft = startingVertices.get( nextEdgeOffset );
			triangle.addVertex( bottomLeft );
            
			triangle.addVertex( bottomRight );
            
			triangles.add( triangle );
			
		}

        // Now treating the end caps. Their surface normals must be
        // treated differently from those of the surrounding cylinder.
        List<VertexInfoBean> startingVerticesClone = new ArrayList<>();
        for ( VertexInfoBean bean: startingVertices ) {
            final VertexInfoBean clonedBean = bean.cloneIt();            
            startingVerticesClone.add( clonedBean);
            addVertex( clonedBean );
        }
        // Winding to point close end away from tube.
		for ( int i = 0; i < (vertsPerPoly - 2); i++ ) {
			final Triangle triangle = new Triangle();
            triangle.addVertex(startingVerticesClone.get(0));
            triangle.addVertex(startingVerticesClone.get(i + 2));
            triangle.addVertex(startingVerticesClone.get(i + 1));
            // End cap polygons must not affect the vertex normals.
            triangle.setNormalCombinationParticant(false);
			triangles.add( triangle );
		}
        
		List<VertexInfoBean> endingVerticesClone = new ArrayList<>();
        for (VertexInfoBean bean: endingVertices) {
            final VertexInfoBean clonedBean = bean.cloneIt();
            endingVerticesClone.add( clonedBean );
            addVertex( clonedBean );
        }
		// Winding to point far end away from tube.
		for ( int i = 0; i < (vertsPerPoly - 2); i++ ) {
			final Triangle triangle = new Triangle();
            triangle.addVertex(endingVerticesClone.get(0));
            triangle.addVertex(endingVerticesClone.get(i + 1));
            triangle.addVertex(endingVerticesClone.get(i + 2));
            // End cap polygons must not affect the vertex normals.
            triangle.setNormalCombinationParticant(false);
			triangles.add( triangle );
		}
	}
    
    private List<double[][]> makeEndPolygons(double[] startCoords, double[] endCoords) {

        endCapPolygonsHolder.clear();
        final double[] lineDelta = getLineDelta(startCoords, endCoords);

        // Get the three angles: about X, about Y, about Z.
        double[] lineUnitVector = normalize(lineDelta);
        if (lineUnitVector == null) {
            return null;
        }

        double aboutX = lineUnitVector[Z] == 0 ? 0 : Math.atan(lineUnitVector[Y] / lineUnitVector[Z]);
        aboutX = placeRound(aboutX);
        double aboutY = lineUnitVector[Z] == 0 ? 0 : Math.atan(lineUnitVector[X] / lineUnitVector[Z]);
        aboutY = placeRound(aboutY);
        double aboutZ = lineUnitVector[X] == 0 ? 0 : Math.atan(lineUnitVector[Y] / lineUnitVector[X]);
        aboutZ = placeRound(aboutZ);

        logger.debug("Using angles: {}, {}, {}.", Math.toDegrees(aboutX), Math.toDegrees(aboutY), Math.toDegrees(aboutZ));

        int axialAlignment = getAxialAlignmentByLineDelta(lineUnitVector);
        logger.debug("Aligned along the #{} axis.", axialAlignment);

        if (axialAlignment == -1) {
            if (lineUnitVector[Z] < 0) {
                // Switch start/end order if facing in negative direction.
                double[] tempCoords = startCoords;
                startCoords = endCoords;
                endCoords = tempCoords;
            }
            // Use different part of triangle to calculate atan, if not special axial alignment.
            if (Math.abs(aboutZ) > PI_DIV_4) {
                aboutZ = lineUnitVector[Y] == 0 ? 0 : Math.atan(lineUnitVector[X] / lineUnitVector[Y]);
                aboutZ = placeRound(aboutZ);
            }
            Matrix transform1 = getAboutXAboutYTransformMatrix(aboutX, aboutY);
            final double[][] startEndPolygon = clonePrototypePolygon(zAxisAlignedPrototypePolygon);
            transformPolygon(startEndPolygon, transform1);
            Matrix transform2 = getAboutZTransform(aboutZ);
            
            transform2.set(0, 3, startCoords[X]);
            transform2.set(1, 3, startCoords[Y]);
            transform2.set(2, 3, startCoords[Z]);
            transformPolygon(startEndPolygon, transform2);
            endCapPolygonsHolder.add(startEndPolygon);

            final double[][] endingEndPolygon = clonePrototypePolygon(zAxisAlignedPrototypePolygon);
            transformPolygon(endingEndPolygon, transform1);
            transform2.set(0, 3, endCoords[X]);
            transform2.set(1, 3, endCoords[Y]);
            transform2.set(2, 3, endCoords[Z]);
            transformPolygon(endingEndPolygon, transform2);
            endCapPolygonsHolder.add(endingEndPolygon);
        } else if (axialAlignment == PERPENDICULAR_ALIGNMENT) {
			// Special case: new normal lies in the xy plane, but not on an axis.
            // Only spin about Z.
            if (lineUnitVector[X] > 0) {
                // Switch start/end order if facing in negative direction.
                double[] tempCoords = startCoords;
                startCoords = endCoords;
                endCoords = tempCoords;
            }
            Matrix transform = getAboutZTransform(aboutZ);
            transform.set(0, 3, startCoords[X]);
            transform.set(1, 3, startCoords[Y]);
            transform.set(2, 3, startCoords[Z]);
            
            double[][] prototypePolygon = getPrototypePolygon(X);
            final double[][] startEndPolygon = clonePrototypePolygon(prototypePolygon);
            transformPolygon(startEndPolygon, transform);
            endCapPolygonsHolder.add(startEndPolygon);

            final double[][] endingEndPolygon = clonePrototypePolygon(prototypePolygon);
            transform.set(0, 3, endCoords[X]);
            transform.set(1, 3, endCoords[Y]);
            transform.set(2, 3, endCoords[Z]);
            transformPolygon(endingEndPolygon, transform);
            endCapPolygonsHolder.add(endingEndPolygon);
        } else {
            // Special case: aligned right along some axis.  Trig assumptions won't help.
            boolean mustSwitch = false;
            if (lineUnitVector[axialAlignment] < 0 && axialAlignment == Z) {
                mustSwitch = true;
            } else if (lineUnitVector[axialAlignment] > 0 && axialAlignment != Z) {
                mustSwitch = true;
            }
            if (mustSwitch) {
                // Switch start/end order if facing in negative direction.
                double[] tempCoords = startCoords;
                startCoords = endCoords;
                endCoords = tempCoords;
            }
            Matrix transform = matrixUtils.getTranslateTransform3D(
                    startCoords[X], startCoords[Y], startCoords[Z]
            );
            double[][] prototypePolygon = getPrototypePolygon(axialAlignment);
            endCapPolygonsHolder.add(producePolygon(transform, prototypePolygon));
            transform.set(0, 3, endCoords[X]);
            transform.set(1, 3, endCoords[Y]);
            transform.set(2, 3, endCoords[Z]);
            endCapPolygonsHolder.add(producePolygon(transform, prototypePolygon));
        }
        return endCapPolygonsHolder;
    }

    private Matrix getAboutXAboutYTransformMatrix(double aboutX, double aboutY) {
        // Now that we have our angles, we make transforms.
        String xyKey = createXYKey(-aboutX, aboutY);
        Matrix transform1 = LineEnclosurePrecomputes.getAboutXAboutYMatrix(xyKey);
        if (transform1 == null) {
            transform1 = matrixUtils.getTransform3D(
                    -aboutX, aboutY, 0,
                    0, 0, 0);
            LineEnclosurePrecomputes.putAboutXAboutYMatrix(xyKey, transform1);
        }
        return transform1;
    }

    private Matrix getAboutZTransform(double aboutZ) {
        final Integer aboutZKey = convertPlaceRounded( aboutZ );
        Matrix transform = LineEnclosurePrecomputes.getAboutZMatrix( aboutZKey );
        if (transform == null) {
            transform = matrixUtils.getTransform3D(
                    0, 0, aboutZ,
                    0, 0, 0);
            LineEnclosurePrecomputes.putAboutZMatrix(aboutZKey, transform);
        }
        return transform;
    }
    
    private double placeRound(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
    
    private Integer convertPlaceRounded(double val) {
        return (int)(val * 100);
    }

    private String createXYKey(double aboutX, double aboutY) {
        return Integer.toString((int)(aboutX * 100.0)) + ',' + Integer.toString((int)(aboutY * 100.0));
    }
    
    /**
     * Makes a string-comparable key out of end-point 3D coordinates, and poly-sides.
     */
    private String createEndPointsKey(double[] startingCoords, double[] endingCoords) {
        return String.format(
                "%11.3f,%11.3f%11.3f_%11.3f,%11.3f,%11.3f_%d", 
                startingCoords[0],
                startingCoords[1],
                startingCoords[2],
                endingCoords[0],
                endingCoords[1],
                endingCoords[2],
                endPolygonSides
        );
    }
    
	private int getAxialAlignmentByLineDelta(double[] lineDelta) {
		double deltaX = Math.abs(lineDelta[X]);
		double deltaY = Math.abs(lineDelta[Y]);
		double deltaZ = Math.abs(lineDelta[Z]);
		if (deltaX < ZERO_TOLERANCE  &&  deltaY < ZERO_TOLERANCE) {
			return Z;
		}
		else if (deltaX < ZERO_TOLERANCE && deltaZ < ZERO_TOLERANCE) {
			return Y;
		}
		else if (deltaY < ZERO_TOLERANCE && deltaZ < ZERO_TOLERANCE) {
			return X;
		}
		else if (deltaZ < ZERO_TOLERANCE ) {
			return PERPENDICULAR_ALIGNMENT;
		}
		else {
			return -1;
		}
	}
	
	private double[][] getPrototypePolygon(int axialAlignment) {
		if (axisAlignedPrototypePolygons.get(axialAlignment) == null) {
			axisAlignedPrototypePolygons.put(axialAlignment, polygonSource.createAxisAlignedPrototypeEndPolygon(axialAlignment));
		}
		return axisAlignedPrototypePolygons.get(axialAlignment);
	}

    private double[] normalize( double[] distance ) {
        double magnitude = getMagnitude( distance );
        if (magnitude < 0.000001) {
            return null;
        }
        distance[0] /= magnitude;
        distance[1] /= magnitude;
        distance[2] /= magnitude;
        return distance;
    }

    public double getMagnitude(double[] distance) {
        double magnitude = Math.sqrt( distance[0] * distance[0] + distance[1] * distance[1] + distance[2] * distance[2] );
        return magnitude;
    }
    
    private double[] getLineDelta( double[] startCoords, double[] endCoords ) {
        double[] delta = new double[ startCoords.length ];
        for ( int i = 0; i < startCoords.length; i++ ) {
            delta[ i ] = startCoords[ i ] - endCoords[ i ];
        }
        return delta;
    }
    
    protected double[][] clonePrototypePolygon(double[][] prototype) {
        //dumpTransform(transform);
        // Clone the prototype.
        double[][] polygon = new double[endPolygonSides][];
        for (int i = 0; i < endPolygonSides; i++) {
            polygon[i] = Arrays.copyOf(prototype[i], 3);
        }
        return polygon;
    }

    protected double[][] producePolygon(Matrix transform, double[][] prototype) {
        double[][] polygon = clonePrototypePolygon(prototype);
		transformPolygon(polygon, transform);
        return polygon;
    }

	private void transformPolygon(double[][] polygon, Matrix transform) {
		for (int i = 0; i < polygon.length; i++) {
			polygon[i] = matrixUtils.transform(transform, polygon[i]);
		}
	}

    private void addVertex(VertexInfoBean vertex) {
        vertex.setVtxBufOffset(vertexNumberGenerator.allocateVertexNumber());
        vertices.add(vertex);
    }

    @SuppressWarnings("unused")
    protected void dumpTransform(Matrix transform) {
        System.out.println("----TRANSFORM----");
        for (int i = 0; i < transform.getRowDimension(); i++) {
            for (int j = 0; j < transform.getColumnDimension(); j++) {
                System.out.print(" " + transform.get(i, j));
            }
            System.out.println();
        }
    }
    
}
