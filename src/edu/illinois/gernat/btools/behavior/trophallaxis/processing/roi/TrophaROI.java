/*
 * Copyright (C) 2017, 2024 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.behavior.trophallaxis.processing.roi;

import edu.illinois.gernat.btools.behavior.trophallaxis.io.LabeledBee;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.parameters.Tuple;


/**
 * Created by tobias on 23.01.17.
 */
public class TrophaROI implements PairROI {
    private static final Vector BASE = new Vector(0, -1);

    // depend on the behavior of the bee's in throphallaxis
    private static final double MAX_ROTATION_DEGREE = 6.0;
    private static final double MAX_TRANSLATION = 10.0;
    private final int width;
    private final int height;

    public TrophaROI(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public CenterROI calcROI(Tuple<LabeledBee, LabeledBee> p) {
        LabeledBee beeA = p.a;
        LabeledBee beeB = p.b;

        float[] points1 = beeA.getCorners();
        float[] points2 = beeB.getCorners();
        Coordinate a = new Coordinate(points1[0], -points1[1]).calculateMidPoint(new Coordinate(points1[6], -points1[7]));
        Vector aVec = new Vector(beeA.getDx(), -beeA.getDy());

        Coordinate b = new Coordinate(points2[0], -points2[1]).calculateMidPoint(new Coordinate(points2[6], -points2[7]));
        Vector bVec = new Vector(beeB.getDx(), -beeB.getDy());

        Vector base = new Vector(a, b);
        double rotA = corRot(aVec.angleBetween(base));
        double rotB = corRot(base.angleBetween(bVec));
        double adjust = sameSide(base, aVec, bVec) ? corTrans(shortestAngleBetween(base, aVec), shortestAngleBetween(base, bVec)) : 0;
        Coordinate aRot = new Vector(b, a).rotate((float) rotA).terminal(b);
        Coordinate bRot = new Vector(a, b).rotate((float) rotB).terminal(a);
        Coordinate center = aRot.calculateMidPoint(bRot);
        Vector vec1 = new Vector(aRot, bRot);
        float angle = vec1.angleBetween(BASE);
        return new CenterROI(new Coordinate(center.x, -center.y), angle, width, adjust, height);
    }

    private static final double MAX_ROTATION = MAX_ROTATION_DEGREE / 180.0 * Math.PI;

    public static double corRot(double angle) {
        return MAX_ROTATION * Math.sin(angle);
    }

    private static final double HALF_TRANS = MAX_TRANSLATION / 2.0;

    public static double corTrans(double angleA, double angleB) {
        return HALF_TRANS * (Math.sin(angleA) + Math.sin(angleB));
    }

    private static final Vector INIT_VECTOR = new Vector(1, 0);

    public static boolean sameSide(Vector base, Vector a, Vector b) {
        float angle = base.angleBetween(INIT_VECTOR);
        Vector aR = a.clone().rotate(angle);
        Vector bR = b.clone().rotate(angle);
        return Math.signum(aR.dy) == Math.signum(bR.dy);
    }

    public float shortestAngleBetween(Vector v1, Vector v2) {
        return (float) Math.acos((v1.dx * v2.dx + v1.dy * v2.dy) / (v1.length() * v2.length()));
    }
}
