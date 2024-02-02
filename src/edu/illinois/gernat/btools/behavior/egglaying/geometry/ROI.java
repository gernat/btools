package edu.illinois.gernat.btools.behavior.egglaying.geometry;

/**
 * Created by tobias on 27.09.16.
 */
public class ROI {
    public final Point2d upperLeft;
    public final double rotAngle;
    public final double width;
    public final double height;

    public ROI(Point2d upperLeft, double rotAngle, double width, double height) {
        this.upperLeft = upperLeft;
        this.rotAngle = rotAngle;
        this.width = width;
        this.height = height;
    }
}
