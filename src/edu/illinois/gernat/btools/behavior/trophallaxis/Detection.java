package edu.illinois.gernat.btools.behavior.trophallaxis;

import edu.illinois.gernat.btools.common.io.token.Tokenizable;

public class Detection
implements Tokenizable
{
	
	public long timestamp;
	
	public int id1;
	
	public int id2;
	
	public float trophallaxisProbability;
	
	public float id1IsDonorProbability;
	
	public Detection(long timestamp, int id1, int id2, float trophallaxisProbability, float id1IsDonorProbability)
	{
		this.timestamp = timestamp;
		this.id1 = id1;
		this.id2 = id2;
		this.trophallaxisProbability = trophallaxisProbability;
		this.id1IsDonorProbability = id1IsDonorProbability;		
	}
	
	@Override
	public Object[] toTokens()
	{
		Object[] tokens = new Object[5];
		tokens[0] = timestamp;
		tokens[1] = id1;
		tokens[2] = id2;
		tokens[3] = trophallaxisProbability;
		tokens[4] = id1IsDonorProbability;
		return tokens;
	}
	
}
