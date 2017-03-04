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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public final class Reader
{

	public static int minBlackThreshold = 10;
	
	public static int maxBlackThreshold = 250;

	public static int thresholdStepSize = 5;
	
	private Reader()
	{
	}

	public static List<MetaCode> read(BufferedImage image)
	{
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		List<BCode> beeIDs = new LinkedList<BCode>();
		Binarizer binarizer = new Binarizer(source);
		for (int i = maxBlackThreshold; i >= minBlackThreshold; i -= thresholdStepSize)
		{
			binarizer.binarize(i);
			beeIDs.addAll(Detector.detect(binarizer.getMatrix(), binarizer.getIndex()));
		}
		Iterator<BCode> iterator = beeIDs.iterator();
		while (iterator.hasNext()) if (!iterator.next().hasGoodTemplate) iterator.remove();
		return Consolidator.consolidate(beeIDs);		
	}
	
	public static List<MetaCode> read(String filename) throws IOException, NotFoundException
	{
		BufferedImage image = ImageIO.read(new File(filename));
		if (image == null) 
		{
			System.err.println("image processor: cannot read image file '" + filename + "'.");
			return new LinkedList<MetaCode>();
		}
        return read(image);
	}
	
}
