/*
 * Copyright (C) 2017 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.behavior.trophallaxis.head;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Head
{

	public Area area;
	
	public Coordinate center;
	
	public Vector orientation;
	
	public Path2D eyesAndFrontBoundary;
	
	public int width;
	
	public int thickness;
	
	public double eyeWidth;
	
	public byte[] index;
	
	public int indexXOffset;
	
	public int indexYOffset;
	
	public int indexW;
	
	public int indexH;

	public BufferedImage debugImage;

	public Gradient gradient;
	
	public Head(Area bounds, Coordinate center, Vector orientation, Path2D eyesAndFrontBoundary, int width, int thickness, double eyeWidth, byte[] index, int indexXOffset, int indexYOffset, int indexW, int indexH, Gradient gradient, BufferedImage debugImage)
	{
		this.area = bounds;
		this.center = center;
		this.orientation = orientation;
		this.eyesAndFrontBoundary = eyesAndFrontBoundary;
		this.width = width;
		this.thickness = thickness;
		this.eyeWidth = eyeWidth;
		this.index = index;
		this.indexXOffset = indexXOffset;
		this.indexYOffset = indexYOffset;
		this.indexW = indexW;
		this.indexH = indexH;
		this.gradient = gradient;
		this.debugImage = debugImage;
	}

	public void translate(int x, int y)
	{
		AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
		area = area.createTransformedArea(transform);
		eyesAndFrontBoundary.transform(transform);
		center.translate(x, y);
		indexXOffset += x;
		indexYOffset += y;
	}

}
