/*
 * Copyright (C) 2017, 2018, 2019, 2020, 2021, 2022, 2023 University of  
 * Illinois Board of Trustees.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.google.zxing.NotFoundException;

import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.record.RecordWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;

public class BCodeDetector
{
	
	private static LinkedHashMap<Long, List<MetaCode>> processImage(String inputFilename) throws IOException, ParseException
	{
		BufferedImage image = ImageIO.read(new File(inputFilename));
		List<MetaCode> bCodes = detectBCodesIn(image);
		long timestamp = Images.getTimestampFromFilename(inputFilename);
		LinkedHashMap<Long, List<MetaCode>> result = new LinkedHashMap<>();
		result.put(timestamp, bCodes);
		return result;
	}

	private static LinkedHashMap<Long, List<MetaCode>> processVideo(String inputFilename, int frameRate) throws ParseException, IOException
	{	
		
		// check parameters
		if (frameRate == -1) throw new IllegalStateException();
		
		// initialize frame grabber and converter
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFilename);
		grabber.setVideoOption("threads", "1");		
		grabber.start();
		Java2DFrameConverter converter = new Java2DFrameConverter();
		
		// initialize bCode detection loop
		LinkedHashMap<Long, List<MetaCode>> result = new LinkedHashMap<>();
		long timestamp = Images.getTimestampFromFilename(inputFilename);
		int frameNumber = 0;
		Frame frame = grabber.grab();				

		// loop over and process video frames
		while (frame != null)
		{
			
			// convert frame
			BufferedImage image = converter.convert(frame);
			
			// detect bCodes
			List<MetaCode> bCodes = detectBCodesIn(image);
			result.put(timestamp + Math.round(frameNumber * 1000d / frameRate), bCodes);
			
			// load next frame
			frame = grabber.grab();
			frameNumber++;
	
		}
		
		// dispose of frame grabber
		grabber.stop();
	    grabber.close();
		grabber.release();
		
		// done
		return result;
		
	}
	
	private static List<MetaCode> detectBCodesIn(BufferedImage image)
	{

		// preprocess image
		image = Preprocessor.preprocess(image);
		
		// detect IDs
		List<MetaCode> metaIDs = Reader.read(image);		
		
		// postprocess bCode detections
		if (Preprocessor.scalingFactor != 1)
		{
			for (MetaCode metaID : metaIDs) 
			{				
				metaID.center.set(metaID.center.x / Preprocessor.scalingFactor, metaID.center.y / Preprocessor.scalingFactor);
				metaID.moduleSize /= Preprocessor.scalingFactor;
				metaID.nw.set(metaID.nw.x / Preprocessor.scalingFactor, metaID.nw.y / Preprocessor.scalingFactor);
				metaID.ne.set(metaID.ne.x / Preprocessor.scalingFactor, metaID.ne.y / Preprocessor.scalingFactor);
				metaID.sw.set(metaID.sw.x / Preprocessor.scalingFactor, metaID.sw.y / Preprocessor.scalingFactor);
				metaID.se.set(metaID.se.x / Preprocessor.scalingFactor, metaID.se.y / Preprocessor.scalingFactor);
			}
		}
		
		// done
		return metaIDs;
		
	}

	private static void showVersionAndCopyright() 
	{
		System.out.println("bCode Detector (bTools) 0.16.0");
		System.out.println("Copyright (C) 2017-2023 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar bcode_detector.jar PARAMETER=VALUE...");
		System.out.println("Detect bCodes in images or videos.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- conserve.margin           whether the bCode border is considered to be part");
		System.out.println("                            of the bCode template");
		System.out.println("- frame.rate           	    frame rate of any videos to be processed");
		System.out.println("- scaling.factor            factor for image scaling prior to detecting bCodes");
		System.out.println("- input.file                the input image, video, or plain text file");
		System.out.println("- intensity.step.size       increment when going from the lowest to the highest");
		System.out.println("                            intensity threshold");
		System.out.println("- max.intensity.threshold   highest intensity threshold for converting to a");
		System.out.println("                            binary image");
		System.out.println("- min.intensity.threshold   lowest intensity threshold for converting to a"); 
		System.out.println("                            binary image");
		System.out.println("- min.template.conservation fraction of bCode modules that need to match the");
		System.out.println("                            bCode template");
		System.out.println("- sharpening.amount         amount of sharpening during unsharp masking");
		System.out.println("- sharpening.sigma          Gaussian blur standard deviation for unsharp");		
		System.out.println("                            masking");		
		System.out.println("- show.credits              set to \"true\" or 1 to display credits and exit");
		System.out.println();
		System.out.println("Notes:");
		System.out.println("If the input.file is a plain text file, this file must list one image or.");
		System.out.println("video file per line");
		System.out.println("");
		System.out.println("Image and video file names need to be a valid date in the format");
		System.out.println("yyyy-MM-dd-HH-mm-ss-SSS. File name and extension must be separated by a dot.");
		System.out.println("Output file names are constructed by replacing the input image file");
		System.out.println("extension with 'txt'.");
		System.out.println();
		System.out.println("When processing videos, a constant frame rate if assumed when calculating");
		System.out.println("timestamps from the date encoded by the video file name and the frame number.");
	}
	 
	private static void showCredits() throws IOException 
	{
		showVersionAndCopyright();
		System.out.println();
		System.out.println("This software uses the following third party software that is distributed");
		System.out.println("under its own terms:");
		System.out.println();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("LICENSE-3RD-PARTY"); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		while (reader.ready()) System.out.println(reader.readLine());
		reader.close();
		inputStream.close();		
	}

	public static void main(String[] args) throws NotFoundException, IOException, ParseException
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

		// get arguments
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		
		// if requested, show credits and exit 
		if ((parameters.exists("show.credits")) && (parameters.getBoolean("show.credits"))) 
		{
			showCredits();
			System.exit(1);
		}

		// set image processing parameters
		Preprocessor.sharpeningSigma = parameters.getDouble("sharpening.sigma"); 
		Preprocessor.sharpeningAmount = parameters.getDouble("sharpening.amount");
		Preprocessor.scalingFactor = (float) parameters.getDouble("scaling.factor");
		Reader.minBlackThreshold = parameters.getInteger("min.intensity.threshold");
		Reader.maxBlackThreshold = parameters.getInteger("max.intensity.threshold");
		Reader.thresholdStepSize = parameters.getInteger("intensity.step.size");
		Detector.minTemplateConservation = parameters.getDouble("min.template.conservation");
		Detector.checkMargin = parameters.getBoolean("conserve.margin");
		
		// map input files to output files
		HashMap<String, String> ioMap = mapInputToOutput(parameters.getString("input.file"));

		// set frame rate parameter, if necessary
		int frameRate = -1;
		for (String inputFilename : ioMap.keySet()) 
		{
			if (inputFilename.endsWith(".h264") || inputFilename.endsWith(".mp4"))
			{
				frameRate = parameters.getInteger("frame.rate");
				break;
			}
		}
		
		// process each input file
		processInputFiles(ioMap, frameRate);
		
	}

	private static void processInputFiles(HashMap<String, String> ioMap, int frameRate) throws IOException, ParseException
	{
		for (String inputFilename : ioMap.keySet())
		{

			// delete output file, if it exists
			String outputFilename = ioMap.get(inputFilename);
			File outputFile = new File(outputFilename);
			if (outputFile.exists()) outputFile.delete();

			// detect bCodes in input file
			LinkedHashMap<Long, List<MetaCode>> bCodeDetectionResults = null;
			try
			{
				if (inputFilename.endsWith(".jpg") || inputFilename.endsWith(".png")) bCodeDetectionResults = processImage(inputFilename);
				else if (inputFilename.endsWith(".h264") || inputFilename.endsWith(".mp4")) bCodeDetectionResults = processVideo(inputFilename, frameRate);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println("Caused by file: " + inputFilename);
			}
			
			// write bCode detections to file 
			RecordWriter writer = new RecordWriter(outputFilename);
			for (Long timestamp : bCodeDetectionResults.keySet())
			{
				List<MetaCode> bCodes = bCodeDetectionResults.get(timestamp);
				for (MetaCode metaCode : bCodes)
				{
					Record record = new Record(timestamp, metaCode.data, metaCode.nw, metaCode.ne, metaCode.sw, metaCode.support, metaCode.errorCorrectionCount);
					record.roundPatternCoordinates();
					writer.writeRecord(record);	
				}
			}
			writer.close();

		}		
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
		if (inputFilename.endsWith(".jpg") || inputFilename.endsWith(".png") || inputFilename.endsWith(".h264") || inputFilename.endsWith(".mp4")) ioMap.put(inputFilename, inputFilename.substring(0, inputFilename.lastIndexOf(".")) + ".txt");
		else throw new IllegalStateException("bCode detector: unsupported input file extension");
	}
	
}