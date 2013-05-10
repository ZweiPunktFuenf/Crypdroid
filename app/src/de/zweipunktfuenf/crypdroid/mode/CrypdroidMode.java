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
package de.zweipunktfuenf.crypdroid.mode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.spongycastle.crypto.InvalidCipherTextException;

/**
 * 
 * @author ZweiD
 */
public interface CrypdroidMode {

	public abstract void encrypt(InputStream plain, String password, OutputStream cipher, Object...options)
			throws IOException, InvalidCipherTextException;
	
	public abstract void decrypt(InputStream cipher, String password, OutputStream plain, Object...options)
			throws IOException, InvalidCipherTextException;
	
}
