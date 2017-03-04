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

package edu.illinois.gernat.btools.behavior.trophallaxis.head;

import java.util.ArrayList;

import edu.illinois.gernat.btools.common.geometry.Angles;
import edu.illinois.gernat.btools.common.image.Pixel;
import ij.process.ColorProcessor;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Gradient
{

	// 5x5 horizontal Sobel kernel 
    public static final float[] kernelX = new float[] 
    { 
    	2, 1, 0, -1, -2,
        3, 2, 0, -2, -3,
        4, 3, 0, -3, -4,
        3, 2, 0, -2, -3,
        2, 1, 0, -1, -2      
    }; 
    
    // 5x5 vertical Sobel kernel
    public static final float[] kernelY = new float[] 
    {
    	 2,  3,  4,  3,  2,
         1,  2,  3,  2,  1,
         0,  0,  0,  0,  0,
        -1, -2, -3, -2, -1,
        -2, -3, -4, -3, -2     
    };

	public int width;

	public int height;

	public float[] magnitude;
	
	public float[] angle;

	// http://stackoverflow.com/questions/9567882/sobel-filter-kernel-of-large-size
	// http://fiji.sc/Directionality
	public Gradient(int width, int height, float[] magnitude, float[] angle)
	{
		this.width = width;
		this.height = height;
		this.magnitude = magnitude;
		this.angle = angle;
	}
	
	public ColorProcessor toColorProcessor()
	{
		int gradientSize = width * height;
        byte[] magnitude = new byte[gradientSize];
        for (int i = 0; i < gradientSize; i++) magnitude[i] = (byte) (this.magnitude[i] * 255); 
        byte[] angle = new byte[gradientSize]; 
        for (int i = 0; i < gradientSize; i++) angle[i] = (byte) ((this.angle[i] + Angles.HALF_PI) / Angles.PI * 255); 
		ColorProcessor gradientProcessor = new ColorProcessor(width, height);
		gradientProcessor.setHSB(angle, magnitude, magnitude);
        return gradientProcessor;
	}

	// taken from ImageJ Convolver.getScale()
	public static double getScale(float[] kernel) 
	{
		double scale = 1.0;
		double sum = 0.0;
		for (int i = 0; i < kernel.length; i++) sum += kernel[i];
		if (sum != 0) scale = 1.0 / sum;
		return scale;
	}

	// adapted from ImageJ Convolver.convolveFloat()
	public static void convolveFloat(float[] pixels, int width, int height, float[] kernel, int kernelH, int kernelW, ArrayList<Pixel> pixelsToConvolve) 
	{
		int uc = kernelH / 2;    
		int vc = kernelW / 2;
		int xedge = width - uc;
		int yedge = height - vc;
		float[] pixels2 = new float[pixels.length];
		System.arraycopy(pixels, 0, pixels2, 0, pixels.length);
		double scale = getScale(kernel); 
		if (pixelsToConvolve != null) for (Pixel pixel : pixelsToConvolve) convolvePixel(pixels, pixels2, width, height, pixel.x, pixel.y, xedge, yedge, kernel, uc, vc, scale);
		else for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) convolvePixel(pixels, pixels2, width, height, x, y, xedge, yedge, kernel, uc, vc, scale);
   	}

	// adapted from ImageJ Convolver.convolveFloat()
	private static final void convolvePixel(float[] pixels, float[] pixels2, int width, int height, int x, int y, int xedge, int yedge, float[] kernel, int uc, int vc, double scale)
	{
		double sum = 0.0;
		int i = 0;
		boolean edgePixel = (y < vc) || (y >= yedge) || (x < uc) || (x >= xedge);
		for (int v = -vc; v <= vc; v++) 
		{
			int offset = x + (y + v) * width;
			for (int u = -uc; u <= uc; u++) 
			{
				if (edgePixel) 
				{
					int pX = x + u;
					int pY = y + v;
					if (pX < 0) pX = 0;
					if (pX >= width) pX = width - 1;
					if (pY < 0) pY = 0;
					if (pY >= height) pY = height - 1;
					sum += pixels2[pX + pY * width] * kernel[i++];
				}
				else sum += pixels2[offset + u] * kernel[i++];
			}
    	}
		pixels[x + y * width] = (float) (sum * scale);
	}
	
}
