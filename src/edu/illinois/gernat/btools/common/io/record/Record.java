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

package edu.illinois.gernat.btools.common.io.record;

import edu.illinois.gernat.btools.common.geometry.Angles;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Record 
{
	
	private static final float BCODE_BIG_SQUARE_SMALL_SQUARE_DISTANCE = 5.700877f; //FIXME this duplicates information in BCode class
	
	public static final int TIMESTAMP  = 0; 
	
	public static final int TL_PATTERN_X  = 1; 
	
	public static final int TL_PATTERN_Y  = 2; 
	
	public static final int TR_PATTERN_X  = 3; 
	
	public static final int TR_PATTERN_Y  = 4; 
	
	public static final int BL_PATTERN_X  = 5; 
	
	public static final int BL_PATTERN_Y  = 6; 
	
	public static final int ID  = 7; 
	
	public static final int SUPPORT = 8;

//	public static final int ERROR_CORRECTION_COUNT = 9;

	public static final int FIELD_COUNT = 9; 
	
	public long timestamp;
	
	public int id;
	
	public int support;
	
	public int errorCorrectionCount;
	
	public Coordinate tlPattern;
	
	public Coordinate trPattern;
	
	public Coordinate blPattern;	
	
	public Coordinate center;
	
	public Vector orientation;
	
	public float modulesizeX;
	
	public float modulesizeY;
	
	public float modulesize;

	public Record(int id, Coordinate nw, Coordinate ne, Coordinate sw, int support, int errorCorrectionCount)
	{
		this(-1, id, nw, ne, sw, support, errorCorrectionCount);
	}
	
	public Record(long timestamp, int id, Coordinate nw, Coordinate ne, Coordinate sw, int support, int errorCorrectionCount)
	{
		this.timestamp = timestamp;
		this.id = id;
		this.tlPattern = nw.clone();
		this.trPattern = ne.clone();
		this.blPattern = sw.clone();
		this.support = support;
		this.errorCorrectionCount = errorCorrectionCount;
		deriveValues();
	}

	public Record(Record record)
	{
		this.timestamp = record.timestamp;
		id = record.id;
		tlPattern = record.tlPattern.clone();
		trPattern = record.trPattern.clone();
		blPattern = record.blPattern.clone();
		support = record.support;
		errorCorrectionCount = record.errorCorrectionCount;
		deriveValues();
	}

	public Record(String[] tokens)
	{
		timestamp = Long.parseLong(tokens[TIMESTAMP]);
		id = Integer.parseInt(tokens[ID]);
		tlPattern = new Coordinate(Integer.parseInt(tokens[TL_PATTERN_X]), Integer.parseInt(tokens[TL_PATTERN_Y]));
		trPattern = new Coordinate(Integer.parseInt(tokens[TR_PATTERN_X]), Integer.parseInt(tokens[TR_PATTERN_Y]));
		blPattern = new Coordinate(Integer.parseInt(tokens[BL_PATTERN_X]), Integer.parseInt(tokens[BL_PATTERN_Y]));
		support = Integer.parseInt(tokens[SUPPORT]);
		//TODO
//		errorCorrectionCount = Integer.parseInt(tokens[ERROR_CORRECTION_COUNT]); 		
		deriveValues();
	}

	public Object[] toTokens()
	{
		Object[] tokens = new Object[FIELD_COUNT];
		tokens[TIMESTAMP] = timestamp;
		tokens[TL_PATTERN_X] = Math.round(tlPattern.x);
		tokens[TL_PATTERN_Y] = Math.round(tlPattern.y);
		tokens[TR_PATTERN_X] = Math.round(trPattern.x);
		tokens[TR_PATTERN_Y] = Math.round(trPattern.y);
		tokens[BL_PATTERN_X] = Math.round(blPattern.x);
		tokens[BL_PATTERN_Y] = Math.round(blPattern.y);
		tokens[ID] = id;
		tokens[SUPPORT] = support; 
//		tokens[ERROR_CORRECTION_COUNT] = errorCorrectionCount; 
		return tokens;
	}

	public void roundPatternCoordinates()
	{
		tlPattern.round();
		trPattern.round();
		blPattern.round();
	}
	
	private void deriveValues()
	{
				
		// calculate center 
		Vector bltl = new Vector(blPattern, tlPattern);
		Vector tltr = new Vector(tlPattern, trPattern);
		Vector bltr = bltl.plus(tltr);
		bltr.scale(0.5f);
		center = bltr.terminal(blPattern);
		
		// calculate orientation vector
		orientation = bltr.clone();
		orientation.rotate(Angles.MINUS_QUARTER_PI);
		orientation.normalize();

		// calculate module size
		modulesizeX = (float) (tlPattern.distanceTo(trPattern) / BCODE_BIG_SQUARE_SMALL_SQUARE_DISTANCE);
		modulesizeY = (float) (tlPattern.distanceTo(blPattern) / BCODE_BIG_SQUARE_SMALL_SQUARE_DISTANCE);
		modulesize = (float) (modulesizeX + modulesizeY) / 2;

	}

	@Override
	public boolean equals(Object object)
	{
		if (object == null) return false;
		if (object.getClass() != getClass()) return false;
		Record other = (Record) object;
		return (timestamp == other.timestamp) && (tlPattern.equals(other.tlPattern)) && (trPattern.equals(other.trPattern)) && (blPattern.equals(other.blPattern)) && (id == other.id) && (support == other.support);
	}

	@Override
	public int hashCode()
	{
		return (((int) (timestamp & 0xFFFF)) << 16) + (id & 0xFFFF);
	}

}
