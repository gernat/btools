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

import java.util.LinkedList;
import java.util.List;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.PerspectiveTransform;

import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public final class Detector
{

	public static double minTemplateConservation = 0.8;
	
	public static boolean checkMargin = false;
	
	private Detector() 
	{
	}
	
	public static List<BCode> detect(BitMatrix image, Index index) 
	{	    
		LinkedList<BCode> beeIDs = new LinkedList<BCode>();
		List<BigSquare> finderPatterns = Detector.findFinderPatterns(image, index);
	    for (BigSquare finderPattern : finderPatterns)
		{
	    	
	    	//
			List<SmallSquare> alignmentPatterns = Detector.findAlignmentPatters(image, finderPattern);
			
			// if only one alignment pattern can be found, guess the position 
			// of the other pattern
			if (alignmentPatterns.size() == 1)
			{
				//TODO simplify once MyResultPoint is derived from Coordinate
				SmallSquare singleton = alignmentPatterns.get(0);
				Coordinate ap = new Coordinate(finderPattern.getX(), finderPattern.getY());
				Coordinate fp = new Coordinate(singleton.getX(), singleton.getY());
				Vector v1 = new Vector(ap, fp).rotate(BCode.ANGLE_BETWEEN_PATTERNS);
				Coordinate candidate1 = v1.terminal(ap);
				alignmentPatterns.add(new SmallSquare(candidate1.x, candidate1.y, (finderPattern.moduleSize + singleton.moduleSize) / 2));
				Vector v2 = new Vector(ap, fp).rotate(-BCode.ANGLE_BETWEEN_PATTERNS);
				Coordinate candidate2 = v2.terminal(ap);
				alignmentPatterns.add(new SmallSquare(candidate2.x, candidate2.y, (finderPattern.moduleSize + singleton.moduleSize) / 2));
			}
			
			//
			for (int i = 0; i < alignmentPatterns.size(); i++)
			{
				for (int j = i + 1; j < alignmentPatterns.size(); j++)
				{
					
					// check that finder pattern is the top-left pattern 
					Pattern[] pattern = new Pattern[BCode.PATTERN_COUNT];
					pattern[0] = finderPattern;
					pattern[1] = alignmentPatterns.get(i);
					pattern[2] = alignmentPatterns.get(j);
					Pattern.orderPatterns(pattern);
					if (!(pattern[1] instanceof BigSquare)) continue;

					// create bee ID if alignment patterns are not 
					// within or too close to finder pattern
					BCode beeID = BCode.createFrom((BigSquare) pattern[1], (SmallSquare) pattern[0], (SmallSquare) pattern[2]);
					if (Pattern.distance(pattern[0], pattern[1]) <= ((float) BCode.TL_WIDTH / 2 + 1) * beeID.moduleSize) continue;
					if (Pattern.distance(pattern[0], pattern[2]) <= ((float) BCode.TL_WIDTH / 2 + 1) * beeID.moduleSize) continue;
					beeIDs.add(beeID);
					
					//
					PerspectiveTransform transform = PerspectiveTransform.quadrilateralToQuadrilateral(BCode.TL_POSITION.x, BCode.TL_POSITION.y, BCode.TR_POSITION.x, BCode.TR_POSITION.y, BCode.BR_POSITION.x, BCode.BR_POSITION.y, BCode.BL_POSITION.x, BCode.BL_POSITION.y, beeID.finderPattern.getX(), beeID.finderPattern.getY(), beeID.topRight.getX(), beeID.topRight.getY(), beeID.bottomRight.getX(), beeID.bottomRight.getY(), beeID.bottomLeft.getX(), beeID.bottomLeft.getY());					
					try
					{
						beeID.bits = GridSampler.getInstance().sampleGrid(image, BCode.DIMENSION + BCode.MARGIN * 2, BCode.DIMENSION + BCode.MARGIN * 2, transform);
						beeID.templateConservation = computeTemplateConservation(beeID.bits);
						if (beeID.templateConservation >= Detector.minTemplateConservation)
						{
							beeID.hasGoodTemplate = true;
							int[] result = Decoder.decode(beeID.bits);
							beeID.data = result[Decoder.FIELD_ID];
							beeID.isErrorCorrected = result[Decoder.FIELD_ERROR_CORRECTION_FLAG] == 1;
							beeID.isDecoded = true;
						}
					} 
					catch (Exception e)
					{
					}				
					
				}
			}
		}
	 
	    //
	    return beeIDs;	    
	
	}
	  
	private static List<SmallSquare> findAlignmentPatters(BitMatrix image, BigSquare finderPattern) 
	{		
	    float estimatedModuleSize = finderPattern.moduleSize;
	    int xOffset = (int) (finderPattern.getX() - BCode.ALIGNMENT_PATTERN_SEARCH_RADIUS * estimatedModuleSize);
	    int left = Math.max(0, xOffset);
	    xOffset -= left; 
	    int yOffset = (int) (finderPattern.getY() - BCode.ALIGNMENT_PATTERN_SEARCH_RADIUS * estimatedModuleSize);
	    int top = Math.max(0, yOffset);
	    yOffset -= top;
	    int width = Math.min(image.getWidth() - left, (int) (BCode.ALIGNMENT_PATTERN_SEARCH_RADIUS * estimatedModuleSize * 2) + xOffset);
	    int height = Math.min(image.getHeight() - top, (int) (BCode.ALIGNMENT_PATTERN_SEARCH_RADIUS * estimatedModuleSize * 2) + yOffset);
	    SmallSquareFinder alignmentFinder = new SmallSquareFinder(image, left, top, width, height, estimatedModuleSize);
	    alignmentFinder.find(); 
	    return (List<SmallSquare>) alignmentFinder.getPossibleCenters();
	}

	private static List<BigSquare> findFinderPatterns(BitMatrix image, Index index) 
	{
		BigSquareFinder finderFinder = new BigSquareFinder(image, index);
	    finderFinder.find();
	    return (List<BigSquare>) finderFinder.getPossibleCenters();
	}

	private static double computeTemplateConservation(BitMatrix matrix)
	{
		
		// create a template to compare the current barcode to 
		BitMatrix template = BCode.createTemplate();		

		// count how many black modules in the center are the same as in the template
		int blackMatches = 0;
		for (int i = 0; i < BCode.TEMPLATE_XY_BLACK.length; i++) 
		{
			int x = BCode.TEMPLATE_XY_BLACK[i][0];
			int y = BCode.TEMPLATE_XY_BLACK[i][1];
			if (matrix.get(x, y) == template.get(x, y)) blackMatches++; 
		}

		// count how many white modules in the center are the same as in the template
		int whiteMatches = 0;
		for (int i = 0; i < BCode.TEMPLATE_XY_WHITE.length; i++) 
		{
			int x = BCode.TEMPLATE_XY_WHITE[i][0];
			int y = BCode.TEMPLATE_XY_WHITE[i][1];
			if (matrix.get(x, y) == template.get(x, y)) whiteMatches++; 
		}

		// if the margin does not need to be checked, return conservation 
		// score for center only
		if (!checkMargin) return ((double) blackMatches / BCode.TEMPLATE_XY_BLACK.length + (double) whiteMatches / BCode.TEMPLATE_XY_WHITE.length) / 2;
		
		// count how many modules in the margin are the same as in the template
		int marginMatches = 0;
		for (int i = 0; i < BCode.TEMPLATE_XY_MARGIN.length; i++) 
		{
			int x = BCode.TEMPLATE_XY_MARGIN[i][0];
			int y = BCode.TEMPLATE_XY_MARGIN[i][1];
			if (matrix.get(x, y) == template.get(x, y)) marginMatches++; 
		}

		// return conservation score for whole barcode
		return ((double) blackMatches / BCode.TEMPLATE_XY_BLACK.length + (double) whiteMatches / BCode.TEMPLATE_XY_WHITE.length + marginMatches / BCode.TEMPLATE_XY_MARGIN.length) / 3;

	}
	
}
