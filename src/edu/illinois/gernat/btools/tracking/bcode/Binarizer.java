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

import com.google.zxing.LuminanceSource;
import com.google.zxing.common.BitMatrix;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Binarizer 
{

	private byte[] luminances;
	
	private int width;
	
	private int height;
	
	private BitMatrix matrix;
	
	private Index index;
	
	private int threshold;
	
	public Binarizer(LuminanceSource source)
	{
		luminances = source.getMatrix();
		width = source.getWidth();
		height = source.getHeight();
		matrix = new BitMatrix(width, height);
		matrix.setRegion(0, 0, width, height);
		index = new Index(width, height);
		threshold = Integer.MAX_VALUE;
	}
	
	public BitMatrix getMatrix()
	{
		return matrix;
	}

	public Index getIndex()
	{
		return index;
	}
	
	public void binarize(int threshold)
	{
		if (this.threshold < threshold) throw new IllegalStateException();
		int t = -1;
		int b = -1;
		int bottom = index.getBottom();
	    for (int y = index.getTop(); y <= bottom; y++) 
	    {	    	
	    	int l = -1;
	    	int r = -1;
	    	int right = index.getRight(y);
	    	int offset = y * width;	    	
	    	for (int x = index.getLeft(y); x <= right; x++) 
    		{
	    		if ((luminances[offset + x] & 0xff) < threshold) 
    			{
	    			if (l == -1) l = x;
	    			r = x;
    			}
	    		else 
    			{
//	    			if (matrix.get(x, y)) matrix.flip(x, y);
	    		    int index = y * matrix.rowSize + (x >> 5);	    		    
	    		    if (((matrix.bits[index] >>> (x & 0x1f)) & 1) != 0) matrix.bits[index] &= ~(1 << (x & 0x1f));
    			}
    		}
	    	if ((l == -1) && (b == -1))
	    	{
		    	index.setLeft(y, 1);
		    	index.setRight(y, 0);
	    	}
	    	else
	    	{
		    	index.setLeft(y, l);
		    	index.setRight(y, r);
    			if (t == -1) t = y;
    			b = y;	    		

	    	}
	    }
	    if ((t == -1) && (b == -1)) 
    	{
	    	index.setTop(1);
	    	index.setBottom(0);
    	}
	    else 
	    {
	    	index.setTop(t);
	    	index.setBottom(b);
	    }
		this.threshold = threshold;
	}

}
