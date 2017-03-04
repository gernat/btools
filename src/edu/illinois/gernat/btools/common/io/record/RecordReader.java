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

package edu.illinois.gernat.btools.common.io.record;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.illinois.gernat.btools.common.io.token.TokenReader;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class RecordReader
{

	private static final int DEFAULT_BUFFER_SIZE = 1048576;

	private TokenReader reader;
	
	private Record record;
	
	private long timestamp;
	
	public RecordReader(String filename) throws IOException
	{
		reader = new TokenReader(filename, ",", false, DEFAULT_BUFFER_SIZE);
		timestamp = -1;
	}

	public RecordReader(File file) throws IOException
	{
		this(file.getAbsolutePath());
	}

	public boolean hasMoreRecords() throws IOException 
	{
		return (record != null) || (reader.hasMoreLines());		
	}

	public Record readRecord() throws IOException 
	{
		if (!hasMoreRecords()) throw new IOException();
		Record record = null;
		if (this.record != null) 
		{
			record = this.record;
			this.record = null;
		}
		else record = new Record(reader.readTokens()); 
		timestamp = record.timestamp;
		return record; 		
	}
	
	public List<Record> readRecords() throws IOException
	{
		if (!hasMoreRecords()) throw new IOException();
		ArrayList<Record> records = new ArrayList<Record>();
		if (record != null) 
		{
			records.add(record);
			timestamp = record.timestamp;
			record = null;
		}
		while (reader.hasMoreLines())
		{
			record = new Record(reader.readTokens());
			if (timestamp == -1) timestamp = record.timestamp;
			if (timestamp == record.timestamp) 
			{
				records.add(record);
				record = null;
			}
			else break;
		}
		return records; 
	}

	public long getTimestamp()
	{
		return timestamp;
	}
	
	public void close() throws IOException
	{
		reader.close();
	}

}
