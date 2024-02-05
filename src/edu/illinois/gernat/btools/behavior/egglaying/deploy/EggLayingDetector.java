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

package edu.illinois.gernat.btools.behavior.egglaying.deploy;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import edu.illinois.gernat.btools.behavior.egglaying.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.egglaying.processing.MyProcessor;
import edu.illinois.gernat.btools.behavior.egglaying.processing.Processor;
import edu.illinois.gernat.btools.behavior.egglaying.processing.roi.DiagonalBee;
import edu.illinois.gernat.btools.behavior.egglaying.processing.roi.LowerEdgeROI;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.image.MyLookUpOp;
import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.io.record.IndexedReader;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.tracking.bcode.MetaCode;

/**
 * Created by tobias on 10.12.16.
 */
public class EggLayingDetector 
{
		
	private static final String THIRD_PARTY_LICENSES_FILE = "worker_egg-laying_detector_3rd_party_licenses.txt";
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("Worker egg-laying Detector (bTools) 0.18.0");
		System.out.println("Copyright (C) 2017-2024 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar worker_egg-laying_detector.jar PARAMETER=VALUE...");
		System.out.println("Detect worker egg-laying.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- filtered.data.file file containing the bCode detection results for the file(s)");
		System.out.println("                     named by the input.file parameter. Must be sorted by");
		System.out.println("                     timestamp column");
		System.out.println("- input.file         the input image or plain text file");
		System.out.println("- show.credits       set to \"true\" or 1 to display credits and exit");
		System.out.println();
		System.out.println("Notes:");
		System.out.println("If the input.file is a plain text file, this file must list one image or");
		System.out.println("video file per line.");
		System.out.println("");
		System.out.println("Input image filenames need to be a valid date in the format");
		System.out.println("yyyy-MM-dd-HH-mm-ss-SSS followed by a dot and the filename extension (e.g.,");
		System.out.println("2013-07-18-13-57-25-600.jpg)");
		System.out.println();
		System.out.println("Egg-laying output filenames are constructed by replacing the input image");
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
		else throw new IllegalStateException("Worker egg-laying detector: unsupported input file extension");
	}

	private static void processInputFiles(HashMap<String, String> ioMap, String bCodeDetectionPath) throws IOException, ParseException
	{
		
		// extract CNN models
        File s1Folder = Files.createTempDirectory("s1_01").toFile();
        s1Folder.deleteOnExit();
        InputStream s1ckptJAR = Thread.currentThread().getContextClassLoader().getResourceAsStream("egg-laying_abdomen_model.ckpt");
        File s1ckpt = File.createTempFile("s1model",".ckpt",s1Folder);
        s1ckpt.deleteOnExit();
        Files.copy(s1ckptJAR, s1ckpt.toPath(), StandardCopyOption.REPLACE_EXISTING);

        InputStream s1modelJAR = Thread.currentThread().getContextClassLoader().getResourceAsStream("egg-laying_abdomen_model.proto");
        File s1model = File.createTempFile("s1model",".proto",s1Folder);
        s1model.deleteOnExit();
        Files.copy(s1modelJAR, s1model.toPath(), StandardCopyOption.REPLACE_EXISTING);

        File s2Folder = Files.createTempDirectory("s2_02").toFile();
        s2Folder.deleteOnExit();
        InputStream s2ckptJAR = Thread.currentThread().getContextClassLoader().getResourceAsStream("egg-laying_whole-bee_model.ckpt");
        File s2ckpt = File.createTempFile("s2model",".ckpt",s2Folder);
        s2ckpt.deleteOnExit();
        Files.copy(s2ckptJAR, s2ckpt.toPath(), StandardCopyOption.REPLACE_EXISTING);

        InputStream s2modelJAR = Thread.currentThread().getContextClassLoader().getResourceAsStream("egg-laying_whole-bee_model.proto");
        File s2model = File.createTempFile("s2model",".proto",s2Folder);
        s2model.deleteOnExit();
        Files.copy(s2modelJAR, s2model.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
        // create CNNs
        NeuralNetwork abdomenCNN = new NeuralNetwork(s1model.getPath(), s1ckpt.getPath(), 130, 130);
        NeuralNetwork wholeBeeCNN = new NeuralNetwork(s2model.getPath(), s2ckpt.getPath(), 256, 256);

        // open bCode reader 
        IndexedReader indexedReader = new IndexedReader(bCodeDetectionPath);

        // create an image processor for extracting the image region showing
        // a bees abdomen
        Processor abdomenROIExtractor = new MyProcessor(null, null);
        abdomenROIExtractor.setRoiCalculator(new LowerEdgeROI(10, 130, 130));
        abdomenROIExtractor.addManipulator(new MyLookUpOp((short) 200));

        // create an image processor for extracting the image region showing
        // an entire bee on the ROI's diagonal
        Processor wholeBeeROIExtractor = new MyProcessor(null, null);
        wholeBeeROIExtractor.setRoiCalculator(new DiagonalBee(0));
        wholeBeeROIExtractor.addManipulator(new MyLookUpOp((short) 200));
		
		// iterate over input files
		for (String inputFilename : ioMap.keySet())
		{

			// delete output file, if it exists
			String outputFilename = ioMap.get(inputFilename);
			File outputFile = new File(outputFilename);
			Files.deleteIfExists(outputFile.toPath());
			
			// detect egg-laying in input file
			List<Detection> trophallaxisDetectionResults = null;
			try
			{
				long timestamp = Images.getTimestampFromFilename(inputFilename);
				List<Record> bCodeDetections = indexedReader.readThis(timestamp);
				trophallaxisDetectionResults = processImage(inputFilename, timestamp, bCodeDetections, abdomenROIExtractor, wholeBeeROIExtractor, abdomenCNN, wholeBeeCNN);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println("Caused by file: " + inputFilename);
				continue;
			}
			
			// write egg-laying detections to file 
			TokenWriter writer = new TokenWriter(outputFilename);
			for (Detection detection : trophallaxisDetectionResults) writer.writeTokens(detection); 
			writer.close();
			
		}	
		
		// close bCode reader 
        indexedReader.close();
		
	}

    private static List<Detection> processImage(String inputFilename, long timestamp, List<Record> bCodeDetections, Processor abdomenROIExtractor, Processor wholeBeeROIExtractor, NeuralNetwork abdomenCNN, NeuralNetwork wholeBeeCNN) throws IOException 
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
    	
        // extract ROIs
        int beeCount = bCodeDetections.size();
        BufferedImage[] abdomenROIs = new BufferedImage[beeCount];
        BufferedImage[] wholeBeeROIs = new BufferedImage[beeCount];
        for (int i = 0; i < beeCount; ++i) 
        {
        	
        	// get bee information
        	Record imageBee = bCodeDetections.get(i);
        	MetaCode mID = MetaCode.createFrom(imageBee);
        	
        	// obtain corners of her barcode
        	float[] corners = mID.calculateBoundingBoxCoordinates();
        	for (int j = 1; j < corners.length; j = j + 2) corners[j] = -corners[j];
        	
        	// extract ROIs
        	LabeledBee curBee = new LabeledBee(imageBee.id, imageBee.center.x, -imageBee.center.y, imageBee.orientation.dx, -imageBee.orientation.dy, 0, corners);
        	abdomenROIs[i] = abdomenROIExtractor.processSingle(greatImage, curBee);
        	wholeBeeROIs[i] = wholeBeeROIExtractor.processSingle(greatImage, curBee);
        	
        }

        // obtain probability that a bee is an egg-layer and that this 
        // probability is a true positive
        float[] egglayingProbabilities = abdomenCNN.predict(abdomenROIs); 
        float[] isTruePositiveProbabilities = wholeBeeCNN.predict(wholeBeeROIs);

        // return egg-laying detections
        LinkedList<Detection> egglayingDetections = new LinkedList<Detection>();
        for (int i = 0; i < beeCount; i++) egglayingDetections.add(new Detection(timestamp, bCodeDetections.get(i).id, egglayingProbabilities[i], isTruePositiveProbabilities[i]));
        return egglayingDetections;

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
        String filteredDataFile = parameters.getString("filtered.data.file");
        String inputFile = parameters.getString("input.file");
        
        // map input files to output files
        HashMap<String, String> ioMap = mapInputToOutput(inputFile);

        // process input files
        processInputFiles(ioMap, filteredDataFile);

    }
	
}
