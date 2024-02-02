package edu.illinois.gernat.btools.behavior.egglaying.deploy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

import edu.illinois.gernat.btools.behavior.egglaying.io.Bee;
import edu.illinois.gernat.btools.behavior.egglaying.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.egglaying.processing.MyProcessor;
import edu.illinois.gernat.btools.behavior.egglaying.processing.Processor;
import edu.illinois.gernat.btools.behavior.egglaying.processing.roi.DiagonalBee;
import edu.illinois.gernat.btools.behavior.egglaying.processing.roi.LowerEdgeROI;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.image.MyLookUpOp;
import edu.illinois.gernat.btools.common.io.record.IndexedReader;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.tracking.bcode.MetaCode;

/**
 * Created by tobias on 10.12.16.
 */
public class Predictor {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

    public static void main(String[] args) throws IOException {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        // parse command line arguments
        Parameters parameters = Parameters.INSTANCE;
        parameters.initialize(args);

        File s1Folder = Files.createTempDirectory("s1_01").toFile();
        s1Folder.deleteOnExit();
        URL s1ckptJAR = Predictor.class.getResource("/egg-laying_abdomen_model.ckpt");
        File s1ckpt = File.createTempFile("s1model",".ckpt",s1Folder);
        s1ckpt.deleteOnExit();
        FileUtils.copyURLToFile(s1ckptJAR,s1ckpt);

        URL s1modelJAR = Predictor.class.getResource("/egg-laying_abdomen_model.proto");
        File s1model = File.createTempFile("s1model",".proto",s1Folder);
        s1model.deleteOnExit();
        FileUtils.copyURLToFile(s1modelJAR,s1model);

        File s2Folder = Files.createTempDirectory("s2_02").toFile();
        s2Folder.deleteOnExit();
        URL s2ckptJAR = Predictor.class.getResource("/egg-laying_whole-bee_model.ckpt");
        File s2ckpt = File.createTempFile("s2model",".ckpt",s2Folder);
        s2ckpt.deleteOnExit();
        FileUtils.copyURLToFile(s2ckptJAR,s2ckpt);

        URL s2modelJAR = Predictor.class.getResource("/egg-laying_whole-bee_model.proto");
        File s2model = File.createTempFile("s2model",".proto",s2Folder);
        s2model.deleteOnExit();
        FileUtils.copyURLToFile(s2modelJAR,s2model);

        String bCodeDetectionPath = parameters.getString("filtered.data.file");
        String imagesFile = parameters.getString("image.list.file");
        String outPutFileEnding = "txt";

        // check image source
        String[] imagesList;
        if (imagesFile.endsWith(".txt")) {
            imagesList = parseFile(imagesFile);
        } else {
            imagesList = new String[]{imagesFile};
        }

        NeuralNetwork net1 = new NeuralNetwork(s1model.getPath(), s1ckpt.getPath(), 130, 130);
        NeuralNetwork net2 = new NeuralNetwork(s2model.getPath(), s2ckpt.getPath(), 256, 256);

        IndexedReader records = new IndexedReader(bCodeDetectionPath);

        Processor processor1 = new MyProcessor(null, null);

        processor1.setRoiCalculator(new LowerEdgeROI(10, 130, 130));
        processor1.addManipulator(new MyLookUpOp((short) 200));

        Processor processor2 = new MyProcessor(null, null);

        processor2.setRoiCalculator(new DiagonalBee(0));
        processor2.addManipulator(new MyLookUpOp((short) 200));

        // iterate throug all hive images
        for (String imagePath : imagesList) {
            BufferedImage greatImage = null;
            try {
                greatImage = ImageIO.read(new File(imagePath));
            } catch (IOException e) {
                System.out.println(imagePath);
                e.printStackTrace();
                continue;
            }
            String imageName = imagePath.substring(0, imagePath.lastIndexOf("."));
            Files.deleteIfExists(new File(imageName + "." + outPutFileEnding).toPath());
            long timestamp = 0;
            try {
                timestamp = DATE_FORMAT.parse(imageName.substring(imageName.lastIndexOf(File.separator) + 1)).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            List<Record> imageBees = records.readThis(timestamp);

            // check if records available
            if (imageBees == null || imageBees.isEmpty()) {
                Files.createFile(new File(imageName + "." + outPutFileEnding).toPath());
                continue;
            }
            BufferedImage[] smallImages1 = new BufferedImage[imageBees.size()];
            BufferedImage[] smallImages2 = new BufferedImage[imageBees.size()];
            // iterate over all detected bCods in the hive image
            for (int i = 0; i < imageBees.size(); ++i) {
                Record imageBee = imageBees.get(i);
                MetaCode mID = null;
                try {
                    mID = MetaCode.createFrom(imageBee);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    System.out.println(imageName);
                    System.out.println(imageBee.timestamp + " " + imageBee.id + " " + imageBee.center);
                    continue;
                }
                float[] corners = mID.calculateBoundingBoxCoordinates();
                switchCoordinates(corners); // convert image coordinates to world coordinates
                Bee curBee = new LabeledBee(imageBee.id, imageBee.center.x, -imageBee.center.y, imageBee.orientation.dx, -imageBee.orientation.dy, 0, corners);
                smallImages1[i] = processor1.processSingle(greatImage, curBee);
                smallImages2[i] = processor2.processSingle(greatImage, curBee);
            }

            float[] res1 = net1.predict(smallImages1); // predict small images with the neural network
            float[] res2 = net2.predict(smallImages2); // predict small images with the neural network

            PrintWriter outputWriter = new PrintWriter(new File(imageName + "." + outPutFileEnding));
            // write output
            for (int i = 0; i < res1.length; i++) {
                outputWriter.println(timestamp + "," + imageBees.get(i).id + "," + res1[i] + "," + res2[i]);
            }
            outputWriter.close();
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
