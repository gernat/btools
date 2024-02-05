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
 * Created by tobias on 27.09.16.
 */
public class ROI {
    public final Point2d upperLeft;
    public final double rotAngle;
    public final double width;
    public final double height;

    public ROI(Point2d upperLeft, double rotAngle, double width, double height) {
        this.upperLeft = upperLeft;
        this.rotAngle = rotAngle;
        this.width = width;
        this.height = height;
    }
}
