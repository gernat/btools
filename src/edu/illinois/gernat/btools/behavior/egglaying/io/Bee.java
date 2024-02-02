package edu.illinois.gernat.btools.behavior.egglaying.io;

import edu.illinois.gernat.btools.behavior.egglaying.goemetry.Point2d;
import edu.illinois.gernat.btools.behavior.egglaying.goemetry.Vector2d;

/**
 * Created by tobias on 29.09.16.
 */
public interface Bee {
    int getId();

    float getX();

    float getY();

    float getDx();

    float getDy();

    Point2d getPosition();

    Vector2d getOrientation();

    float[] getCorners();
}
