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

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import edu.illinois.gernat.btools.common.geometry.Angles;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.image.Pixel;
import edu.illinois.gernat.btools.common.parameters.Parameters;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class HeadDetector
{
	
	private static final boolean DEBUG_MODE = false;
	
	private static final int ROI_SIDE_LENGTH = 150; //pixels

	public static final int THRESHOLD_RADIUS = 2; //pixels
	
	public static Rectangle getAxisAlignedROIBounds(BufferedImage image, Vector labelOrientation, Coordinate labelCenter)
	{
		
		// calculate bounding box coordinates
		Vector v = labelOrientation.clone();
		v.rotate(Angles.HALF_PI).scale(ROI_SIDE_LENGTH / 2);
		Coordinate c1 = v.terminal(labelCenter);
		Coordinate c2 = v.initial(labelCenter);
		v.rotate(Angles.MINUS_HALF_PI).setLength(ROI_SIDE_LENGTH);
		Coordinate c3 = v.terminal(c1);
		Coordinate c4 = v.terminal(c2);
		
		// calculate axis aligned ROI coordinates, cropped to be   
		// inside image bounds
		int x1 = (int) Math.max(Math.min(c1.x, Math.min(c2.x, Math.min(c3.x, c4.x))), 0);
		int y1 = (int) Math.max(Math.min(c1.y, Math.min(c2.y, Math.min(c3.y, c4.y))), 0);
		int x2 = (int) Math.min(Math.max(c1.x, Math.max(c2.x, Math.max(c3.x, c4.x))), image.getWidth() - 1);
		int y2 = (int) Math.min(Math.max(c1.y, Math.max(c2.y, Math.max(c3.y, c4.y))), image.getHeight() - 1);

		// return axis aligned ROI
		return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
		
	}
	
	public static ByteProcessor createGrayProcessor(BufferedImage image, Rectangle roiBounds)
	{
		return new ByteProcessor(Images.getIndependentSubimage(image, roiBounds.x, roiBounds.y, roiBounds.width, roiBounds.height));				
	}
	
	public static ByteProcessor createBinaryProcessor(BufferedImage image, Rectangle roiBounds)
	{

		// level background
		ByteProcessor binaryProcessor = new ByteProcessor(Images.getIndependentSubimage(image, roiBounds.x, roiBounds.y, roiBounds.width, roiBounds.height));		
		byte[] binaryPixels = (byte[]) binaryProcessor.getPixels();
//FIXME
Parameters parameters = Parameters.INSTANCE;
int levelingThreshold = parameters.getInteger("leveling.threshold");		
		for (int i = 0; i < binaryPixels.length; i++) if ((binaryPixels[i] & 0xFF) > levelingThreshold) binaryPixels[i] = (byte) levelingThreshold;
		
		// binarize image
		ImagePlus imagePlus = new ImagePlus(null, binaryProcessor);
		Auto_Local_Threshold autoLocalThreshold = new Auto_Local_Threshold();
		autoLocalThreshold.exec(imagePlus, "MidGrey", THRESHOLD_RADIUS, 0, 0, true);
		
		// return image processor
		return binaryProcessor;
		
	}
	
	public static Gradient createGradient(byte[] pixels, int width, int height, ArrayList<Pixel> pixelsToConvolve)
	{
		
		// limit max gray value 
		pixels = pixels.clone();
//FIXME
Parameters parameters = Parameters.INSTANCE;
int levelingThreshold = parameters.getInteger("leveling.threshold");			
		for (int i = 0; i < pixels.length; i++) if ((pixels[i] & 0xFF) > levelingThreshold) pixels[i] = (byte) levelingThreshold;
		
		// create arrays for x and y gradient
		float[] array1 = new float[pixels.length];
		for (int i = 0; i < pixels.length; i++) array1[i] = pixels[i] & 0xFF;
		float[] array2 = array1.clone();
		
        // convolve image with Sobel operators
        Gradient.convolveFloat(array1, width, height, Gradient.kernelX, 5, 5, pixelsToConvolve);  
        Gradient.convolveFloat(array2, width, height, Gradient.kernelY, 5, 5, pixelsToConvolve);
        
        // calculate gradient magnitude and direction
        float maxMagnitude = Float.MIN_VALUE;        
        if (pixelsToConvolve == null) for (int i = 0; i < array1.length; i++) maxMagnitude = calculatePixelGradient(array1, array2, i, maxMagnitude);
        else for (Pixel pixel : pixelsToConvolve) maxMagnitude = calculatePixelGradient(array1, array2, pixel.y * width + pixel.x, maxMagnitude);
        
        // normalize gradient magnitude
        for (int i = 0; i < array1.length; i++) array1[i] /= maxMagnitude;

        // return gradient
        return new Gradient(width, height, array1, array2);
        
	}

	private static final float calculatePixelGradient(float[] array1, float[] array2, int i, float maxMagnitude)
	{
    	float dx = array1[i];
    	float dy = array2[i];
    	float magnitude = (float) Math.sqrt(dx * dx + dy * dy);
    	array1[i] = magnitude;    	
    	if (dy == 0) array2[i] = Angles.HALF_PI;
    	else array2[i] = (float) Math.atan(dx / dy);
    	if (magnitude > maxMagnitude) return magnitude;
    	else return maxMagnitude;
	}

	public static Head detect(BufferedImage image, Coordinate center, Vector orientation) 
	{
			
		// create image processor for ROI
		Rectangle roiBounds = getAxisAlignedROIBounds(image, orientation, center);
		ByteProcessor grayProcessor = createGrayProcessor(image, roiBounds);
		
		//calculate image gradient
		Gradient gradient = createGradient((byte[]) grayProcessor.getPixels(), roiBounds.width, roiBounds.height, null);
		
		// translate label center to ROI coordinate system
		Coordinate labelCenter = center.clone();
		labelCenter.translate(-roiBounds.x, -roiBounds.y);
		
		// find head
		Optimizer optimizer = new Optimizer(labelCenter, orientation, grayProcessor, gradient, Optimizer.PSO_TYPE_GLOBAL);
		optimizer.optimize();
		Head head = optimizer.getBestHead();
		head.translate(roiBounds.x, roiBounds.y);
	
		// create debug image
		if (DEBUG_MODE)
		{
			
			// draw gray image and binary image
			BufferedImage debugImage = new BufferedImage(roiBounds.width * 2, roiBounds.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D debugGraphics = debugImage.createGraphics();
			debugGraphics.drawImage(grayProcessor.getBufferedImage(), 0, 0, null);
			debugGraphics.drawImage(gradient.toColorProcessor().getBufferedImage(), roiBounds.width, 0, null);

			// draw head outline
			debugGraphics.setColor(Color.RED);
			AffineTransform transform = AffineTransform.getTranslateInstance(-roiBounds.x, -roiBounds.y);
			debugGraphics.draw(transform.createTransformedShape(head.area));
			transform.translate(roiBounds.width, 0);
			debugGraphics.draw(transform.createTransformedShape(head.area));
			
			// set debug image
			head.debugImage = debugImage;

		}
												
		// done
		return head;

	}
	
}
