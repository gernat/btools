package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi;

import edu.illinois.gernat.btools.common.geometry.Coordinate;

/**
 * Created by tobias on 27.09.16.
 */
public class CenterROI {
    public final Coordinate center;
    public final double rotAngle;
    public final double width;
    public final double adjust;
    public final double height;

    public CenterROI(Coordinate center, double rotAngle, double width, double adjust, double height) {
        this.center = center;
        this.rotAngle = rotAngle;
        this.width = width;
        this.adjust = adjust;
        this.height = height;
    }
}
