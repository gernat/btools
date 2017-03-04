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

package edu.illinois.gernat.btools.tracking.bcode;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Index
{

	private static final int LEFT = 0;
	
	private static final int RIGHT = 1;
	
	private int[][] index;
	
	private int top;
	
	private int bottom;
	
	public Index(int width, int height)
	{
		index = new int[height][2];
		for (int i = 0; i < height; i++) index[i][RIGHT] = width - 1;
		top = 0;
		bottom = height - 1;
	}

	public int getTop()
	{
		return top;
	}

	public void setTop(int value)
	{
		top = value;
	}

	public int getBottom()
	{
		return bottom;
	}

	public void setBottom(int value)
	{
		bottom = value;
	}

	public int getLeft(int row)
	{
		return index[row][LEFT];
	}

	public int setLeft(int row, int value)
	{
		return index[row][LEFT] = value;
	}

	public int getRight(int row)
	{
		return index[row][RIGHT];
	}

	public int setRight(int row, int value)
	{
		return index[row][RIGHT] = value;
	}
	
}
