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
 *
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.illinois.gernat.btools.tracking.bcode;

import java.util.ArrayList;

import com.google.zxing.common.BitMatrix;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 * @author Sean Owen
 */
// Re-implementation of com.google.zxing.qrcode.detector.AlingmentPatternFinder
//
// Major changes:
// - works with bCode pattern geometry instead of QRCode pattern geometry
// - allows for more variation in module size to increase likelihood of 
//   detecting visually small bCodes
// - updates pattern properties with new estimates, if the same pattern is
//   detected multiple times to improve pattern localization
// - calculates module size from horizontal and vertical estimates to reduce
//   variance of module size error
public final class SmallSquareFinder 
{

	private final BitMatrix image;

	private final ArrayList<SmallSquare> possibleCenters;
	
	private final int startX;
	
	private final int startY;
	
	private final int width;
	
	private final int height;
	
	private final float moduleSize;
	
	private final int[] crossCheckStateCount;

	public SmallSquareFinder(BitMatrix image, int startX, int startY, int width, int height, float moduleSize) 
	{
		this.image = image;
		this.possibleCenters = new ArrayList<>();
		this.startX = startX;
		this.startY = startY;
		this.width = width;
		this.height = height;
		this.moduleSize = moduleSize;
		this.crossCheckStateCount = new int[3];
	}

	public void find() 
	{
		int startX = this.startX;
		int height = this.height;
		int maxJ = startX + width;
		int middleI = startY + (height >> 1);
		int[] stateCount = new int[3];
		for (int iGen = 0; iGen < height; iGen++) 
		{

			int i = middleI + ((iGen & 0x01) == 0 ? (iGen + 1) >> 1 : -((iGen + 1) >> 1));
			stateCount[0] = 0;
			stateCount[1] = 0;
			stateCount[2] = 0;
			int j = startX;
			while (j < maxJ && image.get(j, i)) j++;
			int currentState = 0;
			while (j < maxJ) 
			{
				if (!image.get(j, i)) 
				{
					if (currentState == 1) stateCount[currentState]++;
					else 
					{
						if (currentState == 2) 
						{ 
							if (foundPatternCross(stateCount)) handlePossibleCenter(stateCount, i, j);
							stateCount[0] = stateCount[2];
							stateCount[1] = 1;
							stateCount[2] = 0;
							currentState = 1;
						} 
						else stateCount[++currentState]++;
					}
				} 
				else 
				{ 
					if (currentState == 1) currentState++;
					stateCount[currentState]++;
				}
				j++;
			}
			if (foundPatternCross(stateCount)) handlePossibleCenter(stateCount, i, maxJ);

		}

	}

	public ArrayList<SmallSquare> getPossibleCenters() 
	{
		return possibleCenters;
	}

	private static float centerFromEnd(int[] stateCount, int end) 
	{
		return (float) (end - stateCount[2]) - stateCount[1] / 2.0f;
	}

	private boolean foundPatternCross(int[] stateCount) 
	{
		float moduleSize = this.moduleSize;
		float maxVariance = moduleSize / 2.0f;
		for (int i = 0; i < 3; i++) if (Math.abs(moduleSize - stateCount[i]) >= maxVariance) return false;
		return true;
	}

	private float crossCheckVertical(int startI, int centerJ, int maxCount, int originalStateCountTotal) 
	{
		BitMatrix image = this.image;

		int maxI = image.getHeight();
		int[] stateCount = crossCheckStateCount;
		stateCount[0] = 0;
		stateCount[1] = 0;
		stateCount[2] = 0;

		int i = startI;
		while (i >= 0 && !image.get(centerJ, i) && stateCount[1] <= maxCount) 
		{
			stateCount[1]++;
			i--;
		}
		if (i < 0 || stateCount[1] > maxCount) return Float.NaN;
		while (i >= 0 && image.get(centerJ, i) && stateCount[0] <= maxCount) 
		{
			stateCount[0]++;
			i--;
		}
		if (stateCount[0] > maxCount) return Float.NaN;

		i = startI + 1;
		while (i < maxI && !image.get(centerJ, i) && stateCount[1] <= maxCount) 
		{
			stateCount[1]++;
			i++;
		}
		if (i == maxI || stateCount[1] > maxCount) return Float.NaN;
		while (i < maxI && image.get(centerJ, i) && stateCount[2] <= maxCount) 
		{
			stateCount[2]++;
			i++;
		}
		if (stateCount[2] > maxCount) return Float.NaN;

		int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2];
		if (2 * Math.abs(stateCountTotal - originalStateCountTotal) > originalStateCountTotal) return Float.NaN;

		return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Float.NaN;
	}

	private void handlePossibleCenter(int[] stateCount, int i, int j) 
	{
		int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2];
		float centerJ = centerFromEnd(stateCount, j);
		float centerI = crossCheckVertical(i, (int) centerJ, 2 * stateCount[1], stateCountTotal);
		if (!Float.isNaN(centerI)) 
		{
			float verticalEstimatedModuleSize = (float) (crossCheckStateCount[0] + crossCheckStateCount[1] + crossCheckStateCount[2]) / SmallSquare.MODULE_COUNT;
			float horizontalEstimatedModuleSize = (float) stateCountTotal / SmallSquare.MODULE_COUNT;
			float estimatedModuleSize = (horizontalEstimatedModuleSize + verticalEstimatedModuleSize) / 2;
			int max = possibleCenters.size();
			for (int index = 0; index < max; index++) 
			{
				SmallSquare center = (SmallSquare) possibleCenters.get(index);
				if (center.isSimilar(estimatedModuleSize, centerJ, centerI)) 
				{
					center.updateWith(estimatedModuleSize, centerJ, centerI);
					return;
				}
			}
			SmallSquare point = new SmallSquare(centerJ, centerI, estimatedModuleSize);
			possibleCenters.add(point);
		}
	}

}
