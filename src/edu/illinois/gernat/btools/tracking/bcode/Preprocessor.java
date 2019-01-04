/*
 * Copyright (C) 2018 University of Illinois Board of Trustees.
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

import java.awt.image.BufferedImage;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.filter.UnsharpMask;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * @version 0.13.1
 * @since 0.13.1
 * @author Tim Gernat
 */
public class Preprocessor 
{

	public static double sharpeningSigma = 0; // gaussian blur kernel radius ~= sigma * 3 + 1
	
	public static double sharpeningAmount = 0;
	
	public static float scalingFactor = 1;

	public static BufferedImage preprocess(BufferedImage image)
	{

		// does image need to be preprocessed?
		if ((scalingFactor != 1) || (sharpeningAmount != 0))
		{
			
			// set up an ImageJ image processor
			Prefs.setThreads(1);
			ImagePlus imagePlus = new ImagePlus(null, image);			
			FloatProcessor floatProcessor = (FloatProcessor) imagePlus.getProcessor().convertToFloat();
			
			// scale image
			if (scalingFactor != 1)
			{
				floatProcessor.setInterpolationMethod(ImageProcessor.BILINEAR);
				floatProcessor = (FloatProcessor) floatProcessor.resize((int) (floatProcessor.getWidth() * scalingFactor));
				if (sharpeningAmount == 0) image = floatProcessor.getBufferedImage();
			}
			
			// sharpen image
			if (sharpeningAmount != 0)
			{
				floatProcessor.snapshot();			
				UnsharpMask unsharpMask = new UnsharpMask();
				unsharpMask.sharpenFloat(floatProcessor, sharpeningSigma, (float) sharpeningAmount);
				image = floatProcessor.getBufferedImage();
			}
			
		}
		
		// done
		return image;

	}
	
}
