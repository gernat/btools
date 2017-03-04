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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.gernat.btools.common.image.Pixel;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Walk
implements Cloneable
{
	
	private static final byte COLOR_0 = 0;
	
	private static final byte COLOR_1 = 1;
	
	private static final byte COLOR_2 = 2;
	
	private static final byte COLOR_3 = 3;
	
	private static final byte COLOR_4 = 4;
	
	private static final byte COLOR_5 = 5;
	
	private static final int MAX_NEIGHBOR_COUNT = 8;
	
	private static final int MAX_WALK_LENGTH = 139; //140; //pixels

	private static final int MIN_WALK_LENGTH = 0; // pixels
	
	private static final int MAX_THIN_SEGMENT_THICKNESS = 10; //pixels
	
	private static final int MIN_THIN_SEGMENT_LENGTH = 0; // pixels
	
	public Pixel start;
	
	public Pixel end;

	public int length;

	public ArrayList<Pixel> pixels;

	public int longestThinSegmentLength;
	
	public ArrayList<Integer> pathThickness; // may be null
	
	public Walk(Pixel start)
	{
		this.start = start;
		this.end = start;
		pixels = new ArrayList<>();
		pixels.add(start);
		length = 1;
		longestThinSegmentLength = 0;		
	}
	
	public Walk extend(Pixel pixel)
	{
		pixels.add(pixel);
		end = pixel;
		length++;
		return this;
	}
	
	public Walk clone() 
	{
		Walk walk = null;
		try
		{
			walk = (Walk) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}
		walk.pixels = new ArrayList<>(pixels.size() + 1);
		walk.pixels.addAll(pixels);
		walk.start = new Pixel(start);
		walk.end = new Pixel(end);
		return walk;
	}
	
	public static Walk computeOneWalkBetween(Pixel[] sourceHeadPixels, Pixel[] targetHeadPixels, Pixel targetHeadCenter, ByteProcessor imageProcessor, float[] localThickness, int maxPathThickness, int maxThickSegmentLength, HashSet<Pixel> visitedPixels)  
	{
		
		// create a copy of the image processor and get image pixels
		imageProcessor = (ByteProcessor) imageProcessor.duplicate();
		byte[] pixels = (byte[]) imageProcessor.getPixels();
		int width = imageProcessor.getWidth();
		int height = imageProcessor.getHeight();

		// return null if there is no walk connecting the two heads; otherwise
		// set all pixels that connect the two heads to COLOR_5
		if (!isHeadsConnected(sourceHeadPixels, targetHeadPixels, imageProcessor, pixels)) return null;			
		colorConnectingPixels(sourceHeadPixels, targetHeadPixels, imageProcessor, pixels);
		
		// create initial set of walks 
		ArrayList<Walk> incomplete = createInitialWalks(sourceHeadPixels, pixels, width, height);
		
		// process all incomplete walks
		Set<Pixel> targetPixelSet = new HashSet<>(Arrays.asList(targetHeadPixels));
		while (!incomplete.isEmpty())
		{

			// process next walk
			Walk walk = incomplete.remove(incomplete.size() - 1);
			visitedPixels.add(walk.start);
			while (true)
			{
				
				// remember which pixels were visited
				visitedPixels.add(walk.end);
				
				// drop this walk if it is too long to be extended further
				if (walk.length == MAX_WALK_LENGTH) break;
				
				// stop extending this walk if a target has been reached; 
				// return walk if it has a minimum length and a thin segment
				// longer than the specified threshold length
				if (targetPixelSet.contains(walk.end))
				{
					if (walk.length >= MIN_WALK_LENGTH)
					{
						walk.pathThickness = getPathThickness(walk, localThickness, width);
						if (!ShapeMachine.isTouchShaped(walk.pathThickness, maxThickSegmentLength, maxPathThickness, MAX_THIN_SEGMENT_THICKNESS, MIN_THIN_SEGMENT_LENGTH)) break;
						walk.longestThinSegmentLength = ShapeMachine.getLongestThinSegmentLength();
						return walk;
					}
					break;
				}

				// stop extending this walk if its end-pixel has no more black 
				// neighboring pixels
				List<Pixel> neighbors = getSomeNeighborsOf(walk.end.x, walk.end.y, width, height, pixels, COLOR_5);
				neighbors.removeAll(walk.pixels);
				if (neighbors.isEmpty()) break;
				
				// extend this walk with a neighbor and create a new walk for 
				// each neighbor that is not used to extend this walk
				walk.extend(processNeighbors(neighbors, walk, targetHeadCenter, incomplete));

			}
			
		}
		
		// found no walk connecting heads
		return null;
		
	}
	
	// returns a neighbor pixel that is closest to the target head; if there 
	// are multiple neighbors, this method creates a new walk for each  
	// neighbor that is not returned and adds this walk to the specified list
	// of incomplete walks; the list of neighbor pixels must not be empty
	private static Pixel processNeighbors(List<Pixel> neighbors, Walk walk, Pixel targetHeadCenter, ArrayList<Walk> incomplete)
	{
		Pixel pixel = null;
		int neighborCount = neighbors.size();	
		if (neighborCount == 1) pixel = neighbors.get(0);
		else
		{
			int distance = Integer.MAX_VALUE;
			for (int i = 0; i < neighborCount; i++) 
			{
				Pixel p = neighbors.get(i);
				int d = Math.max(Math.abs(targetHeadCenter.x - p.x), Math.abs(targetHeadCenter.y - p.y));
				if (d < distance)
				{
					if (pixel != null) incomplete.add(walk.clone().extend(pixel));
					pixel = p;
					distance = d;
				}
				else incomplete.add(walk.clone().extend(p));
			}
		}
		return pixel;
	}
	
	// gets the thickness for each pixel of the specified walk
	private static ArrayList<Integer>getPathThickness(Walk walk, float[] localThickness, int width)
	{
		ArrayList<Integer> thickness = new ArrayList<>();
		int lastPixel = walk.pixels.size();
		for (int i = 0; i < lastPixel; i++)
		{
			Pixel p = walk.pixels.get(i);
			thickness.add(Math.round(localThickness[p.y * width + p.x]));
		}
		return thickness;
	}

	// creates a walk from each source head pixel to its adjacent black  
	// (COLOR_5) pixels; if black pixel is adjacent to two or more source  
	// head pixels, then only one of the shortest walks is created   
	private static ArrayList<Walk> createInitialWalks(Pixel[] sourceHeadPixels, byte[] pixels, int width, int height)
	{
		
		//TODO can this be simplified (smaller source code), e.g. by creating each possible extension and then dropping paths with non-unique ends? 
		// or exclude source and target pixels from walk?
		// create a walk for each source pixel
		ArrayList<Walk> incomplete = new ArrayList<>();
		for (Pixel source : sourceHeadPixels) 
		{
			List<Pixel> neighbors = getAllNeighborsOf(source.x, source.y, width, height, pixels, COLOR_5); 
			for (Pixel neighbor : neighbors) 
			{
				int distance = source.distanceTo(neighbor);
				if (distance == 1)
				{
					Walk w = new Walk(source);
					w.extend(neighbor);
					incomplete.add(w);
				}
				else
				{
					int minDistance = Integer.MAX_VALUE;
					for (Pixel otherSource : sourceHeadPixels)
					{
						int d = otherSource.distanceTo(neighbor);
						if (d < minDistance) minDistance = d;
					}
					if (minDistance > 1)
					{
						Walk w = new Walk(source);
						w.extend(neighbor);
						incomplete.add(w);						
					}
				}
			}
		}
		
		// if multiple walks have the same end, drop all but one 
		ArrayList<Walk> tmp = new ArrayList<>();
		int walkCount = incomplete.size();
		for (int i = 0; i < walkCount; i++)
		{
			Walk walk = incomplete.get(i);
			boolean uniqueEnd = true;
			for (int j = i + 1; (j < walkCount) && (uniqueEnd); j++) if (walk.end.equals(incomplete.get(j).end)) uniqueEnd = false;
			if (uniqueEnd) tmp.add(walk);
		}

		// return walks
		return tmp;
		
	}

	// set all COLOR_1 colored pixels that connect the two heads to COLOR_5
	private static void colorConnectingPixels(Pixel[] sourceHeadPixels, Pixel[] targetHeadPixels, ByteProcessor imageProcessor, byte[] image)
	{
		floodFill(sourceHeadPixels, COLOR_1, targetHeadPixels, COLOR_2, image, imageProcessor, COLOR_3);
		floodFill(targetHeadPixels, COLOR_3, sourceHeadPixels, COLOR_4, image, imageProcessor, COLOR_5);
	}

	// return null if there is no walk connecting the two heads; set pixels 
	// which can be reached from the source head to color 1
	private static boolean isHeadsConnected(Pixel[] sourceHeadPixels, Pixel[] targetHeadPixels, ByteProcessor imageProcessor, byte[] image)
	{
		floodFill(sourceHeadPixels, COLOR_0, targetHeadPixels, COLOR_0, image, imageProcessor, COLOR_1);
		if (image[targetHeadPixels[0].y * imageProcessor.getWidth() + targetHeadPixels[0].x] == COLOR_1) return true;
		else return false;
	}

	/**
	 * Returns the 8-connected neighbor pixels of the specified pixel. 
	 */
	private static List<Pixel> getAllNeighborsOf(int x, int y, int width, int height, byte[] image, byte neighborColor)
	{
		ArrayList<Pixel> neighbors = new ArrayList<>(MAX_NEIGHBOR_COUNT);
		for (int j = y - 1; j <= y + 1; j++)
		{
			for (int i = x - 1; i <= x + 1; i++)
			{
				if (((i != x) || (j != y)) && (i >= 0) && (i < width) && (j >= 0) && (j < height) && (image[j * width + i] == neighborColor)) neighbors.add(new Pixel((short) i, (short)j));
			}
		}
		return neighbors;
	}

	/**
	 * Returns the neighbor pixels of the specified pixel. 8-connected 
	 * neighbors are only returned if they cannot be reached via a 4-connected 
	 * neighbor.
	 */
	private static List<Pixel> getSomeNeighborsOf(int x, int y, int width, int height, byte[] image, byte neighborColor)
	{
		ArrayList<Pixel> neighbors = new ArrayList<>(MAX_NEIGHBOR_COUNT);
		for (int j = y - 1; j <= y + 1; j++)
		{
			for (int i = x - 1; i <= x + 1; i++)
			{
				if (((i != x) || (j != y)) && (i >= 0) && (i < width) && (j >= 0) && (j < height) && (image[j * width + i] == neighborColor)) 
				{
					int k = i - x;
					int l = j - y;
					if (((k == 0) || (l == 0)) || ((i != 0) && (j != 0) && (image[y * width + x + k] != neighborColor) && (image[(y + l) * width + x] != neighborColor))) neighbors.add(new Pixel((short) i, (short)j));
 				}
			}
		}
		return neighbors;
	}
	
	/**
	 * Draws pixels in source and target onto image and then flood-fills image
	 * starting at the first source pixel. 
	 */
	private static void floodFill(Pixel[] sourcePixels, byte sourceColor, Pixel[] targetPixels, byte targetColor, byte[] image, ByteProcessor processor, byte floodColor)
	{
		int width = processor.getWidth();
		for (Pixel pixel : sourcePixels) image[pixel.y * width + pixel.x] = sourceColor;
		for (Pixel pixel : targetPixels) image[pixel.y * width + pixel.x] = targetColor;
		processor.setValue(floodColor);
		FloodFiller floodFiller = new FloodFiller(processor);
		floodFiller.fill8(sourcePixels[0].x, sourcePixels[0].y);
	}
}
