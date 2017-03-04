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

package edu.illinois.gernat.btools.behavior.trophallaxis.touch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import edu.illinois.gernat.btools.behavior.trophallaxis.head.Head;
import edu.illinois.gernat.btools.common.geometry.Angles;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.image.Pixel;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileInfo;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import optinav.Clean_Up_Local_Thickness;
import optinav.Distance_Ridge;
import optinav.EDT_S1D;
import optinav.Local_Thickness_Parallel;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class TouchDetector
{

	private static final boolean DEBUG_MODE = false;
	
	private static final float ANTENNAL_FOV_CENTRAL_ANGLE = Angles.PI;
	
	private static final int PROBOSCIS_LENGTH = 140; //pixels
		
	public static Pixel[] lastSourceHeadPixels;
	
	public static Pixel[] lastTargetHeadPixels;
	
	public static Pixel lastTargetHeadCenter;
	
	public static ByteProcessor lastROIProcessor;
	
	public static float[] lastLocalThickness;
	
	private static Area getAntennalFieldOfView(Head head, int radius, float centralAngle)
	{
		int diameter = radius * 2;
		float startAngle = (float) -Math.toDegrees(Vector.I.angleBetween(head.orientation.clone().rotate(Angles.HALF_PI)));
		centralAngle = (float) Math.toDegrees(centralAngle);
		Arc2D antennalFOV = new Arc2D.Float(head.center.x - radius, head.center.y - radius, diameter, diameter, startAngle, centralAngle, Arc2D.PIE);
		return new Area(antennalFOV);
	}
	
	public static Touch detect(BufferedImage image, int id1, int id2, Map<Integer, Head> heads) 
	{
		
		// skip if no head was detected for one of the potential
		// interaction partners 
		if ((!heads.containsKey(id1)) || (!heads.containsKey(id2))) return new Touch(-1, id1, id2, null, null, false, -1, -1, new ArrayList<Integer>(0), null); 
		Head head1 = heads.get(id1);
		Head head2 = heads.get(id2);
		
		// skip if head areas overlap
		Area head1Area = new Area(head1.area);
		Area head2Area = new Area(head2.area);
		Area headOverlap = new Area(head1Area);
		headOverlap.intersect(head2Area);
		if (!headOverlap.isEmpty()) return new Touch(-1, id1, id2, head1, head2, false, -1, -1, new ArrayList<Integer>(0), null);
		
		// skip if the areas defined by the heads and antennal fields of view 
		// don't overlap inside the image
		Area antennalField1 = getAntennalFieldOfView(head1, PROBOSCIS_LENGTH, ANTENNAL_FOV_CENTRAL_ANGLE);
		Area antennalField2 = getAntennalFieldOfView(head2, PROBOSCIS_LENGTH, ANTENNAL_FOV_CENTRAL_ANGLE);
		Area bee1 = new Area(antennalField1);
		bee1.add(head1Area);
		Area bee2 = new Area(antennalField2);
		bee2.add(head2Area);
		Area interactionArea = new Area(bee1);
		interactionArea.intersect(bee2);
		int imageW = image.getWidth();
		int imageH = image.getHeight();
		Area imageArea = new Area(new Rectangle(0, 0, imageW, imageH));
		interactionArea.intersect(imageArea);
		if (interactionArea.isEmpty()) return new Touch(-1, id1, id2, head1, head2, false, -1, -1, new ArrayList<Integer>(0), null);
		
		// calculate ROI boundaries, adjusted for possible rounding errors,
		// and create ROI
		Area roiArea = new Area(bee1);
		roiArea.add(bee2);
		roiArea.intersect(imageArea);
		Rectangle roiBounds = roiArea.getBounds();
		int roiXOffset = roiBounds.x - Math.max(roiBounds.x - 1, 0);
		int roiYOffset = roiBounds.y - Math.max(roiBounds.y - 1, 0);
		int roiX = roiBounds.x - roiXOffset;
		int roiY = roiBounds.y - roiYOffset;
		int roiW = Math.min(roiX + roiBounds.width + roiXOffset + 1, imageW - 1) - roiX + 1; 
		int roiH = Math.min(roiY + roiBounds.height + roiYOffset + 1, imageH - 1) - roiY + 1; 
		roiBounds = null;
		BufferedImage roi = Images.getIndependentSubimage(image, roiX, roiY, roiW, roiH);
		Graphics2D roiGraphics = roi.createGraphics();
		
		// create debug image
		int debugIndex = 0;
		BufferedImage debugImage = null;
		Graphics2D debugGraphics = null;
		if (DEBUG_MODE)
		{
			debugImage = new BufferedImage(roiW * 5, roiH, BufferedImage.TYPE_3BYTE_BGR);
			debugGraphics = debugImage.createGraphics();	
		}
		
		// background leveling: replace all colors above white 
		// threshold with white threshold
		ImagePlus roiPlus = new ImagePlus(null, roi);
		ByteProcessor roiProcessor = (ByteProcessor) roiPlus.getProcessor();
		lastROIProcessor = roiProcessor;
		byte[] roiPixels = (byte[]) roiProcessor.getPixels();
//FIXME
Parameters parameters = Parameters.INSTANCE;
int levelingThreshold = parameters.getInteger("leveling.threshold");
		for (int i = 0; i < roiPixels.length; i++) if ((roiPixels[i] & 0xFF) > levelingThreshold) roiPixels[i] = (byte) levelingThreshold;
		if (DEBUG_MODE) debugGraphics.drawImage(roi, roiW * debugIndex++, 0, null);
		
		// threshold ROI
//FIXME
String method = parameters.getString("thresholding.method");
Integer radius = parameters.getInteger("thresholding.radius");
Integer p1 = parameters.getInteger("contrast.threshold");
Prefs.setThreads(1);
Auto_Local_Threshold autoLocalThreshold = new Auto_Local_Threshold();
autoLocalThreshold.exec(roiPlus, method, radius, p1, 0, true);

		// close 1 pixel holes by setting white pixels to black if all 
		// 4-connected neighbors are black; speeds up subsequent image 
		// processing
		for (int i = 0; i < roiPixels.length; i++)
		{
			if ((roiPixels[i] != 0) 
			&& ((i - roiW < 0) || (roiPixels[i - roiW] == 0)) 
			&& ((i + roiW >= roiPixels.length) || (roiPixels[i + roiW] == 0))
			&& (((i + 1) / roiW != i / roiW) || (roiPixels[i + 1] == 0))
			&& ((i == 0) || ((i - 1) / roiW != i / roiW) || (roiPixels[i - 1] == 0))) roiPixels[i] = 0;
		}
		
		// reduce detail
		Prefs.setThreads(1);
		RankFilters rankFilters = new RankFilters();
		rankFilters.rank(roiProcessor, 0.5, RankFilters.MEDIAN);

		// close 1 pixel holes by setting white pixels to black if all 
		// 4-connected neighbors are black; speeds up subsequent image 
		// processing
		for (int i = 0; i < roiPixels.length; i++)
		{
			if ((roiPixels[i] != 0) 
			&& ((i - roiW < 0) || (roiPixels[i - roiW] == 0)) 
			&& ((i + roiW >= roiPixels.length) || (roiPixels[i + roiW] == 0))
			&& (((i + 1) / roiW != i / roiW) || (roiPixels[i + 1] == 0))
			&& ((i == 0) || ((i - 1) / roiW != i / roiW) || (roiPixels[i - 1] == 0))) roiPixels[i] = 0;
		}

		// compute local thickness of foreground pixels
		ImageProcessor thicknessProcessor = roiProcessor.duplicate();
		EDT_S1D edtS1D = new EDT_S1D(thicknessProcessor, 127, true);
		edtS1D.run(thicknessProcessor);
		Distance_Ridge distanceRidge = new Distance_Ridge(edtS1D.impOut);
		distanceRidge.run(edtS1D.impOut.getProcessor());
		Local_Thickness_Parallel localThicknessParallel = new Local_Thickness_Parallel(distanceRidge.impOut);
		localThicknessParallel.run(distanceRidge.impOut.getProcessor()); 
		Clean_Up_Local_Thickness cleanUpLocalThickness = new Clean_Up_Local_Thickness(distanceRidge.impOut);
		cleanUpLocalThickness.run(distanceRidge.impOut.getProcessor());
		float[] localThickness = (float[]) cleanUpLocalThickness.impOut.getProcessor().getPixels();
		lastLocalThickness = localThickness;

		// remove image content outside the area where bees can contact
		// each other
		AffineTransform transform = AffineTransform.getTranslateInstance(-roiX, -roiY);
		antennalField1.transform(transform);
		antennalField2.transform(transform);
		Area contactArea = new Area(antennalField1);
		contactArea.add(antennalField2);
		Area outsideArea = new Area(new Rectangle(0, 0, roiW, roiH)); 
		outsideArea.subtract(contactArea);
		roiGraphics.setColor(Color.WHITE);
		roiGraphics.fill(outsideArea);
		if (DEBUG_MODE) debugGraphics.drawImage(roi, debugIndex++ * roiW, 0, null);			
		
		// skeletonize image
		roiProcessor.skeletonize();
		
		// simplify skeleton by setting all black pixels which have more 
		// than 2 4-connected neighbors to white
		for (int i = 0; i < roiPixels.length; i++)
		{
			if (roiPixels[i] != 0) continue;
			int neighborCount = 0;
			if ((i - roiW >= 0) && (roiPixels[i - roiW] == 0)) neighborCount++;
			if ((i + roiW < roiPixels.length) && (roiPixels[i + roiW] == 0)) neighborCount++;
			if (((i + 1) / roiW == i / roiW) && (roiPixels[i + 1] == 0)) neighborCount++;
			if ((i > 0) && ((i - 1) / roiW == i / roiW) && (roiPixels[i - 1] == 0)) neighborCount++;
			if (neighborCount > 2) roiPixels[i] = (byte) 255;
		}
		
		// draw skeleton onto median-filtered image using colors derived
		// from thickness calculation to draw the skeleton 
		if (DEBUG_MODE) 
		{

			// create LUT
			FileInfo fileInfo = new FileInfo();
			fileInfo.reds = new byte[256]; 
			fileInfo.greens = new byte[256]; 
			fileInfo.blues = new byte[256];
			fileInfo.lutSize = 256;
			int colorCount = fire(fileInfo.reds, fileInfo.greens, fileInfo.blues);
			if ((colorCount>0) && (colorCount<256)) interpolate(fileInfo.reds, fileInfo.greens, fileInfo.blues, colorCount);

			// apply LUT
			ImageProcessor ip = cleanUpLocalThickness.impOut.getChannelProcessor();
			IndexColorModel cm = new IndexColorModel(8, 256, fileInfo.reds, fileInfo.greens, fileInfo.blues);
			ip.setColorModel(cm);		
			
			// draw skeleton
			BufferedImage bi = cleanUpLocalThickness.impOut.getProcessor().getBufferedImage();
			int xOffset = debugIndex * roiW;
			for (int i = 0; i < roiPixels.length; i++) 
			{
				if (roiPixels[i] != 0) continue;
				int x = i % roiW;
				int y = i / roiW;
				debugImage.setRGB(xOffset + x, y, bi.getRGB(x, y));
			}				
			debugIndex++;
			
		}			
					
		// try to trace path from border of head 1 to border of head 2
		head1Area.transform(transform);
		head2Area.transform(transform);
		roiGraphics.fill(head1Area);
		roiGraphics.fill(head2Area);
		Pixel[] source = Images.getBorderPixels(head1.eyesAndFrontBoundary, -roiX, -roiY, roiW, roiH);
		lastSourceHeadPixels = source;
		Pixel[] target = Images.getBorderPixels(head2.eyesAndFrontBoundary, -roiX, -roiY, roiW, roiH);
		lastTargetHeadPixels = target;
		HashSet<Pixel> visitedPixels = new HashSet<>(); 
		Pixel targetCenter = new Pixel(head2.center);
		lastTargetHeadCenter = targetCenter;
		targetCenter.x -= roiX;
		targetCenter.y -= roiY;
		Walk walk = null;
//FIXME
Integer maxThickSegmentLength = parameters.getInteger("max.thick.segment.length");
Integer maxPathThickness = parameters.getInteger("max.path.thickness");
		if ((source.length > 0) && (target.length > 0)) walk = Walk.computeOneWalkBetween(source, target, targetCenter, roiProcessor, localThickness, maxPathThickness, maxThickSegmentLength, visitedPixels);
		
		if (DEBUG_MODE) 
		{
			
			// draw heads and walks over skeleton background
			int xOffset = roiW * debugIndex;
			debugGraphics.drawImage(roi, roiW * debugIndex, 0, null);
			debugGraphics.setColor(Color.MAGENTA);
			for (Pixel pixel : source) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			for (Pixel pixel : target) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			debugGraphics.setColor(Color.BLUE);
			for (Pixel pixel : visitedPixels) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			if (walk != null)
			{
				debugGraphics.setColor(Color.GREEN);
				for (Pixel pixel : walk.pixels) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			}
			debugIndex++;
			
			// draw heads and walks over image background
			xOffset = roiW * debugIndex;
			debugGraphics.drawImage(Images.getIndependentSubimage(image, roiX, roiY, roiW, roiH), roiW * debugIndex, 0, null);
			debugGraphics.setColor(Color.MAGENTA);
			for (Pixel pixel : source) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			for (Pixel pixel : target) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			debugGraphics.setColor(Color.BLUE);
			for (Pixel pixel : visitedPixels) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			if (walk != null)
			{
				debugGraphics.setColor(Color.GREEN);
				for (Pixel pixel : walk.pixels) debugGraphics.drawLine(xOffset + pixel.x, pixel.y, xOffset + pixel.x, pixel.y);
			}
			debugIndex++;
		}
		
		// return interaction
		if (walk != null) return new Touch(-1, id1, id2, head1, head2, true, walk.length, walk.longestThinSegmentLength, walk.pathThickness, debugImage);
		else return new Touch(-1, id1, id2, head1, head2, false, -1, -1, new ArrayList<Integer>(0), debugImage);
		
	}

	// taken from ImageJ:LutLoader
	private static int fire(byte[] reds, byte[] greens, byte[] blues) {
		int[] r = {0,0,1,25,49,73,98,122,146,162,173,184,195,207,217,229,240,252,255,255,255,255,255,255,255,255,255,255,255,255,255,255};
		int[] g = {0,0,0,0,0,0,0,0,0,0,0,0,0,14,35,57,79,101,117,133,147,161,175,190,205,219,234,248,255,255,255,255};
		int[] b = {0,61,96,130,165,192,220,227,210,181,151,122,93,64,35,5,0,0,0,0,0,0,0,0,0,0,0,35,98,160,223,255};
		for (int i=0; i<r.length; i++) {
			reds[i] = (byte)r[i];
			greens[i] = (byte)g[i];
			blues[i] = (byte)b[i];
		}
		return r.length;
	}

	// taken from ImageJ:LutLoader
	private static void interpolate(byte[] reds, byte[] greens, byte[] blues, int nColors) {
		byte[] r = new byte[nColors]; 
		byte[] g = new byte[nColors]; 
		byte[] b = new byte[nColors];
		System.arraycopy(reds, 0, r, 0, nColors);
		System.arraycopy(greens, 0, g, 0, nColors);
		System.arraycopy(blues, 0, b, 0, nColors);
		double scale = nColors/256.0;
		int i1, i2;
		double fraction;
		for (int i=0; i<256; i++) {
			i1 = (int)(i*scale);
			i2 = i1+1;
			if (i2==nColors) i2 = nColors-1;
			fraction = i*scale - i1;
			//IJ.write(i+" "+i1+" "+i2+" "+fraction);
			reds[i] = (byte)((1.0-fraction)*(r[i1]&255) + fraction*(r[i2]&255));
			greens[i] = (byte)((1.0-fraction)*(g[i1]&255) + fraction*(g[i2]&255));
			blues[i] = (byte)((1.0-fraction)*(b[i1]&255) + fraction*(b[i2]&255));
		}
	}

}
