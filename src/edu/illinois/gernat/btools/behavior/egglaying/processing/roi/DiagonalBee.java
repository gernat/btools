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

package edu.illinois.gernat.btools.behavior.egglaying.processing.roi;

import edu.illinois.gernat.btools.behavior.egglaying.geometry.Point2d;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.ROI;
import edu.illinois.gernat.btools.behavior.egglaying.geometry.Vector2d;
import edu.illinois.gernat.btools.behavior.egglaying.io.LabeledBee;

/**
 * Created by tobias on 16.10.16.
 */
public class DiagonalBee implements ROICalculator {

    private final double padding;

    public DiagonalBee(double padding) {
        this.padding = padding;
    }

    /**
     * Calculates the ROI for the small image to train or use the neural network (2. stage).
     *
     * @param bee information about the bee.
     * @return the roi definition
     */
    public ROI calcROI(LabeledBee bee) {
        double angle = bee.getOrientation().signedAngle(new Vector2d(1, 1).normalize());
        Point2d upperLeft = bee.getPosition().scaleAdd(111.0 + this.padding * Math.sqrt(2.0), bee.getOrientation().normalize()).scaleAdd(256.0 + this.padding * 2.0, new Vector2d(-1, 0).rotate(-angle));

        return new ROI(upperLeft, -angle, 256 + this.padding * 2, 256 + this.padding * 2);
    }
}
