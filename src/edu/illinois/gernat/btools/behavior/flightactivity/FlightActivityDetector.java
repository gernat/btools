package edu.illinois.gernat.btools.behavior.flightactivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.mining.MiningModelEvaluator;
import org.xml.sax.SAXException;

import edu.illinois.gernat.btools.common.io.record.Record;
import edu.illinois.gernat.btools.common.io.record.RecordReader;
import edu.illinois.gernat.btools.common.io.token.TokenWriter;
import edu.illinois.gernat.btools.common.parameters.Parameters;
import edu.illinois.gernat.btools.tracking.bcode.BCode;

public class FlightActivityDetector
{
	
	private static final int EXPECTED_FRAME_RATE = 10; // Hz

	private static final String RANDOM_FOREST_MODEL_FILE = "random_forest.xml";
	
	private static final int MIN_TIME_BETWEEN_EVENTS = 2900; // ms

	private static final double CREEP_FACTOR = 0.1;
	
	private static final String THIRD_PARTY_LICENSES_FILE = "flight_activity_detector_3rd_party_licenses.txt";
	
	// assumptions: filtered.data.file must be sorted by timestamp
	public static void main(String[] args) throws IOException, SAXException, JAXBException, ParseException
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
		
		// obtain command line arguments; show credits if necessary
		Parameters parameters = Parameters.INSTANCE;
		parameters.initialize(args);
		if ((parameters.exists("show.credits")) && (parameters.getBoolean("show.credits"))) showCredits();
		
		// set parameters
		String bCodeDetectionsFile = parameters.getString("filtered.data.file");
		int frameRate = parameters.getInteger("frame.rate"); // Hz
		String entranceEventsFile = parameters.getString("entrance.events.file");

		// validate arguments
		if (frameRate != EXPECTED_FRAME_RATE) throw new IllegalStateException("The random forest expects a frame rate of " + EXPECTED_FRAME_RATE + "Hz.");
		
		// predict feeding events
		List<Feature> features = computeFeatures(bCodeDetectionsFile, frameRate);
		List<EntranceEvent> entranceEvents = predictEvents(features, frameRate);
		classifyEvents(entranceEvents);
		saveEvents(entranceEvents, entranceEventsFile);
		
	}
	
	private static void saveEvents(List<EntranceEvent> entranceEvents, String entranceEventsFile) throws IOException
	{

		// save events to disk
		TokenWriter eventWriter = new TokenWriter(entranceEventsFile);
		for (EntranceEvent entranceEvent : entranceEvents) if (entranceEvent.type != "other") eventWriter.writeTokens(entranceEvent);
		eventWriter.close();

	}

	private static void classifyEvents(List<EntranceEvent> events) throws IOException, SAXException, JAXBException
	{
		
		// load random forest model and create an evaluator
		MiningModelEvaluator evaluator = (MiningModelEvaluator) new LoadingModelEvaluatorBuilder().load(Thread.currentThread().getContextClassLoader().getResourceAsStream(RANDOM_FOREST_MODEL_FILE)).build();
		evaluator.verify();
 
		// get evaluator input fields
		List<? extends InputField> inputFields = evaluator.getInputFields();
		
		// process events
		for (EntranceEvent event : events)
		{

			// create evaluator arguments from current feature 
			Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
			for (InputField inputField : inputFields) arguments.put(inputField.getName(), inputField.prepare(event.getElementByName(inputField.getName().toString())));

			// classify feature
			Map<FieldName, ?> results = evaluator.evaluate(arguments);
			
			// assign feature class
			event.type = EvaluatorUtil.decode(results.get(evaluator.getTargetField().getName())).toString();
			
		}
		
	}

	private static List<EntranceEvent> predictEvents(List<Feature> features, int frameRate)
	{
		
		// create result and helper variables
		List<EntranceEvent> events = new LinkedList<>();
		EntranceEvent[] tmp = new EntranceEvent[BCode.UNIQUE_ID_COUNT];
		long[] lastSeen = new long[BCode.UNIQUE_ID_COUNT];
		double timeBetweenFrames = 1000.0 / frameRate;
		double maxCreep = timeBetweenFrames * CREEP_FACTOR;
		
		// process features
		for (Feature feature : features)
		{
			
			// create a new event if there is no feature for the current bee 
			// yet or if the last time the bee was detected is too long ago to 
			// continue with the current event
			if ((lastSeen[feature.beeID] == 0) || (feature.timestamp - lastSeen[feature.beeID] > MIN_TIME_BETWEEN_EVENTS))
			{
				EntranceEvent event = new EntranceEvent(feature);
				events.add(event);
				tmp[feature.beeID] = event;
			}
		
			// otherwise, update the current event if the frame-wise difference
			// was computed from two successive frames
			else if (feature.deltaT < timeBetweenFrames + maxCreep)
			{
				EntranceEvent event = tmp[feature.beeID];
				event.verticalDisplacement += feature.deltaY;
				event.horizontalDisplacement += feature.deltaX;
				event.distanceTraveled += (float) Math.sqrt(Math.pow(feature.deltaX, 2) + Math.pow(feature.deltaY, 2));
				event.rotation += feature.deltaAngle;
				event.lastY = feature.y;
				event.duration = (int) (feature.timestamp - event.begin);
			}
			
			// remember when this bee was last seen
			lastSeen[feature.beeID] = feature.timestamp;
				
		}

		// drop events created from a single between-frames difference
		Iterator<EntranceEvent> eventIterator = events.iterator();
		while (eventIterator.hasNext()) if (eventIterator.next().duration == 0) eventIterator.remove();
		
		// done
		return events;
		
	}

	private static List<Feature> computeFeatures(String bCodeDetectionsFile, int frameRate) throws IOException, ParseException
	{

		// open bCode detection file 
		RecordReader reader = new RecordReader(bCodeDetectionsFile);
		
		// create result and helper variables
		List<Feature> features = new LinkedList<>();
		Record[] previousOccurence = new Record[BCode.UNIQUE_ID_COUNT];

		// process bCode detections
		while (reader.hasMoreRecords())
		{

			// read next record
			Record record = reader.readRecord();

			// compute feature
			Feature feature = new Feature(record, previousOccurence[record.id]);
			features.add(feature);
			
			// remember this record
			previousOccurence[record.id] = record;
			
		}
		
		// close bCode detection file		
		reader.close();
		
		// return features
		return features;
		
	}
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("Flight Activity Detector (bTools) 0.15.1");
		System.out.println("Copyright (C) 2017-2022 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}
	
	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar flight_activity_detector.jar PARAMETER=VALUE...");
		System.out.println("Detect hive exits and returns.");
		System.out.println();  		
		System.out.println("Parameters:");
		System.out.println("- entrance.events.file output file containing detected entrance events");
		System.out.println("- filtered.data.file   file containing the bCode detection results. Must be sorted");		
		System.out.println("                       by timestamp column");
		System.out.println("- frame.rate           frame rate at which bCodes were recorded");
		System.out.println("- show.credits         set to \"true\" or 1 to display credits and exit");
	}
	
	private static void showCredits() throws IOException 
	{
		showVersionAndCopyright();
		System.out.println();
		System.out.println("This software uses the following third party software that is distributed");
		System.out.println("under its own terms:");
		System.out.println();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(THIRD_PARTY_LICENSES_FILE); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		while (reader.ready()) System.out.println(reader.readLine());
		reader.close();
		inputStream.close();
		System.exit(1);
	}

}
