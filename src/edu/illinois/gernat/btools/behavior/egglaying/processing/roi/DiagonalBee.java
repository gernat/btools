package edu.illinois.gernat.btools.behavior.egglaying.processing.roi;

import edu.illinois.gernat.btools.behavior.egglaying.geometry.Point2d;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.ROI;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.Vector2d;
import edu.illinois.gernat.btools.behavior.egglaying.io.Bee;

/**
 * Created by tobias on 16.10.16.
 */
public class DiagonalBee implements ROICalculator {

    private final double padding;

    public DiagonalBee(double padding) {
        this.padding = padding;
    }

    /**
     * Calculates the ROI for the small image to train or use the neural network (2. stage).
     *
     * @param bee information about the bee.
     * @return the roi definition
     */
    public ROI calcROI(Bee bee) {
        double angle = bee.getOrientation().signedAngle(new Vector2d(1, 1).normalize());
        Point2d upperLeft = bee.getPosition().scaleAdd(111.0 + this.padding * Math.sqrt(2.0), bee.getOrientation().normalize()).scaleAdd(256.0 + this.padding * 2.0, new Vector2d(-1, 0).rotate(-angle));

        return new ROI(upperLeft, -angle, 256 + this.padding * 2, 256 + this.padding * 2);
    }
}
