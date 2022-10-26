/*
 * Copyright (C) 2021, 2022 University of Illinois Board of Trustees.
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
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.record.RecordReader;
import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.tracking.bcode.BCode;

public class MovementDetector
{

	private static void detectMovement(String bCodeDetectionFile, String movementFile, int frameRate, double minLinearDisplacement, double maxLinearDisplacement, double minAngularDisplacement, double maxAngularDisplacement, double mmPerPixel) throws IOException
	{

		// open bCode detection file
		RecordReader bCodeReader = new RecordReader(bCodeDetectionFile);

		// initialize variables
		int expectedTimeBetweenFrames = (int) (1.0 / frameRate * 1000);
		int maxTimeCreep = expectedTimeBetweenFrames / 10;
		Coordinate[] lastPosition = new Coordinate[BCode.UNIQUE_ID_COUNT];
		Vector[] lastOrientation = new Vector[BCode.UNIQUE_ID_COUNT];
		long[] lastTime = new long[BCode.UNIQUE_ID_COUNT];
		
		// process bCode detection file
		TokenWriter movementWriter = new TokenWriter(movementFile, ",");
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
					
					// calculate linear and angular displacement 
					float distance = lastPosition[record.id].distanceTo(record.center);
					float angle = lastOrientation[record.id].angleBetween(record.orientation);
					
					// write movement to file if the linear and/or angular 
					// displacement is within the specified thresholds
					if (((minLinearDisplacement != -1) && (minAngularDisplacement != -1) && ((distance >= minLinearDisplacement) || (Math.abs(angle) >= minAngularDisplacement))) || ((maxLinearDisplacement != -1) && (maxAngularDisplacement != -1) && (distance <= maxLinearDisplacement) && (Math.abs(angle) <= maxAngularDisplacement)) || ((minLinearDisplacement != -1) && (minAngularDisplacement != -1) && (maxLinearDisplacement != -1) && (maxAngularDisplacement != -1))) movementWriter.writeTokens(timestamp, record.id, (float) (distance * mmPerPixel), angle);
					
				}
				
				// remember when, where, and in which orientation this bCode 
				// was last detected
				lastTime[record.id] = timestamp;
				lastPosition[record.id] = record.center;
				lastOrientation[record.id] = record.orientation;
				
			}		

		}
		
		// close files
		movementWriter.close();
		bCodeReader.close();

	}
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("Movement detector (bTools) 0.15.1");
		System.out.println("Copyright (C) 2017-2022 University of Illinois Board of Trustees");
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
		System.out.println("- filtered.data.file       file containing the bCode detection results. Must be");		
		System.out.println("                           sorted by timestamp column");
		System.out.println("- frame.rate               frame rate at which bCodes were recorded");
		System.out.println("- max.angular.displacement absolute maximum angular displacement to be reported");
		System.out.println("- max.linear.displacement  maximum linear displacement to be reported");
		System.out.println("- min.angular.displacement absolute minimum angular displacement to be reported");
		System.out.println("- min.linear.displacement  minimum linear displacement to be reported");
		System.out.println("- mm.per.pixel             real-world spatial extend represented by a single");
		System.out.println("                           pixel");
		System.out.println("- movement.file            output file containing the movement detections");
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
		boolean minThresholdsPresent = parameters.exists("min.linear.displacement") || parameters.exists("min.angular.displacement");
		boolean maxThresholdsPresent = parameters.exists("max.linear.displacement") || parameters.exists("max.angular.displacement");
		if (minThresholdsPresent && maxThresholdsPresent) throw new IllegalStateException("Minimum and maximum displacement parameters cannot be specified together.");
		double minAngularDisplacement = -1;
		double minLinearDisplacement = -1;
		double maxAngularDisplacement = -1; 
		double maxLinearDisplacement = -1;
		if (minThresholdsPresent)
		{
			minLinearDisplacement = parameters.exists("min.linear.displacement") ? parameters.getDouble("min.linear.displacement") : 0;
			minAngularDisplacement = parameters.exists("min.angular.displacement") ? parameters.getDouble("min.angular.displacement") : 0;
			minLinearDisplacement /= mmPerPixel;
			minAngularDisplacement = minAngularDisplacement / 180 * Math.PI;
		}
		if (maxThresholdsPresent)
		{
			maxLinearDisplacement = parameters.exists("max.linear.displacement") ? parameters.getDouble("max.linear.displacement") : Double.MAX_VALUE;
			maxAngularDisplacement = parameters.exists("max.angular.displacement") ? parameters.getDouble("max.angular.displacement") : Double.MAX_VALUE; 
			maxLinearDisplacement /= mmPerPixel;
			maxAngularDisplacement = maxAngularDisplacement / 180 * Math.PI;
		}
		String movementFile = parameters.getString("movement.file");
		
		// detect movement
		detectMovement(filteredDataFile, movementFile, frameRate, minLinearDisplacement, maxLinearDisplacement, minAngularDisplacement, maxAngularDisplacement, mmPerPixel);

	}

}
