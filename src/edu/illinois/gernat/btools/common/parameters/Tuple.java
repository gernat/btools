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

package edu.illinois.gernat.btools.common.parameters;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
public class Tuple<A, B>
{

	public final A a;
	
	public final B b;

	private Tuple(A a, B b)
	{
		this.a = a;
		this.b = b;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof Tuple)) return false;
		Tuple<?, ?> t = (Tuple<?, ?>) other;
		return a.equals(t.a) && b.equals(t.b);
	}

	@Override
	public int hashCode()
	{
		return (a == null ? 0 : a.hashCode()) ^ (b == null ? 0 : b.hashCode());
	}

	public static <A, B> Tuple<A, B> of(A a, B b)
	{
		return new Tuple<A, B>(a, b);
	}
	
}	

