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

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import edu.illinois.gernat.btools.common.geometry.Angles;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.io.record.Record;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class MetaCode
{

	public ArrayList<BCode> beeIDs;

	public Coordinate center;

	public Vector orientation;
	
	public float moduleSize;
	
	public Coordinate nw;
	
	public Coordinate ne;
	
	public Coordinate sw;
	
	public Coordinate se; //TODO remove?
	
	public int data;
	
	public boolean isDecoded;

	public int support;
	
	public int errorCorrectionCount;
	
	private MetaCode()
	{
		beeIDs = new ArrayList<BCode>();
		data = -1;
		nw = new Coordinate(0, 0);
		ne = new Coordinate(0, 0);
		sw = new Coordinate(0, 0);
		se = new Coordinate(0, 0);
		center = new Coordinate(0, 0);
		orientation = new Vector(0, 0);
		isDecoded = false;
		support = 0;
	}
	
	public MetaCode(BCode beeID)
	{
		this();
		LinkedList<BCode> beeIDs = new LinkedList<BCode>();
		beeIDs.add(beeID);
		add(beeIDs);
	}

	public MetaCode(List<BCode> beeIDs)
	{
		this();
		add(beeIDs);
	}
	
	public void add(List<BCode> beeIDs)
	{
		for (BCode beeID : beeIDs)
		{
			if (!isDecoded) 
			{
				if (beeID.isDecoded) 
				{
					this.beeIDs.clear();
					isDecoded = true;
				}
				this.beeIDs.add(beeID);				
			}
			else if (beeID.isDecoded) this.beeIDs.add(beeID);
		}
		consolidate();
	}
	
	private void consolidate()
	{		
		
		// initialize data aggregation
		center.set(0, 0);
		moduleSize = 0;
		HashMap<Integer, Double> histogram = null;
		if (isDecoded) histogram = new HashMap<Integer, Double>();
		nw.set(0, 0);
		ne.set(0, 0);
		sw.set(0, 0);
		se.set(0, 0);
		
		// aggregate data
		for (BCode beeID : beeIDs)
		{
			center.x += beeID.center.getX();
			center.y += beeID.center.getY();			
			moduleSize += beeID.moduleSize;
			nw.x += beeID.finderPattern.getX();
			nw.y += beeID.finderPattern.getY();
			ne.x += beeID.topRight.getX();
			ne.y += beeID.topRight.getY();
			sw.x += beeID.bottomLeft.getX();
			sw.y += beeID.bottomLeft.getY();
			se.x += beeID.bottomRight.getX();
			se.y += beeID.bottomRight.getY();
			if (!isDecoded) continue;
			Double weight = histogram.get(beeID.data); 
			if (weight == null) weight = 0.0;
			weight += beeID.templateConservation;
			histogram.put(beeID.data, weight);
		}
		
		// consolidate coordinates 
		int count = beeIDs.size();		
		center.x /= count;
		center.y /= count;
		moduleSize /= count;
		nw.x /= count;
		nw.y /= count;
		ne.x /= count;
		ne.y /= count;
		sw.x /= count;
		sw.y /= count;
		se.x /= count;
		se.y /= count;
		Vector v1 = new Vector(sw, nw);
		Vector v2 = new Vector(nw, ne);
		orientation = v1.plus(v2);
		orientation.rotate(Angles.MINUS_QUARTER_PI);
		orientation.normalize();
		
		// consolidate ID data	//TODO better handle cases where there are two best weights
		if (isDecoded)
		{
			data = -1;
			double bestWeight = 0; 
			for (Integer key : histogram.keySet())
			{
				double weight = histogram.get(key);
				if (weight > bestWeight)
				{
					bestWeight = weight;
					data = key;
				}
			}				
		}
	
		// calculate how often this ID was seen and how often the decoded ID
		// was a result of error correction
		support = 0;
		errorCorrectionCount = 0;
		for (BCode	beeID : beeIDs) 
		{
			if (beeID.data == data) 
			{				
				support++;
				if (beeID.isErrorCorrected) errorCorrectionCount++;
			}
		}
		
	}

	public float[] calculateBoundingBoxCoordinates()
	{
		float[] minimumBoundingBox = new float[8];
		float distanceToCorner = BCode.DISTANCE_CENTER_CORNER * nw.distanceTo(se) / BCode.DISTANCE_TL_BR;
		Vector v = orientation.clone();
		v.rotate(Angles.MINUS_QUARTER_PI);
		Coordinate corner = center.clone();
		corner.translate(v, distanceToCorner);
		minimumBoundingBox[0] = corner.x;
		minimumBoundingBox[1] = corner.y;
		v.rotate(Angles.PI);
		corner = center.clone();
		corner.translate(v, distanceToCorner);
		minimumBoundingBox[4] = corner.x;
		minimumBoundingBox[5] = corner.y;
		distanceToCorner = BCode.DISTANCE_CENTER_CORNER * ne.distanceTo(sw) / BCode.DISTANCE_TR_BL;
		v.rotate(Angles.HALF_PI);
		corner = center.clone();
		corner.translate(v, distanceToCorner);
		minimumBoundingBox[2] = corner.x;
		minimumBoundingBox[3] = corner.y;
		v.rotate(Angles.PI);
		corner = center.clone();
		corner.translate(v, distanceToCorner);
		minimumBoundingBox[6] = corner.x;
		minimumBoundingBox[7] = corner.y;
		return minimumBoundingBox;
	}
	

	public Polygon createBoundingBox()
	{
		Polygon polygon = new Polygon();
		float[] boundingBoxCoordinates = calculateBoundingBoxCoordinates();
		polygon.addPoint(Math.round(boundingBoxCoordinates[0]), Math.round(boundingBoxCoordinates[1]));
		polygon.addPoint(Math.round(boundingBoxCoordinates[2]), Math.round(boundingBoxCoordinates[3]));
		polygon.addPoint(Math.round(boundingBoxCoordinates[4]), Math.round(boundingBoxCoordinates[5]));
		polygon.addPoint(Math.round(boundingBoxCoordinates[6]), Math.round(boundingBoxCoordinates[7]));
		return polygon;
	}
	
	@Override
	public String toString() 
	{
		return "<" + center.x + ", " + center.y + ", " + data + ", " + beeIDs.size() + ">";
	}

	public static MetaCode createFrom(Record record)
	{
		
		// calculate module size
		float distanceTLToBL = record.tlPattern.distanceTo(record.blPattern);
		float distanceTLToTR = record.tlPattern.distanceTo(record.trPattern);
		float modulesize = (distanceTLToBL / BCode.DISTANCE_TL_BL + distanceTLToTR / BCode.DISTANCE_TL_TR) / 2;  
		
		// create pattern objects
		BigSquare finderPattern = new BigSquare(record.tlPattern.x, record.tlPattern.y, modulesize);
		SmallSquare bottomLeft = new SmallSquare(record.blPattern.x, record.blPattern.y, modulesize);
		SmallSquare topRight = new SmallSquare(record.trPattern.x, record.trPattern.y, modulesize);
		
		// create bee id
		BCode beeID = BCode.createFrom(finderPattern, bottomLeft, topRight);
		beeID.data = record.id;
		beeID.isDecoded = beeID.data != -1;
		beeID.templateConservation = 1;
		
		// create meta id
		MetaCode metaID = new MetaCode(beeID);
		return metaID;
		
	}
	
}
