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
import java.util.List;

import edu.illinois.gernat.btools.common.io.token.TokenWriter;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class RecordWriter
{

	private TokenWriter writer;
	
	public RecordWriter(String filename) throws IOException
	{
		writer = new TokenWriter(filename, ",");
	}

	public RecordWriter(File file) throws IOException
	{
		this(file.getAbsolutePath());
	}
	
	public void writeRecord(Record record) throws IOException
	{
		writer.writeTokens(record.toTokens());
	}
	
	public void writeRecords(List<Record> records) throws IOException
	{
		for (Record record : records) writer.writeTokens(record.toTokens());
	}
		
	public void close() throws IOException
	{
		writer.close();
	}
	
}
