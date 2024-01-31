/*
 * Copyright (C) 2024 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.behavior.trophallaxis;

import edu.illinois.gernat.btools.common.io.token.Tokenizable;

public class Detection
implements Tokenizable
{
	
	public long timestamp;
	
	public int id1;
	
	public int id2;
	
	public float trophallaxisProbability;
	
	public float id1IsDonorProbability;
	
	public Detection(long timestamp, int id1, int id2, float trophallaxisProbability, float id1IsDonorProbability)
	{
		this.timestamp = timestamp;
		this.id1 = id1;
		this.id2 = id2;
		this.trophallaxisProbability = trophallaxisProbability;
		this.id1IsDonorProbability = id1IsDonorProbability;		
	}
	
	@Override
	public Object[] toTokens()
	{
		Object[] tokens = new Object[5];
		tokens[0] = timestamp;
		tokens[1] = id1;
		tokens[2] = id2;
		tokens[3] = trophallaxisProbability;
		tokens[4] = id1IsDonorProbability;
		return tokens;
	}
	
}
