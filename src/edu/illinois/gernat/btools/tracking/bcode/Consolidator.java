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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import edu.illinois.gernat.btools.common.geometry.Coordinate;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Consolidator
{
			
	private Consolidator()
	{
	}
 
	public static List<MetaCode> consolidate(List<BCode> beeIDs)
	{
		
		// do nothing if there is nothing to do
		if (beeIDs.size() == 0) return new LinkedList<MetaCode>();

		// group bee IDs according to their integer positions
		HashMap<Coordinate, LinkedList<BCode>> groups = new HashMap<Coordinate, LinkedList<BCode>>();		
		for (BCode beeID : beeIDs) 
		{
			Coordinate coordinate = new Coordinate(Math.round(beeID.center.getX()), Math.round(beeID.center.getY()));
			if (!groups.containsKey(coordinate)) groups.put(coordinate, new LinkedList<BCode>());
			LinkedList<BCode> list = groups.get(coordinate);
			list.add(beeID);
		}
		
		// create initial list of meta IDs
		List<MetaCode> metaIDs = new ArrayList<MetaCode>();
		for (LinkedList<BCode> group : groups.values())
		{
			MetaCode metaID = new MetaCode(group);
			metaIDs.add(metaID);
		}

		// calculate distance the centers of two tags must have for the tags
		// to be considered two independent labels; the label margin was 
		// purposefully excluded from this calculation
		boolean decodedOnly = false;
		float sum = 0;
		float count = 0;
		for (MetaCode metaID : metaIDs) 
		{
			if ((!decodedOnly) && (metaID.data != -1))
			{
				sum = 0;
				count = 0;
			}
			if ((decodedOnly) && (metaID.data == -1)) continue;
			sum += metaID.moduleSize;
			count++;
		}
		float minCenterDistance = sum / count * (float) BCode.DIMENSION / 2;
		
		//
		LinkedList<MetaCode> neighbors = new LinkedList<MetaCode>();
		int idCount = metaIDs.size();		
		for (int index1 = 0; index1 < idCount; index1++)
		{
					
			//
			MetaCode id1 = metaIDs.get(index1);
			neighbors.clear();
			for (int index2 = index1 + 1; index2 < idCount; index2++)
			{
				MetaCode id2 = metaIDs.get(index2);
				//TODO add norm method to coordinate and replace this with call to norm
				if ((Math.abs(id1.center.x - id2.center.x) <= minCenterDistance) && (Math.abs(id1.center.y - id2.center.y) <= minCenterDistance)) neighbors.add(id2);
			}
			
			//
			if (neighbors.size() == 0) continue;
			LinkedList<BCode> neighboringBeeIDs = new LinkedList<BCode>();
			for (MetaCode neighbor : neighbors) neighboringBeeIDs.addAll(neighbor.beeIDs); 
			id1.add(neighboringBeeIDs);
			metaIDs.removeAll(neighbors);
			idCount -= neighbors.size();
			
		}

		//
		return metaIDs;
		
	}
		
}
