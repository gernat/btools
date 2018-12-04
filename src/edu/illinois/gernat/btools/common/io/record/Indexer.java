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

package edu.illinois.gernat.btools.common.io.record;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;

/**
 * @version 0.12.1
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Indexer
{

	private static final String INDEX_FILE_EXTENSION = ".idx";

	public static String getIndexFilenameFor(String filename)
	{
		return filename.substring(0, filename.lastIndexOf(".")) + INDEX_FILE_EXTENSION;
	}

	public static void index(String recordFileName, int eolByteCount) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(new File(recordFileName)), 1024000); 
		TokenWriter writer = new TokenWriter(getIndexFilenameFor(recordFileName), ",");
		String line = null;
		long position = 0;
		String tMinus1 = "";
		while ((line = reader.readLine()) != null) 
		{
			String t = line.split(",", -1)[Record.TIMESTAMP];
			if (!t.equals(tMinus1)) 
			{
				writer.writeTokens(t, position);
				tMinus1 = t;
			}
			position += line.length() + eolByteCount;
		}
		writer.close();
		reader.close();
	}

	private static void showVersionAndCopyright() 
	{
		System.out.println("Indexer (bTools) 0.13.0");
		System.out.println("Copyright (C) 2017, 2018 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar indexer.jar PARAMETER=VALUE...");
		System.out.println("Index bCode detection results.");
		System.out.println();  		
		System.out.println("Parameters:");  		
		System.out.println("- eol.byte.count length of the sequence of characters signifying the end of a");
		System.out.println("                 line of text in the file to be indexed");
		System.out.println("- file           the file to be indexed");
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException
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
		
		// index file
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		int eolByteCount = parameters.getInteger("eol.byte.count");
		String file = parameters.getString("file");
		Indexer.index(file, eolByteCount); 
		
	}

}
