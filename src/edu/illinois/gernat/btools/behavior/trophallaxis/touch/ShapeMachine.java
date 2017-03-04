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

import java.util.Iterator;
import java.util.List;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class ShapeMachine
{

	private static final int STATE_HEAD_1 = 0;

	private static final int STATE_SEGMENT = 1;
	
	private static final int STATE_THIN_SEGMENT = 2;

	private static final int STATE_HEAD_2 = 3;
	
	private static final int STATE_IS_NO_TOUCH = 4;
	
	private static int state;

	private static int maxThickSegmentLength;

	private static int maxPathThickness;

	private static int maxThinSegmentThickness;

	private static int thickSegmentLength;

	private static int longestThinSegmentLength;

	private static int thinSegmentLength;

	private static void stateHead1(int pixelThickness)
	{
		if (pixelThickness > maxPathThickness) 
		{
			thickSegmentLength++;
			if (thickSegmentLength > maxThickSegmentLength) switchToNoTouchState();
		}
		else if (pixelThickness <= maxThinSegmentThickness) switchToThinSegmentState();
		else switchToSegmentState(); 
	}

	private static void stateSegment(int pixelThickness)
	{
		if (pixelThickness > maxPathThickness) switchToHead2State();
		else if (pixelThickness <= maxThinSegmentThickness) switchToThinSegmentState(); 
	}

	private static void stateThinSegment(int pixelThickness)
	{
		if (pixelThickness > maxPathThickness) switchToHead2State(); 
		else if (pixelThickness > maxThinSegmentThickness) switchToSegmentState();
		else thinSegmentLength++;
	}

	private static void stateHead2(int pixelThickness)
	{
		if (pixelThickness > maxPathThickness) 
		{
			thickSegmentLength++;
			if (thickSegmentLength > maxThickSegmentLength) switchToNoTouchState();
		}
		else switchToNoTouchState();
	}

	private static void switchToHead1State()
	{
		thickSegmentLength = 0;
		state = STATE_HEAD_1;
	}

	private static void switchToThinSegmentState()
	{
		if (thinSegmentLength > longestThinSegmentLength) longestThinSegmentLength = thinSegmentLength;
		thinSegmentLength = 1;
		state = STATE_THIN_SEGMENT;
	}

	private static void switchToSegmentState()
	{
		state = STATE_SEGMENT;
	}
	
	private static void switchToHead2State()
	{
		thickSegmentLength = 1;
		state = STATE_HEAD_2;
	}

	private static void switchToNoTouchState()
	{
		state = STATE_IS_NO_TOUCH;
	}

	public static int getLongestThinSegmentLength()
	{
		return Math.max(thinSegmentLength, longestThinSegmentLength);
	}

	public static boolean isTouchShaped(List<Integer> pathThickness, int maxThickSegmentLength, int maxPathThickness, int maxThinSegmentThickness, int minThinSegmentLength)
	{
		
		// initialize some state machine parameters
		ShapeMachine.maxThickSegmentLength = maxThickSegmentLength;
		ShapeMachine.maxPathThickness = maxPathThickness;
		ShapeMachine.maxThinSegmentThickness = maxThinSegmentThickness;
		
		// initialize state machine 
		thinSegmentLength = 0;
		longestThinSegmentLength = 0;
		switchToHead1State();
		
		// process all pixel thicknesses
		Iterator<Integer> thicknessIterator = pathThickness.iterator();
		while (thicknessIterator.hasNext())
		{
			
			// change state according to current thickness 
			int pixelThickness = thicknessIterator.next();
			if (state == STATE_HEAD_1) stateHead1(pixelThickness);
			else if (state == STATE_SEGMENT) stateSegment(pixelThickness);
			else if (state == STATE_THIN_SEGMENT) stateThinSegment(pixelThickness);
			else stateHead2(pixelThickness);
			
			// stop if the shape of this path does not correspond to the 
			// expected pattern
			if (state == STATE_IS_NO_TOUCH) break;

		}

		// return whether the shape of this path does not correspond to the 
		// expected pattern
		if (state == STATE_IS_NO_TOUCH) return false;
		else return getLongestThinSegmentLength() >= minThinSegmentLength;
		
	}
	
}
