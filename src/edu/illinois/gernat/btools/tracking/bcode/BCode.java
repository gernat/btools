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

import com.google.zxing.common.BitMatrix;

import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class BCode
{

	public static final int[][] TEMPLATE_XY_MARGIN = 
	{
		{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0}, {8, 0}, {9, 0}, {10, 0}, {11, 0}, 
		{0, 1}, {11, 1},  
		{0, 2}, {11, 2}, 
		{0, 3}, {11, 3}, 
		{0, 4}, {11, 4}, 
		{0, 5}, {11, 5}, 
		{0, 6}, {11, 6}, 
		{0, 7}, {11, 7}, 
		{0, 8}, {11, 8}, 
		{0, 9}, {11, 9}, 
		{0, 10}, {11, 10}, 
		{0, 11}, {1, 11}, {2, 11}, {3, 11}, {4, 11}, {5, 11}, {6, 11}, {7, 11}, {8, 11}, {9, 11}, {10, 11}, {11, 11}
	};

	public static final int[][] TEMPLATE_XY_BLACK = 
	{
		{1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {8, 1}, {9, 1}, {10, 1},  
		{1, 2}, {6, 2}, {8, 2}, {10, 2}, 
		{1, 3}, {3, 3}, {4, 3}, {6, 3}, {8, 3}, {9, 3}, {10, 3}, 
		{1, 4}, {3, 4}, {4, 4}, {6, 4},  
		{1, 5}, {6, 5},
		{1, 6}, {2, 6}, {3, 6}, {4, 6}, {5, 6}, {6, 6}, 
		{1, 8}, {2, 8}, {3, 8}, 
		{1, 9}, {3, 9},  
		{1, 10}, {2, 10}, {3, 10}
	};

	public static final int[][] TEMPLATE_XY_WHITE = 
	{
		{7, 1},   
		{2, 2}, {3, 2}, {4, 2}, {5, 2}, {7, 2}, {9, 2},  
		{2, 3}, {5, 3}, {7, 3},  
		{2, 4}, {5, 4}, {7, 4}, {8, 4}, {9, 4}, {10, 4}, 
		{2, 5}, {3, 5}, {4, 5}, {5, 5}, {7, 5}, 
		{7, 6}, 
		{1, 7}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {6, 7}, {7, 7}, 
		{4, 8}, 
		{2, 9}, {4, 9}, 
		{4, 10}
	};
	
	public static final int[][] DATA_XY = 
	{
		{8, 7}, {7, 8}, {8, 8}, 
		{8, 5}, {9, 5}, {10, 5}, {8, 6}, {9, 6}, {10, 6}, {9, 7}, {10, 7}, 
		{9, 8}, {10, 8}, {8, 9}, {9, 9}, {10, 9}, {8, 10}, {9, 10}, {10, 10}, 
		{5, 8}, {6, 8}, {5, 9}, {6, 9}, {7, 9}, {5, 10}, {6, 10}, {7, 10}
	}; 

	public static final int[][] DATA_BYTE_1 = 
	{
		{8, 7}, {7, 8}, {8, 8}, 
	}; 

	public static final int[][] DATA_BYTE_2 = 
	{
		{8, 5}, {9, 5}, {10, 5}, {8, 6}, {9, 6}, {10, 6}, {9, 7}, {10, 7}, 
	}; 
	
	public static final int[][] DATA_BYTE_3 = 
	{
		{9, 8}, {10, 8}, {8, 9}, {9, 9}, {10, 9}, {8, 10}, {9, 10}, {10, 10}, 
	}; 
	
	public static final int[][] DATA_BYTE_4 = 
	{
		{5, 8}, {6, 8}, {5, 9}, {6, 9}, {7, 9}, {5, 10}, {6, 10}, {7, 10}
	}; 
	
	public static final int DIMENSION = 10;

	public static final int TL_WIDTH = 6;
	
	public static final int DATA_BYTES = 2;
	
	public static final int MASKED_BIT_COUNT = 5;
	
	public static final int ERROR_CORRECTION_BYTES = 2;
	
	public static final int PATTERN_COUNT = 3;
	
	public static final int MARGIN = 1;
	
	public static final int BLOCK_SIZE = 8;

	public static final Coordinate TL_POSITION = new Coordinate(4, 4);
	
	public static final Coordinate TR_POSITION = new Coordinate(9.5f, 2.5f);
	
	public static final Coordinate BL_POSITION = new Coordinate(2.5f, 9.5f);
	
	public static final Coordinate BR_POSITION = new Coordinate(8, 8);
	
	public static final Coordinate CENTER_POSITION = new Coordinate(6, 6);

	public static final float ANGLE_BETWEEN_PATTERNS = new Vector(TL_POSITION, BL_POSITION).angleBetween(new Vector(TL_POSITION, TR_POSITION));

	public static final float DISTANCE_TL_TR = TL_POSITION.distanceTo(TR_POSITION);
	
	public static final float DISTANCE_TL_BL = TL_POSITION.distanceTo(BL_POSITION);
	
	public static final float DISTANCE_TL_BR = TL_POSITION.distanceTo(BR_POSITION);

	public static final float DISTANCE_TR_BL = TR_POSITION.distanceTo(BL_POSITION);

	public static final float DISTANCE_CENTER_CORNER = CENTER_POSITION.distanceTo(new Coordinate(0, 0));

	public static final int ALIGNMENT_PATTERN_SEARCH_RADIUS = 8;   

	public static final int UNIQUE_ID_COUNT = 2048;   
	
	public BigSquare finderPattern;
	
	public SmallSquare bottomLeft;
	
	public SmallSquare topRight;
	
	public Pattern bottomRight; 
	
	public Pattern center;
	
	public float moduleSize; //TODO this variable is never actually being used. remove and replace its use with a better calculation in metaID.java for only use in consolidator (as in zxing's Detector.java)
	
	public BitMatrix bits;
	
	public int data;
	
	public boolean isDecoded;
	
	public double templateConservation;
	
	public boolean hasGoodTemplate;
	
	public boolean isErrorCorrected;
	
	private BCode()
	{
	}
	
	public static BitMatrix createTemplate() 
	{
		BitMatrix template = new BitMatrix(BCode.DIMENSION + BCode.MARGIN * 2);
		for (int x = 1; x < 7; x++) template.set(x, 1);
		for (int y = 2; y < 7; y++) template.set(6, y);
		for (int x = 5; x > 0; x--) template.set(x, 6);
		for (int y = 5; y > 1; y--) template.set(1, y);
		template.setRegion(3, 3, 2, 2);
		template.setRegion(8, 1, 3, 3);
		template.flip(9, 2);
		template.setRegion(1, 8, 3, 3);
		template.flip(2, 9);
		return(template);
	}
	
	public static BCode createFrom(BigSquare finderPattern, SmallSquare bottomLeft, SmallSquare topRight)
	{
		
		// 
		if (finderPattern == null) return null;
		BCode beeID = new BCode();
		beeID.data = -1;
		beeID.isDecoded = false;
		beeID.isErrorCorrected = false;
		
		//
		beeID.finderPattern = finderPattern;
		beeID.bottomLeft = bottomLeft;
		beeID.topRight = topRight;
		
		// don't create an ID if its module size would be smaller than 1
		beeID.moduleSize = (BigSquare.MODULE_COUNT * finderPattern.moduleSize + SmallSquare.MODULE_COUNT * bottomLeft.moduleSize + SmallSquare.MODULE_COUNT * topRight.moduleSize) / (BigSquare.MODULE_COUNT + SmallSquare.MODULE_COUNT + SmallSquare.MODULE_COUNT);  
		if (beeID.moduleSize < 1.0f) return null; 

		//
		float x = beeID.finderPattern.getX() + (beeID.topRight.getX() - beeID.finderPattern.getX()) + (beeID.bottomLeft.getX() - beeID.finderPattern.getX());
		float y = beeID.finderPattern.getY() + (beeID.topRight.getY() - beeID.finderPattern.getY()) + (beeID.bottomLeft.getY() - beeID.finderPattern.getY());
		beeID.bottomRight = new Pattern(x, y, beeID.moduleSize);

		x = beeID.bottomRight.getX() + (finderPattern.getX() - beeID.bottomRight.getX()) / 2;
		y = beeID.bottomRight.getY() + (finderPattern.getY() - beeID.bottomRight.getY()) / 2;
		beeID.center = new Pattern(x, y, -1);

		//
		return beeID;
		
	}

	@Override
	public String toString() 
	{
		return "<" + center.getX() + ", " + center.getY() + ", " + data + ">";
	}

}
