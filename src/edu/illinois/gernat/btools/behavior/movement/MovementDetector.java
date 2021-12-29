/*
 * Copyright (C) 2021 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.behavior.movement;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.record.RecordReader;
import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.tracking.bcode.BCode;

public class MovementDetector
{

	private static void detectMovement(String bCodeDetectionFile, String movementFile, int frameRate, float mmPerPixel) throws IOException
	{

		// open bCode detection file
		RecordReader bCodeReader = new RecordReader(bCodeDetectionFile);

		// write result file header
		TokenWriter movementWriter = new TokenWriter(movementFile, ",");
		movementWriter.writeTokens("timestamp", "id", "distance_traveled");

		// initialize variables
		int expectedTimeBetweenFrames = (int) (1.0 / frameRate * 1000);
		int maxTimeCreep = expectedTimeBetweenFrames / 10;
		Coordinate[] lastPosition = new Coordinate[BCode.UNIQUE_ID_COUNT];
		long[] lastTime = new long[BCode.UNIQUE_ID_COUNT];
		
		// process bCode detection file
		while (bCodeReader.hasMoreRecords())
		{
			
			// get bCode detections for next time point 
			List<Record> records = bCodeReader.readRecords();
			long timestamp = bCodeReader.getTimestamp();
			
			// process bCodes
			for (Record record : records)
			{
				
				// only report a distance for the current bCode if it was 
				// detected in the previous frame
				if ((timestamp - lastTime[record.id]) <= expectedTimeBetweenFrames + maxTimeCreep)
				{
					float distance = lastPosition[record.id].distanceTo(record.center);
					movementWriter.writeTokens(timestamp, record.id, distance * mmPerPixel);
				}
				
				// remember when and where this bCode was last detected
				lastTime[record.id] = timestamp;
				lastPosition[record.id] = record.center;
				
			}		

		}
		
		// close files
		movementWriter.close();
		bCodeReader.close();

	}
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("Movement detector (bTools) 0.14.0");
		System.out.println("Copyright (C) 2017, 2018, 2019, 2020, 2021 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar movement_detector.jar PARAMETER=VALUE...");
		System.out.println("Detect movement.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- filtered.data.file file containing the bCode detection results. Must be sorted");
		System.out.println("                     by timestamp column");
		System.out.println("- frame.rate         frame rate at which bCodes were recorded");
		System.out.println("- mm.per.pixel       real-world spatial extend represented by a single pixel");
		System.out.println("- movement.file      output file containing the movement detections");
	}

	public static void main(String[] args) throws IOException, InterruptedException, ParseException
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
		String filteredDataFile = parameters.getString("filtered.data.file");
		int frameRate = parameters.getInteger("frame.rate");
		double mmPerPixel = parameters.getDouble("mm.per.pixel");
		String movementFile = parameters.getString("movement.file");
		
		// detect movement
		detectMovement(filteredDataFile, movementFile, frameRate, (float) mmPerPixel);

	}

}
