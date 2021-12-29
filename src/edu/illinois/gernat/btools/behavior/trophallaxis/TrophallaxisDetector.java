/*
 * Copyright (C) 2017, 2018, 2019, 2020 University of Illinois Board of 
 * Trustees.
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

package edu.illinois.gernat.btools.behavior.trophallaxis;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import edu.illinois.gernat.btools.behavior.trophallaxis.head.Head;
import edu.illinois.gernat.btools.behavior.trophallaxis.head.HeadDetector;
import edu.illinois.gernat.btools.behavior.trophallaxis.touch.Touch;
import edu.illinois.gernat.btools.behavior.trophallaxis.touch.TouchDetector;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.image.Images;
import edu.illinois.gernat.btools.common.io.record.IndexedReader;
import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.token.TokenReader;
import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;

public class TrophallaxisDetector
{

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

	private static Map<Integer, Head> predictHeads(BufferedImage image, List<Contact> contacts, Map<Integer, Record> records)
	{
		
		// predict position, orientation and shape of the heads of 
		// potentially interacting bees
		HashSet<Record> contactRecords = new HashSet<Record>();
		for (Contact contact : contacts) 
		{
			contactRecords.add(records.get(contact.id1));
			contactRecords.add(records.get(contact.id2));
		}
		HashMap<Integer, Head> heads = new HashMap<Integer, Head>();
		for (Record record : contactRecords) heads.put(record.id, HeadDetector.detect(image, record.center, record.orientation));
		return heads;
		
	}

	private static void filterContacts(List<Contact> contacts, Map<Integer, Head> heads, int visionMinDistance, int visionMaxDistance, double visionMaxAngleSum)
	{

		// remove each contact for which the relative position and orientation
		// of the two head predictions do not meet the specified requirements 
		ListIterator<Contact> contactsIterator = contacts.listIterator();
		while (contactsIterator.hasNext())
		{
			Contact contact = contactsIterator.next();
			Head head1 = heads.get(contact.id1);
			Head head2 = heads.get(contact.id2);
			if (!isContact(head1.center, head2.center, head1.orientation, head2.orientation, visionMinDistance, visionMaxDistance, visionMaxAngleSum)) contactsIterator.remove();
		}
		
	}

	private static void retainTouching(BufferedImage image, List<Contact> contacts, Map<Integer, Head> heads)
	{
		
		// remove all potential contacts for bees which are not touching 
		// each other
		Iterator<Contact> contactsIterator = contacts.iterator();
		while (contactsIterator.hasNext())
		{
			Contact contact = contactsIterator.next();
			Touch touch = TouchDetector.detect(image, contact.id1, contact.id2, heads);
			if (!touch.isTouching) contactsIterator.remove();
		}
		
	}

	public static void processImage(String[] args) throws IOException, ParseException
	{
		
		// get arguments
		Parameters.INSTANCE.initialize(args);
		int distanceLabelHead = Parameters.INSTANCE.getInteger("distance.label.head");
		int geometryMinDistance = Parameters.INSTANCE.getInteger("geometry.min.distance");
		int geometryMaxDistance = Parameters.INSTANCE.getInteger("geometry.max.distance");
		double geometryMaxAngleSum = Math.toRadians(Parameters.INSTANCE.getInteger("geometry.max.angle.sum"));
		int visionMinDistance = Parameters.INSTANCE.getInteger("vision.min.distance");
		int visionMaxDistance = Parameters.INSTANCE.getInteger("vision.max.distance");
		double visionMaxAngleSum = Math.toRadians(Parameters.INSTANCE.getInteger("vision.max.angle.sum"));
		String filteredDataFile = Parameters.INSTANCE.getString("filtered.data.file");
		String imageFilename = Parameters.INSTANCE.getString("image.filename");
		String rawContactsFile = Parameters.INSTANCE.getString("trophallaxis.file"); 

		// delete result file
		File file = new File(rawContactsFile);
		if (file.exists()) file.delete();
		
		// read records corresponding to the image file
		IndexedReader indexedReader = new IndexedReader(filteredDataFile);
		long timestamp = Images.getTimestampFromFilename(imageFilename);
		List<Record> records = indexedReader.readThis(timestamp);
		indexedReader.close();
		if (records == null) 
		{
			file.createNewFile();
			return;
		}

		// predict contacts using the position and orientation of the labels
		List<Contact> contacts = predictContacts(records, distanceLabelHead, geometryMinDistance, geometryMaxDistance, geometryMaxAngleSum);
		if (contacts.isEmpty()) 
		{
			file.createNewFile();
			return;
		}

		// predict position, orientation and shape of the heads of bees which
		// might be engaged in a social contact
		BufferedImage image = ImageIO.read(new File(imageFilename));
		HashMap<Integer, Record> recordLookupTable = new HashMap<Integer, Record>();
		for (Record record : records) recordLookupTable.put(record.id, record);
		Map<Integer, Head> heads = predictHeads(image, contacts, recordLookupTable);

		// drop contacts for which the relative position and orientation of   
		// the head of the bees is not within the specified range
		filterContacts(contacts, heads, visionMinDistance, visionMaxDistance, visionMaxAngleSum);
		if (contacts.isEmpty()) 
		{
			file.createNewFile();
			return;
		}

		// predict if the bees potentially engaged in a social contact are 
		// touching each other's head with their antennae, feet, or tongue and
		// filter out those which don't
		retainTouching(image, contacts, heads);
		if (contacts.isEmpty()) 
		{
			file.createNewFile();
			return;
		}

		// write contact data to file 
		String tmpFilename = rawContactsFile.substring(0, rawContactsFile.length() - 4) + ".tmp";
		TokenWriter tokenWriter = new TokenWriter(tmpFilename);
		for (Contact contact : contacts) tokenWriter.writeTokens(contact);
		tokenWriter.close();

		// verify that contact data on disk is equal to the contact data in 
		// memory
		int count = 0;
		TokenReader tokenReader = new TokenReader(tmpFilename);
		while (tokenReader.hasMoreLines())
		{
			Contact contact = new Contact(tokenReader.readTokens());
			if (contact.equals(contacts.get(count))) count++;
			else break;
		}
		tokenReader.close();

		// delete temporary file if written contact data is different; 
		// otherwise rename to result file name
		file = new File(tmpFilename);
		if (count != contacts.size()) Files.delete(file.toPath());
		else file.renameTo(new File(rawContactsFile));
		
	}

	private static void showVersionAndCopyright() 
	{
		System.out.println("Trophallaxis Detector (bTools) 0.14.0");
		System.out.println("Copyright (C) 2017, 2018, 2019, 2020 University of Illinois Board of Trustees");
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
		System.out.println("- contrast.threshold        contrast threshold used during local thresholding");
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
		System.out.println("- leveling.threshold        maximum allowed intensity of any pixel in the");
		System.out.println("                            image. Pixels with a higher intensity will be set");
		System.out.println("                            to this threshold");
		System.out.println("- max.path.thickness        maximum thickness of a path connecting the heads");
		System.out.println("                            of putative interaction partners");
		System.out.println("- max.thick.segment.length  maximum length of the segments that may be thicker");
		System.out.println("                            than max.path.thickness at the beginning and at");
		System.out.println("                            the end of a path");
		System.out.println("- mean.head.pixel.intensity average expected intensity of bee head pixels");
		System.out.println("- show.credits              set to \"true\" or 1 to display credits and exit");
		System.out.println("- thresholding.method       name of the local thresholding method used for");
		System.out.println("                            converting grayscale images to binary images. Must");
		System.out.println("                            be 'Bernsen'");
		System.out.println("- trophallaxis.file         output file containing the trophallaxis detections");
		System.out.println("- thresholding.radius       radius of the neighborhood over which the local");
		System.out.println("                            threshold will be computed");
		System.out.println("- vision.max.angle.sum      maximum sum of the angles between a line");
		System.out.println("                            connecting the computer-vision-predicted centers");
		System.out.println("                            of the heads of two potential interaction partners");
		System.out.println("                            and the orientation vectors of their heads");
		System.out.println("- vision.max.distance       maximum distance between the");
		System.out.println("                            computer-vision-predicted centers of the heads of");
		System.out.println("                            two potential interaction partners");
		System.out.println("- vision.min.distance       minimum distance between the");
		System.out.println("                            computer-vision-predicted centers of the heads of");
		System.out.println("                            two potential interaction partners");
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

		// construct constant part of contact predictor argument array
		String[] arguments = new String[args.length + 2];
		System.arraycopy(args, 0, arguments, 0, args.length);
		
		// get arguments
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		if ((parameters.exists("show.credits")) && (parameters.getBoolean("show.credits"))) showCredits();
		
		// check arguments
		if (parameters.exists("image.list.filename")) 
		{
			if (parameters.exists("image.filename")) throw new IllegalStateException("image.list.filename cannot be specified together with image.filename.");
			if (parameters.exists("trophallaxis.file")) throw new IllegalStateException("image.list.filename cannot be specified together with trophallaxis.file.");
		}
		
		// create map from input file to output file
		HashMap<String, String> ioMap = new HashMap<>();
		if (!parameters.exists("image.list.filename")) ioMap.put(parameters.getString("image.filename"), parameters.getString("trophallaxis.file"));
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
			arguments[arguments.length - 1] = "trophallaxis.file=" + entry.getValue();			
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