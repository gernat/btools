package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.deploy;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import edu.illinois.gernat.btools.behavior.trophallaxis.Contact;
import edu.illinois.gernat.btools.behavior.trophallaxis.TrophallaxisDetector;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.PairProcessor;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.TrophallaxisProcessor;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.image.MyLookUpOp;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi.TrophaROI;
import edu.illinois.gernat.btools.common.io.record.IndexedReader;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.common.parameters.Tuple;
import edu.illinois.gernat.btools.tracking.bcode.MetaCode;

/**
 * Created by tobias on 10.12.16.
 */
public class Predictor {
	
	private static final String THIRD_PARTY_LICENSES_FILE = "trophallaxis_detector_3rd_party_licenses.txt";
	
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

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
		System.out.println("- distance.label.head       average distance between the center of a bee's");
		System.out.println("                            bCode and the center of her head");
		System.out.println("- filtered.data.file        file containing the bCode detection results for");
		System.out.println("                            the file named by the image.filename parameter.");
		System.out.println("                            Must be sorted by timestamp column");
		System.out.println("- geometry.max.angle.sum    maximum sum of the angles between a line drawn");
		System.out.println("                            between the geometrically predicted centers of the");
		System.out.println("                            head of two potential interaction partners and the");
		System.out.println("                            orientation vector of their labels");
		System.out.println("- geometry.max.distance     maximum distance between the geometrically");
		System.out.println("                            predicted centers of the head of two potential");
		System.out.println("                            interaction partners");
		System.out.println("- geometry.min.distance     minimum distance between the geometrically");
		System.out.println("                            predicted centers of the head of two potential");
		System.out.println("                            interaction partners");
		System.out.println("- image.filename            input image file");
		System.out.println("- image.list.filename       plain text file listing on each line one input");
		System.out.println("                            image file.");
		System.out.println("- show.credits              set to \"true\" or 1 to display credits and exit");
		System.out.println();
		System.out.println("Notes:");
		System.out.println("Input image filenames need to be a valid date in the format");
		System.out.println("yyyy-MM-dd-HH-mm-ss-SSS.");
		System.out.println();
		System.out.println("Parameters image.filename and trophallaxis.file cannot be specified in");
		System.out.println("conjunction with the image.list.filename parameter.");
		System.out.println();
		System.out.println("If the image.list.filename parameter is given, trophallaxis output file names");
		System.out.println("are constructed by replacing the input image file extension with 'txt'. Input");
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
	
    public static void main(String[] args) throws IOException 
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
        
        // queue input files
        String[] imagesList;
        if (imagesFile.endsWith(".txt")) {
            imagesList = parseFile(imagesFile);
        } else {
            imagesList = new String[]{imagesFile};
        }  
        
        // extract CNN models
        File trFolder = Files.createTempDirectory("tropha").toFile();
        trFolder.deleteOnExit();
        URL trmodelJAR = Thread.currentThread().getContextClassLoader().getResource("trophallaxis_occurrence_model.pb");
        File trmodel = File.createTempFile("trmodel",".pb",trFolder);
        trmodel.deleteOnExit();
        FileUtils.copyURLToFile(trmodelJAR,trmodel);
        File dirFolder = Files.createTempDirectory("direction").toFile();
        dirFolder.deleteOnExit();
        URL dirmodelJAR = Thread.currentThread().getContextClassLoader().getResource("trophallaxis_direction_model.pb");
        File dirmodel = File.createTempFile("dirmodel",".pb",dirFolder);
        dirmodel.deleteOnExit();
        FileUtils.copyURLToFile(dirmodelJAR,dirmodel);

        // create CNNs
        NeuralNetwork trophaPredictor = new NeuralNetwork(trmodel.getPath(), 96, 160);
        NeuralNetwork directionPredictor = new NeuralNetwork(dirmodel.getPath(), 96, 160);



        String outPutFileEnding = "txt";
        
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        

        IndexedReader records = new IndexedReader(bCodeDetectionPath);

        PairProcessor processor = new TrophallaxisProcessor(null, null);

        processor.setRoiCalculator(new TrophaROI(96, 160));
        processor.addManipulator(new MyLookUpOp((short) 200));

        // iterate through all hive images
        for (String imagePath : imagesList) {
            String imageName = imagePath.substring(0, imagePath.lastIndexOf("."));
            try {
                Files.deleteIfExists(new File(imageName + "." + outPutFileEnding).toPath());
            } catch (Exception e) {
                System.err.println("Can't read/write from/to disk.");
                System.err.println(imagePath);
                e.printStackTrace();
                continue;
            }
            BufferedImage greatImage = null;
            try {
                greatImage = ImageIO.read(new File(imagePath));
                if (greatImage.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                    BufferedImage image = new BufferedImage(greatImage.getWidth(), greatImage.getHeight(),
                            BufferedImage.TYPE_BYTE_GRAY);
                    Graphics g = image.getGraphics();
                    g.drawImage(greatImage, 0, 0, null);
                    g.dispose();
                    greatImage = image;
                }
            } catch (IOException e) {
                System.err.println("Couldn't read image file.");
                System.err.println(imagePath);
                e.printStackTrace();
                continue;
            }
            long timestamp = 0;
            try {
                timestamp = DATE_FORMAT.parse(imageName.substring(imageName.lastIndexOf('/') + 1)).getTime();
            } catch (ParseException e) {
                System.err.println("Couldn't convert readable timestamp to unix timestamp.");
                System.err.println(imagePath);
                e.printStackTrace();
                continue;
            }

            List<Record> imageBees = records.readThis(timestamp);

            // check if records available
            if (imageBees == null || imageBees.isEmpty()) {
                new File(imageName + "." + outPutFileEnding).createNewFile();
                continue;
            }

            List<Contact> contacts = TrophallaxisDetector.predictContacts(imageBees, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum);

            // check if there are at least one pair with contact
            if (contacts.isEmpty()) {
                new File(imageName + "." + outPutFileEnding).createNewFile();
                continue;
            }
            
            BufferedImage[] smallImages = new BufferedImage[contacts.size()];
            // iterate over all detected bCods in the hive image
            for (int i = 0; i < contacts.size(); ++i) {
                Contact bees = contacts.get(i);
                Record imageBee1 = null;
                MetaCode mID1 = null;
                try {
                    imageBee1 = records.readRecord(timestamp, bees.id1);
                    mID1 = MetaCode.createFrom(imageBee1);
                } catch (NullPointerException e) {
                    System.err.println("Couldn't match record for given timestamp and id");
                    System.err.println(imagePath);
                    System.err.println(timestamp + " " + imageBee1.id + " " + imageBee1.center);
                    e.printStackTrace();
                    continue;
                }
                Record imageBee2 = null;
                MetaCode mID2 = null;
                try {
                    imageBee2 = records.readRecord(timestamp, bees.id2);
                    mID2 = MetaCode.createFrom(imageBee2);
                } catch (NullPointerException e) {
                    System.err.println("Couldn't match record for given timestamp and id");
                    System.err.println(imagePath);
                    System.err.println(timestamp + " " + imageBee2.id + " " + imageBee1.center);
                    e.printStackTrace();
                    continue;
                }
                float[] corners1 = mID1.calculateBoundingBoxCoordinates();
                float[] corners2 = mID2.calculateBoundingBoxCoordinates();
                // convert image coordinates to world coordinates
                switchCoordinates(corners1); 
                switchCoordinates(corners2); 
                LabeledBee curBee1 = new LabeledBee(imageBee1.id, imageBee1.center.x, -imageBee1.center.y, imageBee1.orientation.dx, -imageBee1.orientation.dy, 0, corners1);
                LabeledBee curBee2 = new LabeledBee(imageBee2.id, imageBee2.center.x, -imageBee2.center.y, imageBee2.orientation.dx, -imageBee2.orientation.dy, 0, corners2);
                smallImages[i] = processor.processSingle(greatImage, Tuple.of(curBee1, curBee2));
            }

            // predict small images with the neural network
            float[] res1; 
            float[] res2; 
            try {
                res1 = trophaPredictor.predict(smallImages);
                res2 = directionPredictor.predict(smallImages);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Number of images and output array length did not match.");
                System.err.println(imagePath);
                e.printStackTrace();
                continue;
            }

            // write output
            PrintWriter outputWriter = new PrintWriter(new File(imageName + ".tmp"));
            for (int i = 0; i < res1.length; i++) {
                outputWriter.println(timestamp + "," + contacts.get(i).id1 + "," + contacts.get(i).id2 + "," + res1[i] + "," + res2[i]);
            }
            outputWriter.close();

            // check output file
            BufferedReader br = new BufferedReader(new FileReader(imageName + ".tmp"));
            boolean outputWasCorrect = true;
            for (int i = 0; i < res2.length; i++) {
                if(!br.readLine().equals(timestamp + "," + contacts.get(i).id1 + "," + contacts.get(i).id2 + "," + res1[i] + "," + res2[i])) {
                    System.err.println("Error while writing the output file.");
                    System.err.println(imagePath);
                    outputWasCorrect = false;
                    break;
                }
            }
            br.close();
            if (outputWasCorrect) {
                new File(imageName + ".tmp").renameTo(new File(imageName + "." + outPutFileEnding));
            }
            else {
                Files.delete(new File(imageName + ".tmp").toPath());
            }
        }

    }

    /**
     * Reads a File with paths to images (per line) into an array.
     *
     * @param imagesFile absolute path to a file containing paths to images.
     * @return a array containing all paths in the file linewise.
     */
    private static String[] parseFile(String imagesFile) throws IOException {
        List<String> lines = new LinkedList<String>();
        BufferedReader br = new BufferedReader(new FileReader(imagesFile));
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * By definition every even index of the vector in corners have the x values ad the odds are the
     * y values. To switch the coordinates, all y values need to multiplied with -1.
     *
     * @param corners vector, that describes the boundingbox of a bCode. Can be created with the
     *                method MetaID.calculateBoundingBoxCoordinates()
     * @return the reference of the vector with the inline switched coordinates.
     */
    private static float[] switchCoordinates(float[] corners) {
        for (int i = 1; i < corners.length; i = i + 2) {
            corners[i] = -corners[i];
        }
        return corners;
    }
}
