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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class IndexedReader
{

	public static final int FIELD_TIMESTAMP = 0;
	
	public static final int FIELD_CURSOR = 1;
	
	public static String FILED_SEPARATOR = ",";
		
	private HashMap<Long, Long> cursorIndex;
		
	private HashMap<Long, Long> previousIndex;
	
	private HashMap<Long, Long> nextIndex;

	private RandomAccessFile file;
	
	private ArrayList<Long> timestamps;
	
	public IndexedReader(File file) throws IOException
	{
		this(file.getAbsolutePath());
	}
	
	public IndexedReader(String fileName) throws IOException
	{

		// the in-memory indices
		cursorIndex = new HashMap<Long, Long>();
		previousIndex = new HashMap<Long, Long>();
		nextIndex = new HashMap<Long, Long>();
		
		// load index from file
		timestamps = new ArrayList<Long>();
		BufferedReader reader = new BufferedReader(new FileReader(Indexer.getIndexFilenameFor(fileName)));
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split(FILED_SEPARATOR, -1);
			Long timestamp = Long.parseLong(tokens[FIELD_TIMESTAMP]);
			cursorIndex.put(timestamp, Long.parseLong(tokens[FIELD_CURSOR]));
			timestamps.add(timestamp);
		}
		reader.close();			
		
		// map each timestamp to the timestamp immediately before and after it
		int count = timestamps.size();
		Collections.sort(timestamps);
		for (int i = 0; i < count; i++)
		{
			if (i > 0) previousIndex.put(timestamps.get(i), timestamps.get(i - 1));
			if (i < count - 1) nextIndex.put(timestamps.get(i), timestamps.get(i + 1));
		}
		
		// open file
		file = new RandomAccessFile(fileName, "r");

	}
	
	public List<Record> readPrevious(long timestamp) throws IOException
	{
		Long previousTimestamp = previousIndex.get(timestamp);
		if (previousTimestamp == null) return null;
		else return readThis(previousTimestamp);
	}

	public List<Record> readThis(long timestamp) throws IOException
	{
		
		// jump to first byte of first record for this timestamp
		if (!cursorIndex.containsKey(timestamp)) return null;
		long currentCursorPosition = cursorIndex.get(timestamp);
		file.seek(currentCursorPosition);

		// calculate how many bytes to read until next timestamp
		long nextCursorPosition = nextIndex.containsKey(timestamp) ? cursorIndex.get(nextIndex.get(timestamp)) : file.length();
		int bufferSize = (int) (nextCursorPosition - currentCursorPosition);
				
		// read all records of this timestamp into buffer
		byte[] buffer = new byte[bufferSize];
		file.readFully(buffer);
		
		// convert byte array to buffered reader
		String s = new String(buffer);
		StringReader stringReader = new StringReader(s);
		BufferedReader reader = new BufferedReader(stringReader);
		
		// read records
		ArrayList<Record> list = new ArrayList<Record>();
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split(",", -1);
			Record record = new Record(tokens);
			list.add(record);
		}
		
		// return records  
		return list;
		
	}
	
	public List<Record> readNext(long timestamp) throws IOException
	{
		Long nextTimestamp = nextIndex.get(timestamp);
		if (nextTimestamp == null) return null;
		else return readThis(nextTimestamp);		
	}
	
	public Record readRecord(long timestamp, int id) throws IOException
	{
		List<Record> records = readThis(timestamp);
		for (Record record : records) 
			if (record.id == id) return record;
		return null;//FIXME
//		Record tmp = null;
//		for (Record record : records) 
//		{
//			if (record.id == id) 
//			{
//				if (tmp != null) throw new IllegalStateException();
//				else tmp = record;
//			}
//		}
//		return tmp;
	}

	public List<Long> getSortedTimestamps()
	{		
		return timestamps;
	}

	public Long getTimestampRelativeTo(Long timestamp, int distance)
	{
		if (distance < 0) for (int i = distance; (i < 0) && (timestamp != null); i++) timestamp = previousIndex.get(timestamp);
		else if (distance > 0) for (int i = 0; (i < distance) && (timestamp != null); i++) timestamp = nextIndex.get(timestamp);
		else if (cursorIndex.containsKey(timestamp) == false) timestamp = null;
		return timestamp;
	}
	
	public void close() throws IOException
	{
		file.close();
	}
		
}
