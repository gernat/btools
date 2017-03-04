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

package edu.illinois.gernat.btools.behavior.trophallaxis.touch;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import edu.illinois.gernat.btools.behavior.trophallaxis.head.Head;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Touch
{

	public long timestamp;
	
	public int id1; 
	
	public int id2;
	
	public boolean isTouching;
	
	public BufferedImage debugImage;

	public Head head1;

	public Head head2;

	public int pathLength;

	public int longestThinSegmentLength;
	
	public ArrayList<Integer> pathThickness; 

	public Touch(long timestamp, int id1, int id2, Head head1, Head head2, boolean interacting, int pathLength, int longestThinSegmentLength, ArrayList<Integer> pathThickness, BufferedImage debugImage)
	{
		this.timestamp = timestamp;
		this.id1 = id1;
		this.id2 = id2;
		this.head1 = head1;
		this.head2 = head2;
		this.isTouching = interacting;
		this.pathLength = pathLength;
		this.longestThinSegmentLength = longestThinSegmentLength;
		this.pathThickness = pathThickness;
		this.debugImage = debugImage;
	}
	
}
