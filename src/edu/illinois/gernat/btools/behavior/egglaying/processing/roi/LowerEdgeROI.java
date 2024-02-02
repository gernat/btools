package edu.illinois.gernat.btools.behavior.egglaying.processing.roi;

import edu.illinois.gernat.btools.behavior.egglaying.geometry.Line2d;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.Point2d;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.ROI;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.Vector2d;
import edu.illinois.gernat.btools.behavior.egglaying.io.Bee;

/**
 * Created by tobias on 02.10.16.
 */
public class LowerEdgeROI implements ROICalculator {

    private final double padding;
    private final double width;
    private final double height;

    public LowerEdgeROI(double padding, double width, double height) {
        this.padding = padding;
        this.width = width;
        this.height = height;
    }

    /**
     * Shifts the lower bCode edge parallel in south (negated moveTriangle) direction (padding is
     * the length). From the midpoint of the edge it goes the half width on this line in west
     * direction to determine the upper left corner of the roi.
     *
     * @param bee all metadata from the bee, including id, center and moveTriangle (bCode), maybe
     *            the corners of the bCode
     * @return upper left corner and the rotation angle of the interesting region of the bee
     */
    public ROI calcROI(Bee bee) {
        Vector2d north = bee.getOrientation().normalize();
        float[] corners = bee.getCorners();
        Vector2d south = north.negate();

        Line2d lowerBCodeEdge = new Line2d(new Point2d(corners[4], corners[5]), new Point2d(corners[2], corners[3]));
        Point2d lowerMidPoint = lowerBCodeEdge.getMidpoint();
        Vector2d west = lowerBCodeEdge.getDirection().normalize();

        double rotAngle = Vector2d.ABS_NORTH.signedAngle(west.negate().rotateLeftHalfPi());

        Point2d upperCenter = lowerMidPoint.scaleAdd(padding, south);
        Point2d upperLeft = upperCenter.scaleAdd(width / 2.0, west);

        return new ROI(upperLeft, rotAngle, width, height);
    }
}
