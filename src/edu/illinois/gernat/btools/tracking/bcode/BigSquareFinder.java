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
// Re-implementation of com.google.zxing.qrcode.detector.FinderPatternFinder
//
// Major changes:
// - works with bCode pattern geometry instead of QRCode pattern geometry
// - finds all pattern in an image, not just one
// - does not skip over pixel rows to improve likelihood of finding visually 
//   small bCodes
// - uses an index to speed up pattern detection when image is analyzed at 
//   multiple thresholds
// - allows for more variation in module size to increase likelihood of 
//   detecting visually small bCodes
// - does not require that a pattern be detected multiple times
// - updates pattern properties with new estimates, if the same pattern is
//   detected multiple times to improve pattern localization
// - calculates module size from horizontal and vertical estimates to reduce
//   variance of module size error
public class BigSquareFinder 
{

	private static final int INTEGER_MATH_SHIFT = 8;

	private final BitMatrix image;
	
	private final ArrayList<BigSquare> possibleCenters;
	
	private final int[] crossCheckStateCount;
	
	private Index index;
	
	private float lastModuleSizeEstimate; // FIXME dirty hack

	public BigSquareFinder(BitMatrix image, Index index) 
	{
		this.image = image;
		this.possibleCenters = new ArrayList<>();
		this.crossCheckStateCount = new int[5];
		this.index = index;
	}

	public ArrayList<BigSquare> getPossibleCenters() 
	{
		return possibleCenters;
	}

	public void find() 
	{
		int maxI = Math.min(image.getHeight(), index.getBottom());
		int[] stateCount = new int[5];
		for (int i = Math.max(0, index.getTop()); i < maxI; i++) 
		{
			stateCount[0] = 0;
			stateCount[1] = 0;
			stateCount[2] = 0;
			stateCount[3] = 0;
			stateCount[4] = 0;
			int currentState = 0;
			int maxJ = Math.min(image.getWidth(), index.getRight(i) + 1);
			for (int j = Math.max(0, index.getLeft(i) - 1); j < maxJ; j++) 
			{
				if (image.get(j, i)) 
				{
					if ((currentState & 1) == 1) currentState++;
					stateCount[currentState]++;
				}
				else 
				{
					if ((currentState & 1) == 0) 
					{
						if (currentState == 4) 
						{ 
							if (foundPatternCross(stateCount)) 
							{
								boolean confirmed = handlePossibleCenter(stateCount, i, j);
								if (!confirmed) 
								{
									stateCount[0] = stateCount[2];
									stateCount[1] = stateCount[3];
									stateCount[2] = stateCount[4];
									stateCount[3] = 1;
									stateCount[4] = 0;
									currentState = 3;
									continue;
								}
								currentState = 0;
								stateCount[0] = 0;
								stateCount[1] = 0;
								stateCount[2] = 0;
								stateCount[3] = 0;
								stateCount[4] = 0;
							} 
							else 
							{ 
								stateCount[0] = stateCount[2];
								stateCount[1] = stateCount[3];
								stateCount[2] = stateCount[4];
								stateCount[3] = 1;
								stateCount[4] = 0;
								currentState = 3;
							}
						} 
						else stateCount[++currentState]++;
					} 
					else stateCount[currentState]++; 
				}
			} 
			if (foundPatternCross(stateCount)) handlePossibleCenter(stateCount, i, maxJ);
		} 
	}

	private static float centerFromEnd(int[] stateCount, int end) 
	{
		return (float) (end - stateCount[4] - stateCount[3]) - stateCount[2] / 2.0f;
	}

	protected static boolean foundPatternCross(int[] stateCount) 
	{
		int totalModuleSize = 0;
		for (int i = 0; i < 5; i++) 
		{
			int count = stateCount[i];
			if (count == 0) return false;
			totalModuleSize += count;
		}
		if (totalModuleSize < 6) return false;
		int moduleSize = (totalModuleSize << INTEGER_MATH_SHIFT) / BigSquare.MODULE_COUNT;
		int maxVariance = moduleSize / 2;
		return Math.abs(moduleSize - (stateCount[0] << INTEGER_MATH_SHIFT)) < maxVariance && Math.abs(moduleSize - (stateCount[1] << INTEGER_MATH_SHIFT)) < maxVariance && Math.abs(2 * moduleSize - (stateCount[2] << INTEGER_MATH_SHIFT)) < 2 * maxVariance && Math.abs(moduleSize - (stateCount[3] << INTEGER_MATH_SHIFT)) < maxVariance && Math.abs(moduleSize - (stateCount[4] << INTEGER_MATH_SHIFT)) < maxVariance;
	}

	private int[] getCrossCheckStateCount() 
	{
		crossCheckStateCount[0] = 0;
		crossCheckStateCount[1] = 0;
		crossCheckStateCount[2] = 0;
		crossCheckStateCount[3] = 0;
		crossCheckStateCount[4] = 0;
		return crossCheckStateCount;
	}

	private float crossCheckVertical(int startI, int centerJ, int maxCount, int originalStateCountTotal) 
	{
		BitMatrix image = this.image;

		int maxI = image.getHeight();
		int[] stateCount = getCrossCheckStateCount();

		int i = startI;
		while (i >= 0 && image.get(centerJ, i)) 
		{
			stateCount[2]++;
			i--;
		}
		if (i < 0) return Float.NaN;
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
		while (i < maxI && image.get(centerJ, i)) 
		{
			stateCount[2]++;
			i++;
		}
		if (i == maxI) return Float.NaN;
		while (i < maxI && !image.get(centerJ, i) && stateCount[3] < maxCount) 
		{
			stateCount[3]++;
			i++;
		}
		if (i == maxI || stateCount[3] >= maxCount) return Float.NaN;
		while (i < maxI && image.get(centerJ, i) && stateCount[4] < maxCount) 
		{
			stateCount[4]++;
			i++;
		}
		if (stateCount[4] >= maxCount) return Float.NaN;

		int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
		lastModuleSizeEstimate = (float) stateCountTotal / BigSquare.MODULE_COUNT;
		if (2 * Math.abs(stateCountTotal - originalStateCountTotal) > originalStateCountTotal) return Float.NaN;

		return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Float.NaN;
	}

	private float crossCheckHorizontal(int startJ, int centerI, int maxCount, int originalStateCountTotal) 
	{
		BitMatrix image = this.image;

		int maxJ = image.getWidth();
		int[] stateCount = getCrossCheckStateCount();

		int j = startJ;
		while (j >= 0 && image.get(j, centerI)) 
		{
			stateCount[2]++;
			j--;
		}
		if (j < 0) return Float.NaN;
		while (j >= 0 && !image.get(j, centerI) && stateCount[1] <= maxCount) 
		{
			stateCount[1]++;
			j--;
		}
		if (j < 0 || stateCount[1] > maxCount) 
		{
			return Float.NaN;
		}
		while (j >= 0 && image.get(j, centerI) && stateCount[0] <= maxCount) 
		{
			stateCount[0]++;
			j--;
		}
		if (stateCount[0] > maxCount) return Float.NaN;

		j = startJ + 1;
		while (j < maxJ && image.get(j, centerI)) 
		{
			stateCount[2]++;
			j++;
		}
		if (j == maxJ) return Float.NaN;
		while (j < maxJ && !image.get(j, centerI) && stateCount[3] < maxCount) 
		{
			stateCount[3]++;
			j++;
		}
		if (j == maxJ || stateCount[3] >= maxCount) return Float.NaN;
		while (j < maxJ && image.get(j, centerI) && stateCount[4] < maxCount) 
		{
			stateCount[4]++;
			j++;
		}
		if (stateCount[4] >= maxCount) return Float.NaN;

		int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
		lastModuleSizeEstimate = (float) stateCountTotal / BigSquare.MODULE_COUNT;
		if (4 * Math.abs(stateCountTotal - originalStateCountTotal) > originalStateCountTotal) return Float.NaN;

		return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : Float.NaN;
	}

	protected boolean handlePossibleCenter(int[] stateCount, int i, int j) 
	{
		int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
		float centerJ = centerFromEnd(stateCount, j);
		float centerI = crossCheckVertical(i, (int) centerJ, stateCount[2], stateCountTotal);
		if (!Float.isNaN(centerI)) 
		{
			float verticalModuleSizeEstimate = lastModuleSizeEstimate;
			centerJ = crossCheckHorizontal((int) centerJ, (int) centerI, stateCount[2], stateCountTotal);
			if (!Float.isNaN(centerJ)) 
			{
				float horizontalModuleSizeEstimate = lastModuleSizeEstimate;
				float estimatedModuleSize = (horizontalModuleSizeEstimate + verticalModuleSizeEstimate) / 2;
				boolean found = false;
				int max = possibleCenters.size();
				for (int index = 0; index < max; index++) 
				{
					BigSquare center = (BigSquare) possibleCenters.get(index);
					if (center.isSimilar(estimatedModuleSize, centerJ, centerI)) 
					{
						center.updateWith(estimatedModuleSize, centerJ, centerI);
						found = true;
						break;
					}
				}
				if (!found) 
				{
					BigSquare point = new BigSquare(centerJ, centerI, estimatedModuleSize);
					possibleCenters.add(point);
				}
				return true;
			}
		}
		return false;
	}

}
