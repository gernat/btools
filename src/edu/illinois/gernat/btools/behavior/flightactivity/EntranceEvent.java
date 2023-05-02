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

import edu.illinois.gernat.btools.common.io.token.Tokenizable;

public class EntranceEvent implements Tokenizable
{
	
	public static final int FIELD_BEGIN = 0;
	
	public static final int FIELD_END = 1;

	public static final int FIELD_BEE_ID = 2;

	public static final int FIELD_TYPE = 3;
	
	public int beeID;
	
	public String type;
	
	public int verticalDisplacement;
	
	public int horizontalDisplacement;
	
	public float distanceTraveled;
	
	public int firstY;
	
	public int lastY;
	
	public float rotation;
	
	public long begin;
	
	public int duration;
	
	public EntranceEvent(Feature feature)
	{
		beeID = feature.beeID;
		verticalDisplacement = 0;
		horizontalDisplacement = 0;
		distanceTraveled = 0;
		firstY = feature.y;
		lastY = feature.y;
		rotation = 0;
		begin = feature.timestamp;
		duration = 0; 
	}
	
	public Object getElementByName(String name)
	{
		if (name.equals("vertical_displacement")) return verticalDisplacement;
		else if (name.equals("horizontal_displacement")) return horizontalDisplacement;
		else if (name.equals("distance_traveled")) return distanceTraveled;
		else if (name.equals("first_y")) return firstY;
		else if (name.equals("last_y")) return lastY;
		else if (name.equals("rotation")) return rotation;
		else if (name.equals("duration")) return duration;
		else return null;
	}

	@Override
	public Object[] toTokens()
	{
		Object[] tokens = new Object[4];		
		tokens[FIELD_BEGIN] = begin;
		tokens[FIELD_END] = begin + duration;
		tokens[FIELD_BEE_ID] = beeID;
		tokens[FIELD_TYPE] = type;
		return tokens;
	}

}
