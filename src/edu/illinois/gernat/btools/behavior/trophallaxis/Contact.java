/*
 * Copyright (C) 2017 University of Illinois Board of Trustees.
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

import edu.illinois.gernat.btools.common.io.token.Tokenizable;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Contact
implements Tokenizable
{

	public long timestamp;
	
	public int id1;
	
	public int id2;

	public Contact(long timestamp, int id1, int id2)
	{
		this.timestamp = timestamp;
		this.id1 = Math.min(id1, id2);
		this.id2 = Math.max(id1, id2);
	}
	
	public Contact(String[] tokens)
	{
		timestamp = Long.parseLong(tokens[0]); 
		int tmp1 = Integer.parseInt(tokens[1]); 
		int tmp2 = Integer.parseInt(tokens[2]); 
		id1 = Math.min(tmp1, tmp2);
		id2 = Math.max(tmp1, tmp2);
	}
	
	@Override
	public Object[] toTokens()
	{
		Object[] tokens = new Object[3];
		tokens[0] = timestamp;
		tokens[1] = id1;
		tokens[2] = id2;
		return tokens;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null) return false;
		if (o.getClass() != getClass()) return false;
		Contact other = (Contact) o;
		return ((id1 == other.id1) && (id2 == other.id2));
	}

	@Override
	public int hashCode()
	{
		return Contact.hashCode(id1, id2);
	}
	
	public static int hashCode(int id1, int id2)
	{
		return ((id1 & 0xFFFF) << 16) + (id2 & 0xFFFF);
	}

}
