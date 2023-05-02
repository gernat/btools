package edu.illinois.gernat.btools.behavior.flightactivity;

import edu.illinois.gernat.btools.common.io.record.Record;

public class Feature
{

	public long timestamp;
	
	public int beeID;
	
	public int x;
	
	public int y;
	
	public float angle;

	public int deltaT;
	
	public int deltaX;
	
	public int deltaY;
	
	public float deltaAngle;
	

	public Feature(Record current, Record previous)
	{
		
		// properties of current detection
		timestamp = current.timestamp;
		beeID = current.id;
		x = Math.round(current.center.x);
		y = Math.round(current.center.y);
		angle = (float) Math.atan2(current.orientation.dy, current.orientation.dx);
		
		// differences to previous detection
		if (previous != null)
		{
			deltaT = (int) (timestamp - previous.timestamp);
			deltaX = x - Math.round(previous.center.x);
			deltaY = y - Math.round(previous.center.y);
			deltaAngle = previous.orientation.angleBetween(current.orientation);
		}
		else
		{
			deltaT = -1;
			deltaX = 0;
			deltaY = 0;
			deltaAngle = 0;
		}
		
	}
	
}
