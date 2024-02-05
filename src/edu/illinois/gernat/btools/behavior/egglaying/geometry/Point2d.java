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

package edu.illinois.gernat.btools.behavior.egglaying.geometry;

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
