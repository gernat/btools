package com.tjagla.deploy;

import com.tjagla.io.Bee;
import com.tjagla.io.LabeledBee;
import com.tjagla.processing.MyProcessor;
import com.tjagla.processing.Processor;
import com.tjagla.processing.image.MyLookUpOp;
import com.tjagla.processing.roi.DiagonalBee;
import com.tjagla.processing.roi.LowerEdgeROI;

import org.apache.commons.io.FileUtils;

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

import name.timgernat.common.Parameters;
import name.timgernat.common.io.record.IndexedReader;
import name.timgernat.common.io.record.Record;
import name.timgernat.image.matrixcode.MetaID;

/**
 * Created by tobias on 10.12.16.
 */
public class Predictor {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

    public static void main(String[] args) throws IOException {
//        long startTime = System.currentTimeMillis();
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        // parse command line arguments
        Parameters parameters = Parameters.INSTANCE;
        parameters.initialize(args);

        File s1Folder = Files.createTempDirectory("s1_01").toFile();
        s1Folder.deleteOnExit();
        URL s1ckptJAR = Predictor.class.getResource("/s1_01/model.ckpt");
        File s1ckpt = File.createTempFile("s1model",".ckpt",s1Folder);
        s1ckpt.deleteOnExit();
        FileUtils.copyURLToFile(s1ckptJAR,s1ckpt);

        URL s1modelJAR = Predictor.class.getResource("/s1_01/trained_model.proto");
        File s1model = File.createTempFile("s1model",".proto",s1Folder);
        s1model.deleteOnExit();
        FileUtils.copyURLToFile(s1modelJAR,s1model);
//        String netDefPath1st = parameters.getString("network.model.file.1");

        File s2Folder = Files.createTempDirectory("s2_02").toFile();
        s2Folder.deleteOnExit();
        URL s2ckptJAR = Predictor.class.getResource("/s2_00/model.ckpt");
        File s2ckpt = File.createTempFile("s2model",".ckpt",s2Folder);
        s2ckpt.deleteOnExit();
        FileUtils.copyURLToFile(s2ckptJAR,s2ckpt);

        URL s2modelJAR = Predictor.class.getResource("/s2_00/trained_model.proto");
        File s2model = File.createTempFile("s2model",".proto",s2Folder);
        s2model.deleteOnExit();
        FileUtils.copyURLToFile(s2modelJAR,s2model);
//        String netDefPath2nd = parameters.getString("network.model.file.2");

        // network.definition.folder=/home/tobias/data/Persoenliche_Daten/Docs/work/robionsonLab/egg_laying_worker/tensorflow/server/logs/great_nets/2016-12-12_12-29-17-0/1/
        String bCodeDetectionPath = parameters.getString("filtered.data.file");
        // bCode.detection.file=/home/tobias/data/Persoenliche_Daten/Docs/work/robionsonLab/egg_laying_worker/data/all/raw_bcode_detection_results.txt
//        String outputPath = parameters.getString("output.path");
        String imagesFile = parameters.getString("image.list.file");
        // image.file=/home/tobias/data/Persoenliche_Daten/Docs/work/robionsonLab/egg_laying_worker/data/First_training_set/2016-06-01-10-00-00-763.jpg
        String outPutFileEnding = "txt";
        // output.file.ending=n2_02

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
                MetaID mID = null;
                try {
                    mID = Record.createMetaIDFrom(imageBee);
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
//        System.out.println("duration: "+(System.currentTimeMillis()-startTime)+"ms");
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
