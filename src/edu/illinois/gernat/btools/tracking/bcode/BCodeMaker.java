/*
 * Copyright (C) 2017, 2018 University of Illinois Board of Trustees.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;

import com.google.zxing.NotFoundException;

import edu.illinois.gernat.btools.common.parameters.Parameters;

/**
 * @version 0.12.1
 * @since 0.12.0
 * @author Tim Gernat
 */
public class BCodeMaker
{
	
	private static final int ID_START = 1; // This should never be 0
	
	private static final int ID_INCREMENT = 1;

	private static final int ID_COUNT = 2047;
	
	private static final boolean LABEL_INVERT = false;

	private static final int LABEL_TYPE = Writer.TYPE_GRID3;
	
	private static final float LABEL_SPACER_WIDTH = 0f; // mm 
	
	private static final int CUTTING_LINE_WIDTH = 1; // dots
	
	private static final float WHISKER_LENGTH = 5f; // mm

	private static final int GROUP_X_COUNT = 2;

	private static final int GROUP_Y_COUNT = 4;

	private static final int GROUP_SIDE_LENGTH = 16;

	private static final float GROUP_SPACER_WIDTH = 30; // mm

	private static final int PRINTER_DPI = 1200; // dots
		
	public static final float MM_PER_INCH = 25.4f;

	private static void drawLines(Graphics2D graphics, int groupSideLength, int groupSpacerWidth, int squareSideLength, int squareSpacerWidth, int cuttingLineExcessLength, int cuttingLineWidth)
	{

		// loop over group rows and columns 
		for (int groupRow = 0; groupRow < GROUP_Y_COUNT; groupRow++)
		{
			for (int groupColumn = 0; groupColumn < GROUP_X_COUNT; groupColumn++) 				
			{

				// calculate group coordinate offsets
				int groupXOffset = groupSpacerWidth + groupColumn * (groupSideLength + groupSpacerWidth);
				int groupYOffset = groupSpacerWidth + groupRow * (groupSideLength + groupSpacerWidth);

				// loop over rows 
				for (int squareRow = -1; squareRow <= GROUP_SIDE_LENGTH; squareRow++) 
				{
					
					// calculate offset
					int y = groupYOffset + squareRow * (squareSideLength + squareSpacerWidth);
					
					// draw whiskers
					graphics.setColor(Color.BLACK);
					if (squareRow > - 1) graphics.drawLine(groupXOffset - cuttingLineExcessLength, y, groupXOffset + groupSideLength + cuttingLineExcessLength - 1, y);
					if (squareRow < GROUP_SIDE_LENGTH) graphics.drawLine(groupXOffset - cuttingLineExcessLength, y + squareSideLength - 1, groupXOffset + groupSideLength + cuttingLineExcessLength - 1, y + squareSideLength - 1);
					
					// draw dividers
					if (LABEL_INVERT) graphics.setColor(Color.WHITE);
					else graphics.setColor(Color.BLACK);
					if (squareRow > - 1) graphics.drawLine(groupXOffset, y, groupXOffset + groupSideLength - 1, y);
					if (squareRow < GROUP_SIDE_LENGTH) graphics.drawLine(groupXOffset, y + squareSideLength - 1, groupXOffset + groupSideLength - 1, y + squareSideLength - 1);

				}

				// loop over columns
				for (int squareColumn = - 1; squareColumn <= GROUP_SIDE_LENGTH; squareColumn++) 
				{
					
					// calculate offset
					int x = groupXOffset + squareColumn * (squareSideLength + squareSpacerWidth);

					// draw whiskers
					graphics.setColor(Color.BLACK);
					if (squareColumn > - 1) graphics.drawLine(x, groupYOffset - cuttingLineExcessLength, x, groupYOffset + groupSideLength + cuttingLineExcessLength - 1);
					if (squareColumn < GROUP_SIDE_LENGTH) graphics.drawLine(x + squareSideLength - 1, groupYOffset - cuttingLineExcessLength, x + squareSideLength - 1, groupYOffset + groupSideLength + cuttingLineExcessLength - 1);

					// draw dividers
					if (LABEL_INVERT) graphics.setColor(Color.WHITE);
					else graphics.setColor(Color.BLACK);
					if (squareColumn > - 1) graphics.drawLine(x, groupYOffset, x, groupYOffset + groupSideLength - 1);
					if (squareColumn < GROUP_SIDE_LENGTH) graphics.drawLine(x + squareSideLength - 1, groupYOffset, x + squareSideLength - 1, groupYOffset + groupSideLength - 1);
			
				}

			}
		}
		
	}

	private static void drawLabels(Graphics2D graphics, int groupSideLength, int groupSpacerWidth, int squareSideLength, int squareSpacerWidth, int cuttingLineWidth, int labelZoom, int labelExtraMargin)
	{

		// loop over group rows and columns 
		int data = ID_START;
		for (int groupRow = 0; groupRow < GROUP_Y_COUNT; groupRow++)
		{
			for (int groupColumn = 0; groupColumn < GROUP_X_COUNT; groupColumn++) 				
			{

				// calculate group coordinate offsets
				int groupXOffset = groupSpacerWidth + groupColumn * (groupSideLength + groupSpacerWidth);
				int groupYOffset = groupSpacerWidth + groupRow * (groupSideLength + groupSpacerWidth);
				
				// fill background
				if (LABEL_INVERT) graphics.setColor(Color.BLACK);
				else graphics.setColor(Color.WHITE);
				graphics.fillRect(groupXOffset, groupYOffset, groupSideLength, groupSideLength);
				
				// loop over squares
				for (int squareRow = 0; squareRow < GROUP_SIDE_LENGTH; squareRow++) 
				{
					for (int squareColumn = 0; squareColumn < GROUP_SIDE_LENGTH; squareColumn++) 
					{

						// draw label
						if (data <= ID_START + ID_INCREMENT * (ID_COUNT - 1))
						{
							BufferedImage label = Writer.create(data % BCode.UNIQUE_ID_COUNT, LABEL_TYPE, labelZoom, LABEL_INVERT);
							int x = groupXOffset + squareColumn * (squareSideLength + squareSpacerWidth) + cuttingLineWidth + labelExtraMargin;
							int y = groupYOffset + squareRow * (squareSideLength + squareSpacerWidth) + cuttingLineWidth + labelExtraMargin;
							graphics.drawImage(label, x, y, null);
							data += ID_INCREMENT;
						}
						
					}
				}
				
			}
		}

	}
	
	private static float toPixels(float mm)
	{
		return mm * PRINTER_DPI / MM_PER_INCH;
	}
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("bCode Maker (bTools) 0.13.0");
		System.out.println("Copyright (C) 2017, 2018 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar make_bcodes.jar PARAMETER=VALUE...");
		System.out.println("Draw 2048 unique bCodes.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- bcode.file	     output image file");
		System.out.println("- padding            amount of extra space (in pixels) between the cutting");
		System.out.println("                     guides and the (invisible) bCode border");
		System.out.println("- show.credits       set to \"true\" or 1 to display credits and exit");
		System.out.println("- square.side.length size (in pixels) of the squares are that make up a bCode");
	}
	
	private static void showCredits() throws IOException 
	{
		showVersionAndCopyright();
		System.out.println();
		System.out.println("This software uses the following third party software that is distributed");
		System.out.println("under its own terms:");
		System.out.println();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("LICENSE-3RD-PARTY"); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		while (reader.ready()) System.out.println(reader.readLine());
		reader.close();
		inputStream.close();
		System.exit(1);
	}

	public static void main(String[] args) throws IOException, NotFoundException
	{

		// show version, copyright, and usage information if no arguments were 
		// given on the command line 
		if (args.length == 0) 
		{
			showVersionAndCopyright();
			System.out.println();
			showUsageInformation();		
			System.exit(1);
		}
		
		// get arguments
		Parameters parameters = Parameters.INSTANCE; 
		parameters.initialize(args);
		if ((parameters.exists("show.credits")) && (parameters.getBoolean("show.credits"))) showCredits();

		// set parameters
		int labelZoom = parameters.getInteger("square.side.length"); // default: 5 dots
		int labelExtraMargin = parameters.getInteger("padding"); // default: 2 dots
		String labelFilename = parameters.getString("bcode.file"); 
		
		// perform some calculations
		int cuttingLineExcessLength = Math.round(toPixels(WHISKER_LENGTH));
		int labelWidthOffset = LABEL_TYPE == Writer.TYPE_SOLID ? 0 : -1;
		int squareSideLength = Writer.getLabelSideLength(LABEL_TYPE, labelZoom) + 2 * (CUTTING_LINE_WIDTH) + 2 * labelExtraMargin + labelWidthOffset;
		int squareSpacerWidth = Math.round(toPixels(LABEL_SPACER_WIDTH));
		int groupSideLength = GROUP_SIDE_LENGTH * squareSideLength + (GROUP_SIDE_LENGTH - 1) * squareSpacerWidth;
		int groupSpacerWidth = Math.round(toPixels(GROUP_SPACER_WIDTH / 2)) * 2;
		int width = GROUP_X_COUNT * groupSideLength + (GROUP_X_COUNT + 1) * groupSpacerWidth; 
		int height = GROUP_Y_COUNT * groupSideLength + (GROUP_Y_COUNT + 1) * groupSpacerWidth;		
		
		// create a blank image
		BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = canvas.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());		

		// draw cutting lines and labels
		drawLabels(graphics, groupSideLength, groupSpacerWidth, squareSideLength, squareSpacerWidth, CUTTING_LINE_WIDTH, labelZoom, labelExtraMargin);
		drawLines(graphics, groupSideLength, groupSpacerWidth, squareSideLength, squareSpacerWidth, cuttingLineExcessLength, CUTTING_LINE_WIDTH);

		// write labels to disk
		ImageIO.write(canvas, "png", new File(labelFilename));

	}
	
}
