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

package edu.illinois.gernat.btools.common.io.token;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class TokenWriter
{
	
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	
	private PrintWriter writer;
	
	private String delimiter;

	public TokenWriter(String filename, boolean append) throws IOException
	{
		this(filename, ",", append, TokenWriter.DEFAULT_BUFFER_SIZE);
	}

	public TokenWriter(String filename) throws IOException
	{
		this(filename, ",", false, TokenWriter.DEFAULT_BUFFER_SIZE);
	}

	public TokenWriter(String filename, String delimiter) throws IOException
	{
		this(filename, delimiter, false, TokenWriter.DEFAULT_BUFFER_SIZE);
	}
	
    public TokenWriter(String filename, String delimiter, boolean append) throws IOException  
	{
		this(filename, delimiter, append, TokenWriter.DEFAULT_BUFFER_SIZE);
	}

    public TokenWriter(String filename, String delimiter, boolean append, int bufferSize) throws IOException  
	{
		writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, append), bufferSize));
		this.delimiter = delimiter;
	}

	public void writeTokens(Tokenizable tokenizable)
	{
		writeTokens(tokenizable.toTokens());
	}

    public void writeTokens(String[] tokens)
	{
    	writeTokens((Object[])tokens);
	}
    
	public void writeTokens(Object... tokens)
	{
		for (int i = 0; i < tokens.length; i++) 
		{
			writer.print(tokens[i]);
			if (i < tokens.length - 1) writer.print(delimiter);
		}
		writer.println();
	}
	
	public void close() 
	{
		writer.close();
	}
	
}
