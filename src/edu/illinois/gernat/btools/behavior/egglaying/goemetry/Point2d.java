package com.tjagla.goemetry;

/**
 * Created by tobias on 23.09.16.
 */
public class Point2d extends Tuple2d {
    public static final Point2d ORIGIN = new Point2d(0, 0);

    public Point2d(double x, double y) {
        super(x, y);
    }

    public Point2d(Tuple2d t) {
        super(t);
    }

    public Point2d add(Vector2d vec) {
        return scaleAdd(1.0, vec);
    }

    public Point2d scaleAdd(double scale, Vector2d vec) {
        return new Point2d(this.x + scale * vec.x, this.y + scale * vec.y);
    }

    /**
     * Switches between world coordinates and image coordinates. Be careful! The current state is
     * not tracked by this class. You have to do it by your own.
     *
     * @return a position in the other coordinate system (world or image)
     */
    public Point2d getSwitchedCoordinates() {
        return new Point2d(this.x, -this.y);
    }

    public Vector2d to(Point2d p) {
        return new Vector2d(p.x - this.x, p.y - this.y);
    }

    public Vector2d getPointVector() {
        return new Vector2d(this.x, this.y);
    }

    public double getEuclideanDistance(Point2d p) {
        return this.to(p).getLength();
    }
}
