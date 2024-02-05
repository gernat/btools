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

package edu.illinois.gernat.btools.behavior.egglaying.deploy;

import edu.illinois.gernat.btools.common.io.token.Tokenizable;

public class Detection
implements Tokenizable
{
	
	public long timestamp;
	
	public int id;
	
	public float egglayingProbability;
	
	public float truePositiveProbability;
	
	public Detection(long timestamp, int id, float egglayingProbability, float truePositiveProbability)
	{
		this.timestamp = timestamp;
		this.id = id;
		this.egglayingProbability = egglayingProbability;
		this.truePositiveProbability = truePositiveProbability;		
	}
	
	@Override
	public Object[] toTokens()
	{
		Object[] tokens = new Object[4];
		tokens[0] = timestamp;
		tokens[1] = id;
		tokens[2] = egglayingProbability;
		tokens[3] = truePositiveProbability;
		return tokens;
	}
	
}
