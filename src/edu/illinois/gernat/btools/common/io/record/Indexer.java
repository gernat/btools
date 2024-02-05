/*
 * Copyright (C) 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024 University of  
 * Illinois Board of Trustees.
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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;

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
		System.out.println("Indexer (bTools) 0.18.0");
		System.out.println("Copyright (C) 2017-2024 University of Illinois Board of Trustees");
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
		System.out.println("- file the file to be indexed");
	}
	
	public static int determineLineSeparatorLength(String file) throws IOException 
	{
	    char c;
	    FileInputStream fis = new FileInputStream(file);
	    try 
	    {
	        while (fis.available() > 0) 
	        {
	            c = (char) fis.read();
	            if (c == '\n') return 1;
	            if (c == '\r') 
	            {
	                if (fis.available() > 0) 
	                {
	                    c = (char) fis.read();
	                    if (c == '\n') return 2;
	                    else return 1;
	                }
	                return 1;
	            }
	        }
	    } 
	    finally 
	    {
	        if (fis!=null) fis.close();
	    }
	    return -1;
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
		
		// get arguments
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);		
		String file = parameters.getString("file");
		
		// determine line separator size
		int eolByteCount = determineLineSeparatorLength(file);
		if (eolByteCount == -1) throw new IllegalStateException("Could not determine line separator size.");
			
		// index file
		Indexer.index(file, eolByteCount); 
		
	}

}
