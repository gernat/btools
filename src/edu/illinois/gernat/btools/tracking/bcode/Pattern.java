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

package edu.illinois.gernat.btools.tracking.bcode;

import com.google.zxing.ResultPoint;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Pattern 
{

	protected float x;

	protected float y;

	public float moduleSize;

	private float cx;

	private float cy;

	protected float cModuleSize;

	protected int count;

	public Pattern(float x, float y, float moduleSize) 
	{
		this.x = x;
		this.y = y;
		this.moduleSize = moduleSize;
		cx = x;
		cy = y;
		cModuleSize = moduleSize;
		count = 1;
	}

	public final float getX() 
	{
		return x;
	}

	public final float getY() 
	{
		return y;
	}

	public void updateWith(float moduleSize, float x, float y) 
	{
		cx += x;
		cy += y;
		cModuleSize += moduleSize;
		count++;
		this.x = cx / count;
		this.y = cy / count;		
		this.moduleSize = cModuleSize / count;
	}

	protected boolean isSimilar(float moduleSize, float x, float y) 
	{
		if (Math.abs(y - getY()) < moduleSize && Math.abs(x - getX()) < moduleSize) 
		{
			float moduleSizeDiff = Math.abs(moduleSize - this.moduleSize);
			return moduleSizeDiff < 1 || moduleSizeDiff / this.moduleSize < 1;
		}
		return false;
	}

	public boolean equals(Object other) 
	{
		if (other instanceof Pattern) {
			Pattern otherCenter = (Pattern) other;
			return (x == otherCenter.x) && (y == otherCenter.y);
		}
		return false;
	}

	public int hashCode() 
	{
		return 31 * Float.floatToIntBits(x) + Float.floatToIntBits(y);
	}

	// TODO derive this class from Coordinate and get rid of this method
	public static float distance(Pattern patternCenter1, Pattern patternCenter2) 
	{
		float dx = patternCenter1.x - patternCenter2.x;
		float dy = patternCenter1.y - patternCenter2.y;
		return (float) Math.sqrt((double) (dx * dx + dy * dy));
	}

	public static void orderPatterns(Pattern[] patternCenters) 
	{

		if (patternCenters.length != BCode.PATTERN_COUNT) throw new IllegalArgumentException();
		ResultPoint[] unorderedPoints = new ResultPoint[BCode.PATTERN_COUNT];
		for (int i = 0; i < BCode.PATTERN_COUNT; i++) unorderedPoints[i] = new ResultPoint(patternCenters[i].x, patternCenters[i].y);

		ResultPoint[] orderedPoints = unorderedPoints.clone();
		ResultPoint.orderBestPatterns(orderedPoints);

		int[] order = new int[BCode.PATTERN_COUNT];
		for (int i = 0; i < BCode.PATTERN_COUNT; i++) 
		{
			for (int j = 0; j < BCode.PATTERN_COUNT; j++) 
			{
				if (unorderedPoints[i] == orderedPoints[j]) 
				{
					order[i] = j;
					break;
				}
			}
		}

		Pattern[] originalPatternCenters = patternCenters.clone();
		for (int i = 0; i < BCode.PATTERN_COUNT; i++) patternCenters[order[i]] = originalPatternCenters[i];

	}

}
