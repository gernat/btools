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

import edu.illinois.gernat.btools.common.image.Pixel;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
// A free vector in screen coordinate system.
public class Vector 
implements Cloneable
{
	
	public static final Vector I = new Vector(1, 0);

	public static final Vector MINUS_I = new Vector(-1, 0);

	public static final Vector J = new Vector(0, 1);
		
	public static final Vector MINUS_J = new Vector(0, -1);
	
	public float dx;
	
	public float dy;
	
	public Vector(float dx, float dy)
	{
		this.dx = dx;
		this.dy = dy;
	}
	
	public Vector(Coordinate ip, Coordinate tp)
	{
		dx = tp.x - ip.x;
		dy = tp.y - ip.y;
	}
	
	public Vector(Pixel ip, Pixel tp)
	{
		dx = tp.x - ip.x;
		dy = tp.y - ip.y;
	}
	
	public Vector(double theta)
	{
		dx = (float) Math.cos(theta);
		dy = (float) Math.sin(theta);
	}
	
	public Vector set(float dx, float dy)
	{
		this.dx = dx;
		this.dy = dy;
		return this;
	}
	
	public float length()
	{
		return (float) Math.hypot(dx, dy);
	}

	public Vector setLength(float l)
	{
		normalize();
		scale(l);
		return this;
	}
	
	public Vector scale(float s)
	{
		dx *= s;
		dy *= s;
		return this;
	}

	public Vector normalize()
	{
		float length = length();
		dx /= length;
		dy /= length;
		return this;
	}
	
	public Vector rotate(float a)
	{
		float cosA = (float) Math.cos(a);
		float sinA = (float) Math.sin(a);
		float dx = this.dx * cosA - this.dy * sinA;
		float dy = this.dx * sinA + this.dy * cosA;
		this.dx = dx;
		this.dy = dy;
		return this;
	}

	public Vector reverse()
	{
		this.dx *= -1;
		this.dy *= -1;
		return this;
	}
	
	public Vector plus(Vector v)
	{
		return new Vector(dx + v.dx, dy + v.dy);
	}
	
	public float angleBetween(Vector v)
	{
		float d = (float) (Math.atan2(v.dy, v.dx) - Math.atan2(dy, dx));
		if (d < -Math.PI) return d + Angles.TWO_PI;
		else if (d > Math.PI) return d - Angles.TWO_PI;
		else return d;
	}

	public Coordinate terminal(Coordinate ip)
	{
		return new Coordinate(ip.x + dx, ip.y + dy);
	}

	public Coordinate initial(Coordinate tp)
	{
		return new Coordinate(tp.x - dx, tp.y - dy);
	}

	public Vector clone()
	{
		try
		{
			return (Vector) super.clone();
		} 
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		return "[" + dx + "," + dy + "]";
	}
		
}
