/*
 * Copyright (C) 2017, 2018 University of Illinois Board of Trustees.
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
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.google.zxing.NotFoundException;

import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.record.RecordReader;
import edu.illinois.gernat.btools.common.io.record.RecordWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;

/**
 * @version 0.14.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class BCodeDetector
{

	public static void processImage(String[] args) throws NotFoundException, IOException, ParseException
	{
		
		// set parameters
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		Preprocessor.sharpeningSigma = parameters.getDouble("sharpening.sigma"); 
		Preprocessor.sharpeningAmount = parameters.getDouble("sharpening.amount");
		Preprocessor.scalingFactor = (float) parameters.getDouble("scaling.factor");
		Reader.minBlackThreshold = parameters.getInteger("min.intensity.threshold");
		Reader.maxBlackThreshold = parameters.getInteger("max.intensity.threshold");
		Reader.thresholdStepSize = parameters.getInteger("intensity.step.size");
		Detector.minTemplateConservation = parameters.getDouble("min.template.conservation");
		Detector.checkMargin = parameters.getBoolean("conserve.margin");
		String imageFilename = parameters.getString("image.filename");
		String detectedBcodesFilename = parameters.getString("detected.bCodes.filename");
		
		// delete result file
		File file = new File(detectedBcodesFilename);
		if (file.exists()) file.delete();
		
		// read image
		BufferedImage image = ImageIO.read(new File(imageFilename));
		if (image == null) 
		{
			System.err.println("image processor: cannot read image file '" + imageFilename + "'.");
			return;
		}
		
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
		
		// get image timestamp from filename
		long timestamp = Images.getTimestampFromFilename(imageFilename);
		
		// write ID data to file 
		List<Record> records = new ArrayList<Record>();
		String tmpFilename = imageFilename.substring(0, imageFilename.length() - 4) + ".tmp";
		RecordWriter writer = new RecordWriter(tmpFilename);
		for (MetaCode metaID : metaIDs) 
		{
			Record record = new Record(timestamp, metaID.data, metaID.nw, metaID.ne, metaID.sw, metaID.support, metaID.errorCorrectionCount);
			record.roundPatternCoordinates();
			records.add(record);
			writer.writeRecord(record);
		}
		writer.close();

		// verify that written ID data is equal to what got decoded from the image
		int count = 0;
		RecordReader reader = new RecordReader(tmpFilename);
		while (reader.hasMoreRecords())
		{
			Record record = reader.readRecord();
			if (record.equals(records.get(count))) count++;
			else break;
		}
		reader.close();
		
		// delete temporary file if written ID data is different; otherwise
		// rename to result file name
		file = new File(tmpFilename);
		if (count != records.size()) Files.delete(file.toPath());
		else file.renameTo(new File(detectedBcodesFilename));
			
	}

	private static void showVersionAndCopyright() 
	{
		System.out.println("bCode Detector (bTools) 0.13.0");
		System.out.println("Copyright (C) 2017, 2018 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar batch_read_bcodes.jar PARAMETER=VALUE...");
		System.out.println("Detect bCodes in images.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- conserve.margin           whether the bCode border is considered to be part");
		System.out.println("                            of the bCode template");
		System.out.println("- detected.bCodes.filename  output file containing the bCode detection results.");
		System.out.println("- image.filename            input image file");
		System.out.println("- image.list.filename       plain text file listing on each line one input");
		System.out.println("                            image file");
		System.out.println("- scaling.factor       		factor for image scaling prior to detecting bCodes");
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
		System.out.println("Input image filenames need to be a valid date in the format");
		System.out.println("yyyy-MM-dd-HH-mm-ss-SSS.");
		System.out.println();
		System.out.println("Parameters image.filename and detected.bCodes.filename cannot be specified");
		System.out.println("in conjunction with the image.list.filename parameter.");
		System.out.println();
		System.out.println("If the image.list.filename parameter is given, bCode detection output file names");
		System.out.println("are constructed by replacing the input image file extension with 'txt'. Input");
		System.out.println("image file name and extension must be separated by a dot.");
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
		System.exit(1);
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

		// construct constant part of contact predictor argument array
		String[] arguments = new String[args.length + 2];
		System.arraycopy(args, 0, arguments, 0, args.length);
		
		// get arguments
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		if ((parameters.exists("show.credits")) && (parameters.getBoolean("show.credits"))) showCredits();
		
		// check parameters
		if (parameters.exists("image.list.filename")) 
		{
			if (parameters.exists("image.filename")) throw new IllegalStateException("image.list.filename cannot be specified together with image.filename.");
			if (parameters.exists("detected.bCodes.filename")) throw new IllegalStateException("image.list.filename cannot be specified together with detected.bCodes.filename.");
		}
		
		// create map from input file to output file
		HashMap<String, String> ioMap = new HashMap<>();
		if (!parameters.exists("image.list.filename")) ioMap.put(parameters.getString("image.filename"), parameters.getString("detected.bCodes.filename"));
		else
		{
			String imageListFilename = parameters.getString("image.list.filename");
			BufferedReader reader = new BufferedReader(new FileReader(imageListFilename));
			while (reader.ready())
			{
				String imageFileName = reader.readLine();
				String resultFileName = imageFileName.substring(0, imageFileName.lastIndexOf(".")) + ".txt";
				ioMap.put(imageFileName, resultFileName);
			}
			reader.close();
		}

		// process each input image
		for (Entry<String, String> entry : ioMap.entrySet()) 
		{
			arguments[arguments.length - 2] = "image.filename=" + entry.getKey();
			arguments[arguments.length - 1] = "detected.bCodes.filename=" + entry.getValue();			
			try
			{
				processImage(arguments);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println("Caused by file: " + entry.getKey());
			}
		}

	}
	
}