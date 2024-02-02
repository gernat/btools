package com.tjagla.goemetry;

/**
 * Created by tobias on 23.09.16.
 */
public class Vector2d extends Tuple2d {
    public static final Vector2d ABS_NORTH = new Vector2d(0, 1);

    public Vector2d(double x, double y) {
        super(x, y);
    }

    public Vector2d(Tuple2d t) {
        super(t);
    }

    public Vector2d rotateLeftHalfPi() {
        //noinspection SuspiciousNameCombination
        return new Vector2d(-this.y, this.x);
    }

    public double signedAngle(Vector2d vec) {
        return Math.atan2(vec.y, vec.x) - Math.atan2(this.y, this.x);
    }

    public Vector2d negate() {
        return new Vector2d(-this.x, -this.y);
    }

    public Vector2d normalize() {
        double scale = 1.0D / Math.sqrt(this.x * this.x + this.y * this.y);
        return new Vector2d(this.x * scale, this.y * scale);
    }

    public double getLength() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public Vector2d rotate(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vector2d(this.x * c - y * s, this.x * s + y * c);
    }
}
