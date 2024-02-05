/*
 * Copyright (C) 2016, 2024 University of Illinois Board of Trustees.
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

package edu.illinois.gernat.btools.behavior.trophallaxis.deploy;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import edu.illinois.gernat.btools.behavior.trophallaxis.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.PairProcessor;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.TrophallaxisProcessor;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.image.MyLookUpOp;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.roi.TrophaROI;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.io.record.IndexedReader;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.common.parameters.Tuple;
import edu.illinois.gernat.btools.tracking.bcode.MetaCode;

/**
 * Created by tobias on 10.12.16.
 */
public class TrophallaxisDetector {
	
	private static final String THIRD_PARTY_LICENSES_FILE = "trophallaxis_detector_3rd_party_licenses.txt";
	
	public static boolean isContact(Coordinate headCenter1, Coordinate headCenter2, Vector orientation1, Vector orientation2, int minDistance, int maxDistance, double maxAngleSum)
	{

		// return true iff the distance between head center is within the
		// specified range and the sum of the angles between a line connecting
		// the head centers and the specified orientation vectors is smaller
		// than the specified threshold
		float distance = headCenter1.distanceTo(headCenter2);
		if ((distance < minDistance) || (distance > maxDistance)) return false;
		float angle1 = Math.abs(new Vector(headCenter1, headCenter2).angleBetween(orientation1));
		if (angle1 > maxAngleSum) return false;
		float angle2 = Math.abs(new Vector(headCenter2, headCenter1).angleBetween(orientation2));
		if (angle1 + angle2 > maxAngleSum) return false;
		return true;
		
	}

	public static List<Contact> predictContacts(List<Record> records, int distanceLabelHead, int geometryMinDistance, int geometryMaxDistance, double geometryMaxAngleSum)
	{
		
		// create contact for each pair of bees that meets the specified 
		// requirements  
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		int recordCount = records.size();
		for (int i = 0; i < recordCount; i++)
		{
			Record record1 = records.get(i);
			Coordinate headCenter1 = record1.orientation.clone().scale(distanceLabelHead).terminal(record1.center);
			for (int j = i + 1; j < recordCount; j++)
			{
				Record record2 = records.get(j);
				Coordinate headCenter2 = record2.orientation.clone().scale(distanceLabelHead).terminal(record2.center);
				if (isContact(headCenter1, headCenter2, record1.orientation, record2.orientation, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum)) contacts.add(new Contact(record1.timestamp, record1.id, record2.id));
			}
		}
		return contacts;

	}

	private static void showVersionAndCopyright() 
	{
		System.out.println("Trophallaxis Detector (bTools) 0.17.0");
		System.out.println("Copyright (C) 2017-2024 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar trophallaxis_detector.jar PARAMETER=VALUE...");
		System.out.println("Detect trophllaxis between honey bees.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- distance.label.head     average distance between the center of a bee's bCode");
		System.out.println("                          bCode and the center of her head");
		System.out.println("- filtered.data.file      file containing the bCode detection results for the");
		System.out.println("                          file named by the image.filename parameter. Must be");
		System.out.println("                          sorted by timestamp column");
		System.out.println("- geometry.max.angle.sum  maximum sum of the angles between a line drawn");
		System.out.println("                          between the geometrically predicted centers of the");
		System.out.println("                          head of two potential interaction partners and the");
		System.out.println("                          orientation vector of their labels");
		System.out.println("- geometry.max.distance   maximum distance between the geometrically predicted");
		System.out.println("                          centers of the head of two potential interaction");
		System.out.println("                          partners");
		System.out.println("- geometry.min.distance   minimum distance between the geometrically predicted");
		System.out.println("                          centers of the head of two potential interaction");
		System.out.println("                          partners");
		System.out.println("- input.file              the input image or plain text file");
		System.out.println("- show.credits            set to \"true\" or 1 to display credits and exit");
		System.out.println();
		System.out.println("Notes:");
		System.out.println("If the input.file is a plain text file, this file must list one image or");
		System.out.println("video file per line.");
		System.out.println("");
		System.out.println("Input image filenames need to be a valid date in the format");
		System.out.println("yyyy-MM-dd-HH-mm-ss-SSS followed by a dot and the filename extension (e.g.,");
		System.out.println("2013-07-18-13-57-25-600.jpg)");
		System.out.println();
		System.out.println("Trophallaxis output filenames are constructed by replacing the input image");
		System.out.println("filename extension with 'txt'.");
	}
	
	private static void showCredits() throws IOException 
	{
		showVersionAndCopyright();
		System.out.println();
		System.out.println("This software uses the following third party libraries that are distributed");
		System.out.println("under their own terms:");
		System.out.println();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(THIRD_PARTY_LICENSES_FILE); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		while (reader.ready()) System.out.println(reader.readLine());
		reader.close();
		inputStream.close();
		System.exit(1);
	}
	
	private static NeuralNetwork createCNN(String filename) throws IOException 
	{
		
		// extract CNN model
		InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		File target = File.createTempFile("trophallaxis_detector", ".pb");
		target.deleteOnExit();
		Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // return CNN
        return(new NeuralNetwork(target.getPath(), 96, 160));
        
	}
	
	private static void processInputFiles(HashMap<String, String> ioMap, int distanceLabelHead, int geometryMinDistance, int geometryMaxDistance, double geometryMaxAngleSum, String bCodeDetectionPath) throws IOException, ParseException
	{
		
        // create CNNs
        NeuralNetwork occurenceDetector = createCNN("trophallaxis_occurrence_model.pb");
        NeuralNetwork directionDetector = createCNN("trophallaxis_direction_model.pb");

        // open bCode reader 
        IndexedReader indexedReader = new IndexedReader(bCodeDetectionPath);

        // create an image processor for extracting the image region between
        // the heads of potential trophallaxis partners from a hive image
        PairProcessor roiExtractor = new TrophallaxisProcessor(null, null);
        roiExtractor.setRoiCalculator(new TrophaROI(96, 160));
        roiExtractor.addManipulator(new MyLookUpOp((short) 200));
		
		// iterate over input files
		for (String inputFilename : ioMap.keySet())
		{

			// delete output file, if it exists
			String outputFilename = ioMap.get(inputFilename);
			File outputFile = new File(outputFilename);
			Files.deleteIfExists(outputFile.toPath());
			
			// detect trophallaxis in input file
			List<Detection> trophallaxisDetectionResults = null;
			try
			{
				long timestamp = Images.getTimestampFromFilename(inputFilename);
				List<Record> bCodeDetections = indexedReader.readThis(timestamp);
				trophallaxisDetectionResults = processImage(inputFilename, timestamp, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum, bCodeDetections, roiExtractor, occurenceDetector, directionDetector);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println("Caused by file: " + inputFilename);
				continue;
			}
			
			// write trophallaxis detections to file 
			TokenWriter writer = new TokenWriter(outputFilename);
			for (Detection detection : trophallaxisDetectionResults) writer.writeTokens(detection); 
			writer.close();
			
		}	
		
		// close bCode reader 
        indexedReader.close();
		
	}
	
	private static LabeledBee createLabeledBee(int ID, List<Record> bCodeDetections)
	{
		
		// find record of specified bee
		Record bee = null;
		for (Record record : bCodeDetections) 
		{
			if (record.id == ID)
			{
				bee = record;
				break;
			}
		}

		// get corners of the bee's barcode
        MetaCode metaID = MetaCode.createFrom(bee);
        float[] corners = metaID.calculateBoundingBoxCoordinates();
        for (int i = 1; i < corners.length; i = i + 2) corners[i] = -corners[i];
        
        // return new labeled bee
        return new LabeledBee(bee.id, bee.center.x, -bee.center.y, bee.orientation.dx, -bee.orientation.dy, 0, corners);
        
	}
	
	private static List<Detection> processImage(String inputFilename, long timestamp, int distanceLabelHead, int geometryMinDistance, int geometryMaxDistance, double geometryMaxAngleSum, List<Record> bCodeDetections, PairProcessor roiExtractor, NeuralNetwork trophaPredictor, NeuralNetwork directionPredictor) throws IOException, ParseException
	{
		
        // read input image
        BufferedImage greatImage = null;
        greatImage = ImageIO.read(new File(inputFilename));
        
        // convert input image to grayscale, if necessary
        if (greatImage.getType() != BufferedImage.TYPE_BYTE_GRAY) 
        {
            BufferedImage image = new BufferedImage(greatImage.getWidth(), greatImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = image.getGraphics();
            g.drawImage(greatImage, 0, 0, null);
            g.dispose();
            greatImage = image;
        }
        
        // generate candidate trophallaxis detections based on the bees' 
        // position an orientation relative to each other; return empty 
        // list if there are none
        LinkedList<Detection> trophallaxisDetections = new LinkedList<Detection>();
        List<Contact> contacts = predictContacts(bCodeDetections, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum);
        if (contacts.isEmpty()) return trophallaxisDetections;
        
        // for each candidate detection, extract rectangular image region 
        // showing the heads the two bees and space between the heads 
        BufferedImage[] rois = new BufferedImage[contacts.size()];
        for (int i = 0; i < contacts.size(); ++i) 
        {
            Contact bees = contacts.get(i);
            LabeledBee curBee1 = createLabeledBee(bees.id1, bCodeDetections);
            LabeledBee curBee2 = createLabeledBee(bees.id2, bCodeDetections);
            rois[i] = roiExtractor.processSingle(greatImage, Tuple.of(curBee1, curBee2));
        }

        // for all ROIs, obtain probability of trophallaxis and the  
        // probability that bee 1 is the donor  
        float[] occurenceProbabilities = trophaPredictor.predict(rois); 
        float[] bee1DonorProbabilities = directionPredictor.predict(rois); 

        // return trophallaxis detections
        for (int i = 0; i < contacts.size(); i++) trophallaxisDetections.add(new Detection(timestamp, contacts.get(i).id1, contacts.get(i).id2, occurenceProbabilities[i], bee1DonorProbabilities[i]));
        return trophallaxisDetections;

	}
	
	private static HashMap<String, String> mapInputToOutput(String inputFilename) throws IOException
	{
		HashMap<String, String> ioMap = new HashMap<>(); 
		if (!inputFilename.endsWith(".txt")) queueInputFile(ioMap, inputFilename);
		else
		{
			BufferedReader reader = new BufferedReader(new FileReader(inputFilename));
			while (reader.ready()) queueInputFile(ioMap, reader.readLine());
			reader.close();	
		}
		return ioMap;
	}

	private static void queueInputFile(HashMap<String, String> ioMap, String inputFilename)
	{
		if (inputFilename.endsWith(".jpg") || inputFilename.endsWith(".png")) ioMap.put(inputFilename, inputFilename.substring(0, inputFilename.lastIndexOf(".")) + ".txt");
		else throw new IllegalStateException("Trophallaxis detector: unsupported input file extension");
	}
	
    public static void main(String[] args) throws IOException, ParseException 
    {
    	
		// show version, copyright, and usage information if no arguments were 
		// given on the command line 
		if (args.length == 0) 
		{
			showVersionAndCopyright();
			System.out.println();
			showUsageInformation();		
			System.exit(1);
		}
		
        // parse command line arguments
        Parameters parameters = Parameters.INSTANCE;
        parameters.initialize(args);
        
        // show credits, if requested
        if ((parameters.exists("show.credits")) && (parameters.getBoolean("show.credits"))) showCredits();

        // set arguments
        int distanceLabelHead = parameters.getInteger("distance.label.head"); //60
        double geometryMaxAngleSum = Math.toRadians(parameters.getInteger("geometry.max.angle.sum")); // 540 // HACK
        int geometryMaxDistance = parameters.getInteger("geometry.max.distance"); // 140
        int geometryMinDistance = parameters.getInteger("geometry.min.distance"); // 0
        String bCodeDetectionPath = parameters.getString("filtered.data.file");
        String inputFile = parameters.getString("input.file");
        
		// map input files to output files
		HashMap<String, String> ioMap = mapInputToOutput(inputFile);

		// process input files
        processInputFiles(ioMap, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum, bCodeDetectionPath);

    }

}
