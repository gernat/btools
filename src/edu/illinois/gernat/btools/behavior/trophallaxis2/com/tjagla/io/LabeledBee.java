package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io;


/**
 * Created by tobias on 21.09.16.
 */
public class LabeledBee implements Bee {
    private final int id;
    private final float x;
    private final float y;
    private final float dx;
    private final float dy;
    private final int label;
    private final float[] corners;
    private double length;
    private String fName;

    public LabeledBee(int id, float x, float y, float dx, float dy, int label) {
        this(id, x, y, dx, dy, label, null);
    }

    public LabeledBee(int id, float x, float y, float dx, float dy, int label, float[] corners) {
        this(id, x, y, dx, dy, label, corners, null);
    }

    public LabeledBee(int id, float x, float y, float dx, float dy, int label, float[] corners, String fName) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.label = label;
        this.corners = corners;
        this.fName = fName;
    }

    public int getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }

    public int getLabel() {
        return label;
    }

    public float[] getCorners() {
        return corners;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }

    public String getfName() {
        return fName;
    }
}
