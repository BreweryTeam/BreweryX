/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package com.dre.brewery.utility;

import org.jetbrains.annotations.Contract;

public class Tuple<A, B> {

	/**
	 * The first value in the tuple
	 */
	private final A a;

	/**
	 * The second value in the tuple
	 */
	private final B b;

	public Tuple(A a, B b) {
		this.a = a;
		this.b = b;
	}

	/**
	 * Gets the first value in the tuple
	 */
	@Contract(pure = true)
	public A first() {
		return a;
	}

	/**
	 * Gets the second value in the tuple
	 */
	@Contract(pure = true)
	public B second() {
		return b;
	}

	/**
	 * Gets the first value in the tuple, Synonym for first()
	 */
	@Contract(pure = true)
	public A a() {
		return a;
	}

	/**
	 * Gets the second value in the tuple, Synonym for second()
	 */
	@Contract(pure = true)
	public B b() {
		return b;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Tuple)) {
			return false;
		}

		Tuple<?, ?> tuple = (Tuple<?, ?>) object;
		return tuple.a.equals(a) && tuple.b.equals(b);
	}

	@Override
	public int hashCode() {
		return a.hashCode() ^ b.hashCode();
	}

	@Override
	public String toString() {
		return "Tuple{" +
			'{' + a + '}' +
			'{' + b + "}}";
	}
}
