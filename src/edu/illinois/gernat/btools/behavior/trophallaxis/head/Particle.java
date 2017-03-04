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

import java.util.Arrays;

import edu.illinois.gernat.btools.common.geometry.Angles;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Particle
implements Cloneable
{
	
	public static final int INDEX_LABEL_X_OFFSET = 0;

	public static final int INDEX_LABEL_Y_OFFSET = 1;
		
	public static final int INDEX_GF_OFFSET = 2;

	public static final int INDEX_CE_OFFSET = 3;
	
	public static final int INDEX_EF_OFFSET = 4;
	
	public static final int INDEX_AK_OFFSET = 5;
	
	public static final int INDEX_MI_OFFSET = 6;
		
	public static final int INDEX_ROTATION_OFFSET = 7;
	
	public static final int INDEX_EYE_SKEW_OFFSET = 8;

	public static final int INDEX_HM_OFFSET = 9;

	public static final int COMPONENT_COUNT = 10;
												 			   
	public static final double[] minimumOffset = {-20, -20, -5,  -5,   0, 0,  0, Angles.MINUS_PI * 30 / 180, -0.1, -5};
														 
	public static final double[] referenceValues = {0,   0, 15, 35,   5, 0,  10, 0, 							0.5, 	0};
												  		   
	public static final double[] maximumOffset = { 20,  20,  5,  5,   5, 5,  0, Angles.PI * 30 / 180, 		0, 		0};
	
	public double[] position;
	
	public double[] velocity;
	
	public double areaScore;
	
	public double borderScore;
	
	public double angleScore;
	
	public double score;
		
	public double[] bestPosition;

	public double bestScore;
	
	public Particle()
	{
		position = new double[COMPONENT_COUNT];
		velocity = new double[COMPONENT_COUNT];
		bestPosition = new double[COMPONENT_COUNT];
	}

	@Override
	public String toString()
	{
		return Arrays.toString(position);
	}

	@Override
	public Particle clone()
	{
		try
		{
			Particle clone = (Particle) super.clone();
			clone.position = position.clone();
			clone.velocity = velocity.clone();
			clone.bestPosition = bestPosition.clone();
			return clone;
		} 
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}
	
}
