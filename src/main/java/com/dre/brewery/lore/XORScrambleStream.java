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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * A Scramble Stream that uses XOR operations to scramble an outputstream.
 * <p>a byte generator feeded with the seed is used as xor source
 * <p>The resulting data can be unscrambled by the XORUnscrambleStream
 */
public class XORScrambleStream extends FilterOutputStream {

	private final long seed;
	private SeedInputStream xorStream;
	private boolean running;

	/**
	 * Create a new instance of an XORScrambler, scrambling the given outputstream
	 *
	 * @param out The Outputstream to be scrambled
	 * @param seed The seed used for scrambling
	 */
	public XORScrambleStream(OutputStream out, long seed) {
		super(out);
		this.seed = seed;
	}

	/**
	 * To start the scrambling process this has to be called before writing any data to this stream.
	 * <br>Before starting the scrambler, any data will just be passed through unscrambled to the underlying stream.
	 * <br>The Scrambling can be started and stopped arbitrarily at any point, allowing for parts of unscrambled data in the stream.
	 *
	 * @throws IOException IOException
	 */
	public void start() throws IOException {
		running = true;
		if (xorStream == null) {
			short id = 0;
			while (id == 0) {
				id = (short) new Random().nextInt();
			}
			xorStream = new SeedInputStream(seed ^ id);
			out.write((byte) (id >> 8));
			out.write((byte) id);
			write((int) (seed >> 48) & 0xFF); // parity/sanity
		}
	}

	/**
	 * Stop the scrambling, any following data will be passed through unscrambled.
	 * <br>The scrambling can be started again at any point after calling this
	 */
	public void stop() {
		running = false;
	}

	/**
	 * Mark the stream as unscrambled, any effort of unscrambing the data later will automatically read the already unscrambled data.
	 * <p>Useful if a stream may be scrambled or unscrambled, the unscrambler will automatically identify either way.
	 *
	 * @throws IOException IOException
	 * @throws IllegalStateException If the Scrambler was started in normal scrambling mode before
	 */
	public void startUnscrambled() throws IOException, IllegalStateException {
		if (xorStream != null) throw new IllegalStateException("The Scrambler was started in scrambling mode before");
		short id = 0;
		out.write((byte) (id >> 8));
		out.write((byte) id);
	}

	@Override
	public void write(int b) throws IOException {
		if (!running) {
			out.write(b);
			return;
		}
		out.write(b ^ xorStream.read());
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (!running) {
			out.write(b, off, len);
			return;
		}
		byte[] xored = new byte[len];
		xorStream.read(xored);
		int j = off;
		for (int i = 0; i < len; i++) {
			xored[i] ^= b[j++];
		}
		out.write(xored);
	}

	@Override
	public void close() throws IOException {
		running = false;
		xorStream = null;
		super.close();
	}
}
