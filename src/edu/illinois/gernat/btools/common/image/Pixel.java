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

package edu.illinois.gernat.btools.common.image;

import java.awt.Point;

import edu.illinois.gernat.btools.common.geometry.Coordinate;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Pixel 
{

	public short x;
	
	public short y;
	
	public Pixel(short x, short y)
	{
		this.x = x;
		this.y = y;
	}

	public Pixel(int x, int y)
	{
		if ((x > Short.MAX_VALUE) || (x < Short.MIN_VALUE)) throw new IllegalArgumentException();
		if ((y > Short.MAX_VALUE) || (y < Short.MIN_VALUE)) throw new IllegalArgumentException();
		this.x = (short) x;
		this.y = (short) y;
	}

	public Pixel(Coordinate coordinate)
	{
		this(Math.round(coordinate.x), Math.round(coordinate.y));
	}
	
	public Pixel(Point point)
	{
		x = (short) point.x;
		y = (short) point.y;
	}
	
	public Pixel(Pixel other)
	{
		this.x = other.x;
		this.y = other.y;
	}

	public int distanceTo(Pixel pixel)
	{
		return Math.abs(x - pixel.x) + Math.abs(y - pixel.y);
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (object == null) return false;
		if (object.getClass() != getClass()) return false;
		Pixel other = (Pixel) object;
		return (x == other.x) && (y == other.y);
	}

	@Override
	public int hashCode()
	{
		return (y << 16) | x; 
	}
	
	@Override
	public String toString()
	{
		return "(" + x + "," + y + ")"; 
	}
	
}
