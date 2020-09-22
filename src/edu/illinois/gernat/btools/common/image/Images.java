/*
 * Copyright (C) 2017, 2020 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.common.image;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.TimeZone;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Images
{
	
	private static final int PATH_ITERATOR_COORDINATE_X = 0;
	
	private static final int PATH_ITERATOR_COORDINATE_Y = 1;
	
	private static final int PATH_ITERATOR_FLATNESS = 1;
	
	public static String getImageFileNameForTimestamp(String imageBaseDirectory, long timestamp)
	{
		
		// create date directory name
		Date date = new Date(timestamp);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));  
		String dir1 = formatter.format(date);
		
		// create hour directory name
		formatter = new SimpleDateFormat("HH");  
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));  
		String dir2 = formatter.format(date);
		
		// create image file name
		formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");  
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));  
		String filename = formatter.format(date) + ".jpg";
		
		// return image file name
		if (imageBaseDirectory == null) return dir1 + File.separator + dir2 + File.separator + filename;
		else return imageBaseDirectory + File.separator + dir1 + File.separator + dir2 + File.separator + filename;
		
	}

	public static File getImageFileForTimestamp(String imageBaseDirectory, long timestamp)
	{
		return new File(getImageFileNameForTimestamp(imageBaseDirectory, timestamp));
	}
	
	public static long getTimestampFromFilename(String filename) throws ParseException
	{
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");  
		formatter.setTimeZone(TimeZone.getTimeZone("GMT")); 
		Path path = Paths.get(filename);
		filename = path.getFileName().toString();
		return formatter.parse(filename.substring(0, filename.lastIndexOf("."))).getTime();  
	}

	public static long getTimestampFromFile(File file) throws ParseException
	{
		return getTimestampFromFilename(file.getAbsolutePath());
	}
		
	public static BufferedImage makeCopy(BufferedImage original) 
	{
		 ColorModel colorModel = original.getColorModel();
		 boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
		 WritableRaster raster = original.copyData(null);
		 BufferedImage copy = new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
		 return copy;
	}

	public static BufferedImage getIndependentSubimage(BufferedImage image, int x, int y, int w, int h)
	{
		BufferedImage subImage = new BufferedImage(w, h, image.getType());
		Graphics2D graphics = subImage.createGraphics();
		graphics.drawImage(image.getSubimage(x, y, w, h), 0, 0, null);
		return subImage;
	}

	public static Pixel[] getBorderPixels(Shape shape, int tx, int ty, int w, int h)
	{
		LinkedHashSet<Pixel> result = new LinkedHashSet<Pixel>();
		PathIterator pathIterator = shape.getPathIterator(null, PATH_ITERATOR_FLATNESS);		
	    float point1[] = new float[6];
	    float point2[] = new float[6];
		while (!pathIterator.isDone())
		{
			System.arraycopy(point2, 0, point1, 0, 6);
			int seqmentType = pathIterator.currentSegment(point2);
			pathIterator.next();
			if ((seqmentType == PathIterator.SEG_MOVETO) || (seqmentType == PathIterator.SEG_CLOSE)) continue;
			result.addAll(Images.bresenham(Math.round(point1[PATH_ITERATOR_COORDINATE_X]), Math.round(point1[PATH_ITERATOR_COORDINATE_Y]), Math.round(point2[PATH_ITERATOR_COORDINATE_X]), Math.round(point2[PATH_ITERATOR_COORDINATE_Y]), tx, ty, w, h));
		}
		return result.toArray(new Pixel[result.size()]);
	}
	
	// algorithm from: http://en.wikipedia.org/wiki/Bresenham's_line_algorithm#Simplification
	private static ArrayList<Pixel> bresenham(int x0, int y0, int x1, int y1, int tx, int ty, int w, int h)
	{
		ArrayList<Pixel> pixels = new ArrayList<Pixel>();
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0); 
		int sx = 0;
		int sy = 0;
		if (x0 < x1) sx = 1;
		else sx = -1;
		if (y0 < y1) sy = 1;
		else sy = -1;
		int err = dx - dy;
		while (true)
		{
			int x = x0 + tx;
			int y = y0 + ty;
			if ((x >= 0) && (x < w) && (y >= 0) && (y < h)) pixels.add(new Pixel(x, y));
		    if ((x0 == x1) && (y0 == y1)) break;
		    int e2 = 2 * err;
		    if (e2 > -dy)
		    {
		        err = err - dy;
		        x0 = x0 + sx;
		    }
		    if ((x0 == x1) && (y0 == y1))
		    {
				x = x0 + tx;
				y = y0 + ty;
				if ((x >= 0) && (x < w) && (y >= 0) && (y < h))pixels.add(new Pixel(x, y));
		       	break;
		    }
		    if (e2 <  dx)
		    { 
		    	err = err + dx;
		    	y0 = y0 + sy;
		    }
		}
		return pixels;
	}
	
}
