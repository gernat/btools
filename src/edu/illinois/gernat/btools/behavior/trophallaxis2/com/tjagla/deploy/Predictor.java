package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.deploy;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacv.FrameGrabber.Exception;

import edu.illinois.gernat.btools.behavior.trophallaxis.Contact;
import edu.illinois.gernat.btools.behavior.trophallaxis.TrophallaxisDetector;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.PairProcessor;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.TrophallaxisProcessor;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.image.MyLookUpOp;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi.TrophaROI;
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
public class Predictor {
	
	private static final String THIRD_PARTY_LICENSES_FILE = "trophallaxis_detector_3rd_party_licenses.txt";
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("Trophallaxis Detector (bTools) 0.16.0");
		System.out.println("Copyright (C) 2017-2023 University of Illinois Board of Trustees");
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
		System.out.println("- image.list.file         plain text file listing on each line one input image");
		System.out.println("                          file.");
		System.out.println("- show.credits            set to \"true\" or 1 to display credits and exit");
		System.out.println();
		System.out.println("Notes:");
		System.out.println("Input image filenames need to be a valid date in the format");
		System.out.println("yyyy-MM-dd-HH-mm-ss-SSS.");
		System.out.println();
		System.out.println("Parameters image.filename and trophallaxis.file cannot be specified in");
		System.out.println("conjunction with the image.list.file parameter.");
		System.out.println();
		System.out.println("If the image.list.file parameter is given, trophallaxis output file names are");
		System.out.println("constructed by replacing the input image file extension with 'txt'. Input");
		System.out.println("image file name and extension must be separated by a dot.");
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
		URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        File file = File.createTempFile("trophallaxis_detector", ".pb");
        file.deleteOnExit();
        FileUtils.copyURLToFile(url, file);
        
        // return CNN
        return(new NeuralNetwork(file.getPath(), 96, 160));
        
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
        List<Contact> contacts = TrophallaxisDetector.predictContacts(bCodeDetections, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum);
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
        String imagesFile = parameters.getString("image.list.file");
        
		// map input files to output files
		HashMap<String, String> ioMap = mapInputToOutput(imagesFile);

		// process input files
        processInputFiles(ioMap, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum, bCodeDetectionPath);

    }

}
