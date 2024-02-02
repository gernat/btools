package edu.illinois.gernat.btools.behavior.egglaying.goemetry;

/**
 * Created by tobias on 23.09.16.
 */
public class Tuple2d implements Cloneable {
    private static final double EPSILON = 1.0E-8;

    public final double x;
    public final double y;

    public Tuple2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Tuple2d(Tuple2d t) {
        this.x = t.x;
        this.y = t.y;
    }

    public Tuple2d scale(double scale) {
        return new Tuple2d(this.x * scale, this.y * scale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple2d tuple2d = (Tuple2d) o;

        return tuple2d.x - x < EPSILON && tuple2d.y - y < EPSILON;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "[" + x +
                "," + y +
                ']';
    }
}
