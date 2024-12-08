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

package com.dre.brewery.lore;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Arrays;

public class SeedInputStream extends InputStream {
	// From java.util.Random
	private static final long multiplier = 0x5DEECE66DL;
	private static final long addend = 0xBL;
	private static final long mask = (1L << 48) - 1;

	private long seed;
	private byte[] buf = new byte[4];
	private byte reader = 4;
	private long markSeed;
	private byte[] markbuf;

	public SeedInputStream(long seed) {
		this.seed = (seed ^ multiplier) & mask;
	}

	private void calcSeed() {
		seed = (seed * multiplier + addend) & mask;
	}

	private void genNext() {
		calcSeed();
		int next = (int)(seed >>> 16);
		buf[0] = (byte) (next >> 24);
		buf[1] = (byte) (next >> 16);
		buf[2] = (byte) (next >> 8);
		buf[3] = (byte) next;
		reader = 0;
	}

	@Override
	public int read(@NotNull byte[] b, int off, int len) {
		for (int i = off; i < len; i++) {
			if (reader >= 4) {
				genNext();
			}
			b[i] = buf[reader++];
		}
		return len;
	}

	@Override
	public int read() {
		if (reader == 4) {
			genNext();
		}
		return buf[reader++];
	}

	@Override
	public long skip(long toSkip) {
		long n = toSkip;
		while (n > 0) {
			if (reader < 4) {
				reader++;
				n--;
			} else if (n >= 4) {
				calcSeed();
				n -= 4;
			} else {
				genNext();
			}
		}
		return toSkip;
	}

	@Override
	public void close() {
		buf = null;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		markbuf = new byte[] {buf[0], buf[1], buf[2], buf[3], reader};
		markSeed = seed;
	}

	@Override
	public synchronized void reset() {
		seed = markSeed;
		buf = Arrays.copyOfRange(markbuf, 0, 4);
		reader = markbuf[4];
	}
}
