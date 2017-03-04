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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class TokenReader
{

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	
	private BufferedReader reader;
	
	private String delimiter;
	
	private String[] peekedTokens;
	
	private boolean hasPeeked;

	public TokenReader(String filename, String delimiter) throws IOException
	{
		this(filename, delimiter, false, DEFAULT_BUFFER_SIZE);
	}
	
    public TokenReader(String filename, String delimiter, boolean skipHeader, int bufferSize) throws IOException 
	{
		this.delimiter = delimiter;
		reader = new BufferedReader(new FileReader(filename), bufferSize);
		if (skipHeader) readTokens();
	}

	public TokenReader(String filename) throws IOException
	{
		this(filename, ",", false, DEFAULT_BUFFER_SIZE);
	}

	public TokenReader(String filename, boolean skipHeader) throws IOException
	{
		this(filename, ",", skipHeader, DEFAULT_BUFFER_SIZE);
	}

	public boolean hasMoreLines() throws IOException 
	{
		return (hasPeeked) || (reader.ready());		
	}
	
	public String[] readTokens() throws IOException
	{
		if (hasPeeked)
		{
			hasPeeked = false;
			return peekedTokens;
		}
		if (!reader.ready()) throw new IOException();
		String line = reader.readLine();
		return line.split(delimiter, -1);
	}

	public String[] peekTokens() throws IOException
	{
		if (hasPeeked) throw new IllegalStateException();
		peekedTokens = readTokens();
		hasPeeked = true;
		return peekedTokens;
	}

	public void close() throws IOException
	{
		reader.close();
	}

}
