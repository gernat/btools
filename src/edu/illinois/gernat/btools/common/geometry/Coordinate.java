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

package edu.illinois.gernat.btools.common.geometry;

import java.awt.Point;

import edu.illinois.gernat.btools.common.image.Pixel;


/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
// A point in screen coordinate system
public class Coordinate
implements Cloneable
{

	public float x;
	
	public float y;

	public Coordinate(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	public Coordinate(double x, double y)
	{
		this.x = (float) x;
		this.y = (float) y;
	}
	
	public Coordinate(Point point)
	{
		x = point.x;
		y = point.y;
	}

	public Coordinate(Pixel pixel)
	{
		x = pixel.x;
		y = pixel.y;
	}

	public Coordinate set(float x, float y)
	{
		this.x = x;
		this.y = y;
		return this;
	}

	public float distanceTo(Coordinate other)
	{
		return (float) Math.hypot(other.x - x, other.y - y);
	}

	public Coordinate translate(float dx, float dy)
	{
		x += dx;
		y += dy;
		return this;
	}

	public Coordinate translate(Vector direction, float distance)
	{
		x += direction.dx * distance;
		y += direction.dy * distance;
		return this;
	}
	
	public Coordinate round()
	{
		x = Math.round(x);
		y = Math.round(y);
		return this;
	}

	public Coordinate calculateMidPoint(Coordinate coordinate)
	{
		float minX = Math.min(x, coordinate.x);
		float maxX = Math.max(x, coordinate.x);
		float minY = Math.min(y, coordinate.y);
		float maxY = Math.max(y, coordinate.y);
		return new Coordinate(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object == null) return false;
		if (object.getClass() != getClass()) return false;
		Coordinate other = (Coordinate) object;
		return (x == other.x) && (y == other.y);
	}

	@Override
	public int hashCode()
	{
		//TODO improve! see ResultPoint.java for inspiration
		return ((Math.round(y) & 0xFFFF) << 16) + (Math.round(x) & 0xFFFF);
	}

	@Override
	public Coordinate clone()
	{
		try
		{
			return (Coordinate) super.clone();
		} 
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}
	
	@Override
	public String toString() 
	{
		return "(" + x + "," + y + ")";
	}

}
