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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Random;

import edu.illinois.gernat.btools.common.geometry.Angles;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.image.Pixel;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import ij.process.ByteProcessor;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Optimizer
{

	public static final int PSO_TYPE_GLOBAL = 0;
	
	public static final int PSO_TYPE_LOCAL = 1;
	
	public static final int PSO_TYPE_HYBRID = 2;

	public static final int MAX_ITERATIONS = 50;//private

	private static final int THORAX_RADIUS = 35;

	private static final int SWARM_SIZE = 40; 

	private static final double PSO_VELOCITY_WEIGHT = 0.85;

	private static final double PSO_LOCAL_WEIGHT = 1.2;

	private static final double PSO_GLOBAL_WEIGHT = 1.2;
	
	public static final int INDEX_OUTSIDE = 0;
	
	public static final int INDEX_BORDER = 1;
	
	public static final int INDEX_INSIDE = 2;

	public static final byte INDEX_EYE = 5;

	private static final int MIN_SWARM_SIZE = 3;
	
	private static final int HEAD_EXPANSION = 2; // pixels

	private int psoType;

	public Particle[] swarm;
	
	public Particle bestParticle;
	
	private Random randomSource;
	
	private ByteProcessor grayImageProcessor;
	
	private Coordinate labelCenter;
	
	private Vector labelOrientation;

	public double[] averageVelocity;

	private Gradient imageGradient;
	
	private int meanHeadPixelIntensity;
	
	private int swarmSize;
	
	public Optimizer(Coordinate labelCenter, Vector labelOrientation, ByteProcessor grayProcessor, Gradient gradient, int psoType)
	{
		this.labelCenter = labelCenter;
		this.labelOrientation = labelOrientation;
		this.grayImageProcessor = grayProcessor;		
		this.imageGradient = gradient;
		this.psoType = psoType;
		randomSource = new Random(0);
		averageVelocity = new double[Particle.COMPONENT_COUNT];		
		swarmSize = SWARM_SIZE;
//FIXME
Parameters parameters = Parameters.INSTANCE;
meanHeadPixelIntensity = parameters.getInteger("mean.head.pixel.intensity");			
	}
	
	public void initialize()
	{
	
		// create swarm
		if (swarmSize < MIN_SWARM_SIZE) throw new IllegalStateException();
		swarm = new Particle[swarmSize];
		for (int i = 0; i < swarmSize; i++) swarm[i] = new Particle();

		// distribute particles randomly over search space
		for (int i = 0; i < Particle.COMPONENT_COUNT; i++)
		{
			double range = Particle.maximumOffset[i] - Particle.minimumOffset[i];
			for (int j = 0; j < swarmSize; j++) swarm[j].position[i] = Particle.minimumOffset[i] + randomSource.nextDouble() * range;
		}
		
		// compute initial velocities 
		for (int i = 0; i < swarmSize; i++) 
		{
			for (int j = 0; j < Particle.COMPONENT_COUNT; j++)
			{			
				double lowerBound = Particle.minimumOffset[j] - swarm[i].position[j];
				double upperBound = Particle.maximumOffset[j] - swarm[i].position[j];
				swarm[i].velocity[j] = lowerBound + randomSource.nextDouble() * (upperBound - lowerBound);
			}
		}
		
		// evaluate each particle and initialize it's known best known position
		// to its initial position; also initialize global best particle
		for (int i = 0; i < swarmSize; i++)  
		{
			evaluateParticle(swarm[i]);
			swarm[i].bestPosition = swarm[i].position.clone();
			swarm[i].bestScore = swarm[i].score;	
			if ((bestParticle == null) || (swarm[i].score > bestParticle.score)) bestParticle = swarm[i].clone();
		}		
		
	}
	
	public double moveParticles(int iteration)
	{
	
		// update particles
		int neighborhoodSize = Math.max(Math.round((swarmSize - 1) / 2f * iteration / MAX_ITERATIONS), 1);
		double[] averageVelocity = new double[Particle.COMPONENT_COUNT];
		for (int i = 0; i < swarm.length; i++)
		{
		
			// get best known particle; this is the globally best particle if
			// this is a global-best PSO; if this is a local-best PSO, the best
			// known particle is either the left or the right neighbor of this
			// particle
			Particle particle = swarm[i];
			Particle bestKnownParticle = null;
			if (psoType == PSO_TYPE_GLOBAL) bestKnownParticle = bestParticle;
			else if (psoType == PSO_TYPE_LOCAL)
			{
				int leftIndex = i - 1 < 0 ? swarm.length - 1 : i - 1;
				int rightIndex = i + 1 == swarm.length ? 0 : i + 1;
				if (swarm[leftIndex].bestScore > swarm[rightIndex].bestScore) bestKnownParticle = swarm[leftIndex];
				else bestKnownParticle = swarm[rightIndex];
			}
			else if (psoType == PSO_TYPE_HYBRID)
			{
				for (int j = i - neighborhoodSize; j <= i + neighborhoodSize; j++)
				{
					int index = j;
					if (index < 0) index = swarmSize + j;
					else if (index >= swarmSize) index = j - swarmSize;
					if (index == i) continue;
					if ((bestKnownParticle == null) || (bestKnownParticle.score < swarm[index].score)) bestKnownParticle = swarm[index];
				}
			}
			
			// update particle position and velocity 
			float p = randomSource.nextFloat();
			float k = randomSource.nextFloat();
			for (int j = 0; j < Particle.COMPONENT_COUNT; j++) 
			{

				// calculate velocity and position element value 
				particle.velocity[j] = PSO_VELOCITY_WEIGHT * particle.velocity[j] + PSO_LOCAL_WEIGHT * p * (particle.bestPosition[j] - particle.position[j]) + PSO_GLOBAL_WEIGHT * k * (bestKnownParticle.position[j] - particle.position[j]);
				double sum = particle.position[j] + particle.velocity[j];
				particle.position[j] = sum;
				
				// if an position element is outside the search space, set it 
				// onto the search space border; also invert and reduce 
				// corresponding velocity
				if (sum < Particle.minimumOffset[j]) 
				{
					particle.position[j] = Particle.minimumOffset[j];
					particle.velocity[j] = particle.velocity[j] / -2;
				}
				else if (sum > Particle.maximumOffset[j])
				{
					particle.position[j] = Particle.maximumOffset[j];
					particle.velocity[j] = particle.velocity[j] / -2;
				}
				
				// sum up particle velocities
				if (particle.velocity[j] < 0) averageVelocity[j] -= particle.velocity[j];
				else averageVelocity[j] += particle.velocity[j];
				
			}
			
			// update local and global best position
			evaluateParticle(particle);
			if (particle.score > particle.bestScore) 
			{
				particle.bestPosition = particle.position.clone();
				particle.bestScore = particle.score;
			}
			if (particle.score > bestParticle.score) bestParticle = particle.clone(); 
			
		} 								

		// calculate average particle velocity
		for (int j = 0; j < averageVelocity.length; j++) averageVelocity[j] /= swarm.length;
		this.averageVelocity = averageVelocity;
		
		// return best score
		return bestParticle.score;
		
	}

	public double optimize()
	{
		initialize();
		for (int i = 0; i < MAX_ITERATIONS; i++) moveParticles(i);
		return bestParticle.score; 	
	}
	
	public void evaluateParticle(Particle particle) 
	{
		
		//
		byte[] grayImage = (byte[]) grayImageProcessor.getPixels();
		int imageW = grayImageProcessor.getWidth();
		int imageH = grayImageProcessor.getHeight();
		Head head = getHead(particle);
		byte[] index = head.index;
		int indexXOffset = head.indexXOffset;
		int indexYOffset = head.indexYOffset;
		int indexW = head.indexW;

		//
		int insideSum = 0;
		int insideCount = 0;
		double gradientAngleSum = 0;
		int gradientAngleCount = 0;
		double gradientMagnitudeSum = 0;
		int gradientMagnitudeCount = 0;
		for (int i = 0; i < head.index.length; i++)
		{
			int imageY = indexYOffset + i / indexW; 
			int imageX = indexXOffset + i % indexW;
			if ((imageX < 0) || (imageX >= imageW) || (imageY < 0) || (imageY >= imageH)) continue;		
			if (index[i] == Optimizer.INDEX_EYE) 
			{
				insideSum += meanHeadPixelIntensity - Math.min(Math.abs(meanHeadPixelIntensity - (grayImage[imageY * imageW + imageX] & 0xFF)), meanHeadPixelIntensity);
				insideCount++;
			}
			else if (index[i] == Optimizer.INDEX_BORDER) 
			{
				gradientMagnitudeSum += imageGradient.magnitude[imageY * imageW + imageX];
				gradientMagnitudeCount++;
				double orientationDifference = (imageGradient.angle[imageY * imageW + imageX] - head.gradient.angle[i]) / Angles.PI;
				if (orientationDifference < 0) orientationDifference *= -1;
				gradientAngleSum += 1 - orientationDifference;
				gradientAngleCount++;
			}
		}
		
		//
		particle.areaScore = ((double) insideSum) / insideCount / meanHeadPixelIntensity;
		particle.borderScore = gradientMagnitudeSum / gradientMagnitudeCount;
		particle.angleScore = gradientAngleSum / gradientAngleCount;
		particle.score = (particle.areaScore + particle.borderScore + particle.angleScore) / 3;
		
	}

	private Head getHead(Particle particle)
	{
		double labelX = particle.position[Particle.INDEX_LABEL_X_OFFSET];
		double labelY = particle.position[Particle.INDEX_LABEL_Y_OFFSET];
		double labelAngle = Vector.I.angleBetween(labelOrientation);
		double gf = Particle.referenceValues[Particle.INDEX_GF_OFFSET] + particle.position[Particle.INDEX_GF_OFFSET];
		double ce = Particle.referenceValues[Particle.INDEX_CE_OFFSET] + particle.position[Particle.INDEX_CE_OFFSET];
		double ef = Particle.referenceValues[Particle.INDEX_EF_OFFSET] + particle.position[Particle.INDEX_EF_OFFSET];
		double ak = Particle.referenceValues[Particle.INDEX_AK_OFFSET] + particle.position[Particle.INDEX_AK_OFFSET];
		double mi = Particle.referenceValues[Particle.INDEX_MI_OFFSET] + particle.position[Particle.INDEX_MI_OFFSET];
		double rotation = Particle.referenceValues[Particle.INDEX_ROTATION_OFFSET] + particle.position[Particle.INDEX_ROTATION_OFFSET];
		double eyeSkew = Particle.referenceValues[Particle.INDEX_EYE_SKEW_OFFSET] + particle.position[Particle.INDEX_EYE_SKEW_OFFSET];
		double hm = Particle.referenceValues[Particle.INDEX_CE_OFFSET] + particle.position[Particle.INDEX_CE_OFFSET] - (Particle.referenceValues[Particle.INDEX_EF_OFFSET] + particle.position[Particle.INDEX_EF_OFFSET]) + Particle.referenceValues[Particle.INDEX_HM_OFFSET] + particle.position[Particle.INDEX_HM_OFFSET];
		return createHead(labelX, labelY, labelAngle, gf, ce, ef, ak, mi, rotation, eyeSkew, hm, grayImageProcessor.getWidth(), grayImageProcessor.getHeight());
	}
	
	public Head getBestHead()
	{
		Particle particle = bestParticle.clone();
		particle.position[Particle.INDEX_CE_OFFSET] += HEAD_EXPANSION;
		particle.position[Particle.INDEX_GF_OFFSET] += HEAD_EXPANSION;
		return getHead(particle);
	}
	
	// see http://stackoverflow.com/questions/6711707/draw-a-quadratic-bezier-curve-through-three-given-points
	private static final Point2D.Double calculateBezierControlPoint(double x0, double y0, double xC, double yC, double x2, double y2, double t)
	{
		double x1 = (xC - x0 * t * t - x2 * (1 - t) * (1 - t)) / (2 * t * (1 - t));
		double y1 = (yC - y0 * t * t - y2 * (1 - t) * (1 - t)) / (2 * t * (1 - t));
		return new Point2D.Double(x1, y1);
	}
	
	// head model:
	//
	//         E
	//         ·
	// G · · · F · · · H
	// ·       ·       ·     
	// ·       ·       ·       
	// ·       ·       ·           
	// ·       ·       ·               
	// ·       ·       ·               
	// A · K · C · · · M · · I 
	// ·       ·       ·          
	// ·       ·       ·       
	// ·       ·       ·     
	// ·       ·       ·     
	// ·       ·       ·  
	// B · · · L · · · D
	//         ·
	//         J
	//
	// - GF > 0
	// - FH == CM == GF
	// - AK + KC <= GF
	// - GA == CF
	// - HM <= CF
	// - EF > 0
	// - MI > 0 
	// - the head model is 2-fold rotational symmetric around line AI
	// - head outline is formed by quadratic curves GEH, HID, DJB, and line
	//   BG (if AC == GF) or quadratic curve BAG (if AC < GF)
	// - BAG models the back of the head, HID forms the front of the head, and
	//   GEH and DJB model one eye, each
	//
	// note:
	// 
	// - during head model construction C is located at the coordinate 
	//   system origin, the line AI is parallel to the x-axis, and point H  
	//   is in the 1st quadrant
	// - parts of the final head shape may be outside the image boundaries
	private Head createHead(double labelX, double labelY, double labelAngle, double gf, double ce, double ef, double ak, double mi, double rotation, double eyeSkew, double hm, int imageWidth, int imageHeight)
	{
				
		// model head shape 
		double cf = ce - ef;
		Path2D headShape = new Path2D.Double(); 
		Point2D.Double gehControlPoint = calculateBezierControlPoint(-gf, -cf, 0, -cf - ef, gf, -hm, eyeSkew);
		QuadCurve2D.Double geh = new QuadCurve2D.Double(-gf, -cf, gehControlPoint.x, gehControlPoint.y, gf, -hm);
		headShape.append(geh, true);
		Point2D.Double hidControlPoint = calculateBezierControlPoint(gf, -hm, gf + mi, 0, gf, hm, 0.5);
		QuadCurve2D.Double hid = new QuadCurve2D.Double(gf, -hm, hidControlPoint.x, hidControlPoint.y, gf, hm);
		headShape.append(hid, true);
		Point2D.Double djbControlPoint = calculateBezierControlPoint(gf, hm, 0, cf + ef, -gf, cf, 1 - eyeSkew);
		QuadCurve2D.Double djb = new QuadCurve2D.Double(gf, hm, djbControlPoint.x, djbControlPoint.y, -gf, cf);
		headShape.append(djb, true);
		QuadCurve2D.Double bag = null;
		if (ak != 0)
		{
			Point2D.Double bagControlPoint = calculateBezierControlPoint(-gf, cf, -gf + ak, 0, -gf, -cf, 0.5);
			bag = new QuadCurve2D.Double(-gf, cf, bagControlPoint.x, bagControlPoint.y, -gf, -cf); 
			headShape.append(bag, true);
		}
		headShape.closePath();
		
		// model part of head that contains the eyes and ocelli
		Path2D eyesShape = new Path2D.Double();
		eyesShape.append(geh, false);
		Line2D.Double hc = new Line2D.Double(gf, -hm, 0, 0);
		eyesShape.append(hc, true);
		Line2D.Double cd = new Line2D.Double(0, 0, gf, hm);
		eyesShape.append(cd, true);
		eyesShape.append(djb, true);
		if (bag != null) eyesShape.append(bag, true);
		eyesShape.closePath();
		
		// position head
		Coordinate adjustedLabelCenter = labelCenter;		
		if (labelY != 0) adjustedLabelCenter = labelOrientation.clone().scale((float) labelY).terminal(adjustedLabelCenter);
		if (labelX != 0) adjustedLabelCenter = labelOrientation.clone().rotate(Angles.HALF_PI).scale((float) labelX).terminal(adjustedLabelCenter);
		Vector v = labelOrientation.clone().setLength(THORAX_RADIUS);
		Coordinate a = v.terminal(adjustedLabelCenter);
		v.setLength((float) (THORAX_RADIUS + THORAX_RADIUS / 2));
		Coordinate c = v.terminal(adjustedLabelCenter);
		AffineTransform transform = AffineTransform.getRotateInstance(rotation, a.x, a.y);
		transform.translate(c.x, c.y);
		transform.rotate(labelAngle);
		Area headArea = new Area(transform.createTransformedShape(headShape));
		
		// create path tracing the eyes and the front of the head
		Path2D eyesAndFrontBoundary = new Path2D.Double();
		eyesAndFrontBoundary.append(geh, false);
		eyesAndFrontBoundary.append(hid, true);
		eyesAndFrontBoundary.append(djb, true);
		eyesAndFrontBoundary.transform(transform);
		
		// create padded axis aligned bounding box of head
		Rectangle2D headAABB = headArea.getBounds2D();
		int aabbMinX = (int) Math.round(headAABB.getMinX() - 0.5);
		int aabbMaxX = (int) Math.round(headAABB.getMaxX() + 0.5);
		int aabbMinY = (int) Math.round(headAABB.getMinY() - 0.5);
		int aabbMaxY = (int) Math.round(headAABB.getMaxY() + 0.5);
		
		// create an index which can be used to look up if a pixel inside the 
		// AABB is outside, on the border, or inside the head shape
		int indexW = aabbMaxX - aabbMinX + 1;
		int indexH = aabbMaxY - aabbMinY + 1;
		BufferedImage indexImage = new BufferedImage(indexW, indexH, BufferedImage.TYPE_BYTE_GRAY);
		transform.preConcatenate(AffineTransform.getTranslateInstance(-aabbMinX, -aabbMinY));
		Graphics2D indexGraphics = indexImage.createGraphics();
		indexGraphics.setColor(new Color(INDEX_INSIDE, INDEX_INSIDE, INDEX_INSIDE)); 
		indexGraphics.fill(transform.createTransformedShape(headShape));
		indexGraphics.setColor(new Color(INDEX_EYE, INDEX_EYE, INDEX_EYE)); 
		Shape transformedEyesShape = transform.createTransformedShape(eyesShape);
		indexGraphics.fill(transformedEyesShape);
		indexGraphics.setColor(new Color(INDEX_BORDER, INDEX_BORDER, INDEX_BORDER));
		indexGraphics.draw(transform.createTransformedShape(geh));
		indexGraphics.draw(transform.createTransformedShape(djb));
		byte[] index = ((DataBufferByte) indexImage.getRaster().getDataBuffer()).getData();
		
		// determine border pixels
		ArrayList<Pixel> eyeBoundaryPixels = new ArrayList<Pixel>();
		for (int i = 0; i < index.length; i++) if (index[i] == INDEX_BORDER) eyeBoundaryPixels.add(new Pixel(i % indexW, i / indexW));

		// compute gradient of eye boundary pixels
		BufferedImage gradientImage = new BufferedImage(indexW, indexH, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D gradientGraphics = gradientImage.createGraphics();
		gradientGraphics.setColor(Color.WHITE); 
		gradientGraphics.fill(transformedEyesShape);
		Gradient headGradient = HeadDetector.createGradient(((DataBufferByte) gradientImage.getRaster().getDataBuffer()).getData(), indexW, indexH, eyeBoundaryPixels);
		
		// set head geometry properties
		int headWidth = (int) ((ef + cf) * 2);
		int headThickness = (int) (2 * gf - ak + mi); 
		double eyeWidth = 2 * gf;
		Vector headOrientation = new Vector(labelAngle + rotation);
		Point2D.Float p = new Point2D.Float(c.x, c.y);
		AffineTransform.getRotateInstance(rotation, a.x, a.y).transform(p, p);
		Coordinate headCenter = new Coordinate(p.x, p.y);
				
		// return head 
		return new Head(headArea, headCenter, headOrientation, eyesAndFrontBoundary, headWidth, headThickness, eyeWidth, index, aabbMinX, aabbMinY, indexW, indexH, headGradient, null);

	}

}
