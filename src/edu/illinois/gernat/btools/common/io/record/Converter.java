/*
 * Copyright (C) 2017, 2018, 2019, 2020 University of Illinois Board of 
 * Trustees.
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

import java.io.IOException;

import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;

public class Converter
{

	public static final int FIELD_TIMESTAMP = 0;
	
	public static final int FIELD_X = 1;
	
	public static final int FIELD_Y = 2;
	
	public static final int FIELD_DX = 3;
	
	public static final int FIELD_DY = 4;
	
	public static final int FIELD_BEE_ID = 5;
	
	private static void toPlainText(String sourceFileName, String destinationFileName) throws IOException
	{
		RecordReader reader = new RecordReader(sourceFileName);
		TokenWriter writer = new TokenWriter(destinationFileName, ",");
		while (reader.hasMoreRecords())
		{
			Record record = reader.readRecord();
			writer.writeTokens(record.timestamp, record.center.x, record.center.y, record.orientation.dx, record.orientation.dy, record.id);
		}
		writer.close();
		reader.close();		
	}

	private static void showVersionAndCopyright() 
	{
		System.out.println("Converter (bTools) 0.14.0");
		System.out.println("Copyright (C) 2017, 2018, 2019, 2020 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar converter.jar PARAMETER=VALUE...");
		System.out.println("Convert raw bCode detections to human-readable plain text.");
		System.out.println();  		
		System.out.println("Parameters:");  		
		System.out.println("- human.readable.file the human-readable output file");
		System.out.println("- raw.bCode.file      the raw input file");
	}	
	
	public static void main(String[] args) throws IOException
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
		
		// get parameters
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		String rawBCodeFile = parameters.getString("raw.bCode.file");
		String humanReadableFile = parameters.getString("human.readable.file");
		
		// convert raw data
		toPlainText(rawBCodeFile, humanReadableFile);
		
	}

}
