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
 * Created by tobias on 28.09.16.
 */
public class Line2d {
    public final Point2d p;
    public final Point2d q;

    public Line2d(Point2d p, Point2d q) {
        this.p = p;
        this.q = q;
    }

    public Line2d(Point2d p, Vector2d v) {
        this.p = p;
        this.q = p.add(v);
    }

    public Vector2d getDirection() {
        return p.to(q);
    }

    public boolean isParallel(Line2d line) {
        Vector2d d1 = this.getDirection();
        Vector2d d2 = line.getDirection();
        return d1.x * d2.y - d1.y * d2.x == 0;
    }

    public Point2d getIntersection(Line2d line) {
        double x1 = this.p.x;
        double y1 = this.p.y;
        Vector2d d1 = this.getDirection();
        double xv1 = d1.x;
        double yv1 = d1.y;
        double x2 = line.p.x;
        double y2 = line.p.y;
        Vector2d d2 = line.getDirection();
        double xv2 = d2.x;
        double yv2 = d2.y;
        double numeratorPart = xv2 * y1 - xv2 * y2 - x1 * yv2 + x2 * yv2;
        double denominator = xv2 * yv1 - xv1 * yv2;
        double r = -numeratorPart / denominator;
        return new Point2d(x1 + r * xv1, y1 + r * yv1);
    }

    public Point2d getMidpoint() {
        return new Point2d((this.p.x + this.q.x) / 2, (this.p.y + this.q.y) / 2);
    }
}
