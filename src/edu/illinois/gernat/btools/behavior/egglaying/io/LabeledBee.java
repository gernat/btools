/*
 * Copyright (C) 2016, 2024 University of Illinois Board of Trustees.
 *
 * This file is part of bTools.
 *
 * bTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * bTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bTools. If not, see http://www.gnu.org/licenses/.
 */

package edu.illinois.gernat.btools.behavior.egglaying.io;

import edu.illinois.gernat.btools.behavior.egglaying.geometry.Point2d;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.Vector2d;

/**
 * Created by tobias on 21.09.16.
 */
public class LabeledBee {
    private final int id;
    private final float x;
    private final float y;
    private final float dx;
    private final float dy;
    private final int label;
    private final float[] corners;
    private Point2d upperLeftROI;
    private double length;
    private Vector2d relLowerEdgeMidPoint;
    private String fname;

    public LabeledBee(int id, float x, float y, float dx, float dy, int label) {
        this(id, x, y, dx, dy, label, null);
    }

    public LabeledBee(int id, float x, float y, float dx, float dy, int label, float[] corners) {
        this(id, x, y, dx, dy, label, corners, null);
    }

    public LabeledBee(int id, float x, float y, float dx, float dy, int label, float[] corners, String fname) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.label = label;
        this.corners = corners;
        this.fname = fname;
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

    public Point2d getPosition() {
        return new Point2d(x, y);
    }

    public Vector2d getOrientation() {
        return new Vector2d(dx, dy);
    }

    public float[] getCorners() {
        return corners;
    }

    public Point2d getUpperLeftROI() {
        return upperLeftROI;
    }

    public void setUpperLeftROI(Point2d upperLeftROI) {
        this.upperLeftROI = upperLeftROI;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }

    public Vector2d getRelLowerEdgeMidPoint() {
        return relLowerEdgeMidPoint;
    }

    public void setRelLowerEdgeMidPoint(Vector2d relLowerEdgeMidPoint) {
        this.relLowerEdgeMidPoint = relLowerEdgeMidPoint;
    }

    public String getFname() {
        return fname;
    }
}
