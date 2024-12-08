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

import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Base91EncoderStream extends FilterOutputStream {

	private final basE91 encoder = new basE91();
	private byte[] buf = new byte[32];
	private byte[] encBuf = new byte[48];
	private int writer = 0;
	private int encoded = 0;

	public Base91EncoderStream(OutputStream out) {
		super(out);
	}

	private void encFlush() throws IOException {
		encoded = encoder.encode(buf, writer, encBuf);
		out.write(encBuf, 0, encoded);
		writer = 0;
	}

	@Override
	public void write(int b) throws IOException {
		buf[writer++] = (byte) b;
		if (writer >= buf.length) {
			encFlush();
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len == 0) return;
		if (b == null) throw new NullPointerException();
		if (len < 0 || off < 0 || (off + len) > b.length || off > b.length || (off + len) < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (buf.length - writer >= len) {
			// Enough space in the buffer, copy it in
			System.arraycopy(b, off, buf, writer, len);
			writer += len;
			if (writer >= buf.length) {
				encFlush();
			}
			return;
		}

		if (off == 0 && buf.length >= len) {
			// Buffer is too full but it would fit, so flush and encode data directly
			encFlush();
			encoded = encoder.encode(b, len, encBuf);
			out.write(encBuf, 0, encoded);
			return;
		}

		// More data than space in the Buffer
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		while (true) {
			writer += in.read(buf, writer, buf.length - writer);
			if (writer >= buf.length) {
				encFlush();
			} else {
				break;
			}
		}
	}

	@Override
	public void flush() throws IOException {
		if (writer > 0) {
			encFlush();
		}

		encoded = encoder.encEnd(encBuf);
		if (encoded > 0) {
			out.write(encBuf, 0, encoded);
		}
		super.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
		encoder.encReset();
		buf = null;
		encBuf = null;
	}
}
