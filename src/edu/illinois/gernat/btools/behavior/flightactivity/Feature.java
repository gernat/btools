/*
 * Copyright (C) 2023 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.behavior.flightactivity;

import edu.illinois.gernat.btools.common.io.record.Record;

public class Feature
{

	public long timestamp;
	
	public int beeID;
	
	public int x;
	
	public int y;
	
	public float angle;

	public int deltaT;
	
	public int deltaX;
	
	public int deltaY;
	
	public float deltaAngle;
	

	public Feature(Record current, Record previous)
	{
		
		// properties of current detection
		timestamp = current.timestamp;
		beeID = current.id;
		x = Math.round(current.center.x);
		y = Math.round(current.center.y);
		angle = (float) Math.atan2(current.orientation.dy, current.orientation.dx);
		
		// differences to previous detection
		if (previous != null)
		{
			deltaT = (int) (timestamp - previous.timestamp);
			deltaX = x - Math.round(previous.center.x);
			deltaY = y - Math.round(previous.center.y);
			deltaAngle = previous.orientation.angleBetween(current.orientation);
		}
		else
		{
			deltaT = -1;
			deltaX = 0;
			deltaY = 0;
			deltaAngle = 0;
		}
		
	}
	
}
