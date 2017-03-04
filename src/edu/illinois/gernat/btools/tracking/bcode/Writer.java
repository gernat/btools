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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.zxing.common.BitMatrix;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public final class Writer
{

	public static final int TYPE_SOLID = 0;

	public static final int TYPE_GRID = 1;
	
	public static final int TYPE_GRID2 = 2;
	
	public static final int TYPE_GRID3 = 3;

	private Writer()
	{
	}
	
	public static int getLabelSideLength(int type, int zoom)
	{
		int sideLength = (BCode.DIMENSION + 2 * BCode.MARGIN) * zoom;
		if ((type == TYPE_GRID) || (type == TYPE_GRID2)) sideLength--;
		return sideLength;
	}
	
	public static BufferedImage create(int data, int type, int zoom, boolean invert)
	{
		
		// create bit matrix
		if (zoom < 1) throw new IllegalArgumentException();
		BitMatrix code = Encoder.encode(data);
		
		// invert colors, if requested
		if (invert)
		{
			for (int x = 0; x < code.width; x++)
			{
				for (int y = 0; y < code.height; y++)
				{
					code.flip(x, y);
				}
			}
		}
		
		// create image
		int imageSideLength = getLabelSideLength(type, zoom);
		BufferedImage image = new BufferedImage(imageSideLength, imageSideLength, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		
		// draw label
		for (int x = 0; x < code.width; x++)
		{
			for (int y = 0; y < code.height; y++)
			{
				Color color = code.get(x, y) ? Color.BLACK : Color.WHITE;
				graphics.setColor(color);
				graphics.fillRect(x * zoom, y * zoom, zoom, zoom);
			}
		}
		if (type == TYPE_SOLID) return image;
		
		// decorate label to accommodate printer inaccuracies 
		graphics.setColor(Color.WHITE);
		for (int x = 0; x < code.width; x++)
		{
			for (int y = 0; y < code.height; y++)
			{
				if (type == TYPE_GRID) 
				{
					graphics.drawLine(x * zoom, y * zoom + zoom - 1, x * zoom + zoom - 1, y * zoom + zoom - 1);
					graphics.drawLine(x * zoom + zoom - 1, y * zoom, x * zoom + zoom - 1, y * zoom + zoom - 1);
				}
				if (((type == TYPE_GRID2) || (type == TYPE_GRID3))  && (!code.get(x, y)) && (x > 0) && (y > 0))
				{
					graphics.drawLine(x * zoom - 1, y * zoom - 1, x * zoom + zoom - 1, y * zoom - 1);
					graphics.drawLine(x * zoom - 1, y * zoom - 1, x * zoom - 1, y * zoom + zoom - 1);
				}
				if ((type == TYPE_GRID3) && (x > 0) && (y > 0))
				{
					if (code.get(x - 1, y - 1) && code.get(x, y - 1) && code.get(x - 1, y) && (!code.get(x, y))) graphics.drawLine(x * zoom - 2, y * zoom - 2, x * zoom - 2, y * zoom - 2);
					if (code.get(x - 1, y - 1) && code.get(x, y - 1) && (!code.get(x - 1, y)) && code.get(x, y)) graphics.drawLine(x * zoom, y * zoom - 2, x * zoom, y * zoom - 2);
					if ((!code.get(x - 1, y - 1)) && code.get(x, y - 1) && code.get(x - 1, y) && code.get(x, y)) graphics.drawLine(x * zoom, y * zoom, x * zoom, y * zoom);
					if (code.get(x - 1, y - 1) && (!code.get(x, y - 1)) && code.get(x - 1, y) && code.get(x, y)) graphics.drawLine(x * zoom - 2, y * zoom, x * zoom - 2, y * zoom);
				}
			}
		}
		return image;

	}
	
	public static void write(int data, int zoom, int type, boolean invert, String filename) throws IOException
	{
		BufferedImage image = create(data, zoom, type, invert);
		File file = new File(filename);
		ImageIO.write(image, "png", file);
	}
	
}
