/**
 * Copyright 2013 Felix Gro§e
 * Released under the GNU GPL license
 * 
 * 
 * This file is part of Crypdroid.
 * 
 * Crypdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Crypdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Crypdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.zweipunktfuenf.crypdroid;

import android.util.Base64;

/**
 * 
 * @author ZweiD
 */
public class BufferedBase64 {
	// Base64: 3 bytes -> 4 characters
	// ==> max 2 bytes or 3 characters to buffer
	// ==> len of buffer == 3
	byte[] buffer = new byte[3];
	short n = 0;
	
	// encode + finalize
	public byte[] encode(byte[] bytes, int len) {
		return Base64.encode(buffer(bytes, len, (short) 3), Base64.NO_WRAP);
	}
	public byte[] finalizeEncode() {
		return Base64.encode(buffer, 0, n, Base64.NO_WRAP);
	}
	
	// decode + finalize
	public byte[] decode(byte[] bytes, int len) {
		return Base64.decode(buffer(bytes, len, (short) 4), Base64.NO_WRAP);
	}
	public byte[] finalizeDecode() {
		return Base64.decode(buffer, 0, n, Base64.NO_WRAP);
	}
	
	// internal buffer handling
	private byte[] buffer(byte[] bytes, int length, short blocksize) {
		short nextBufferLen = (short) ((length + n) % blocksize);
		int len = length + n - nextBufferLen;
		
		// prepend buffer
		byte[] fitted = new byte[len];
		System.arraycopy(buffer, 0, fitted, 0, n);
		System.arraycopy(bytes, 0, fitted, n, length - nextBufferLen);
		
		// refill buffer
		if(0 < (n = nextBufferLen)) System.arraycopy(
			bytes, length - nextBufferLen,
			buffer, 0, nextBufferLen
		);
		
		return fitted;
	}
}