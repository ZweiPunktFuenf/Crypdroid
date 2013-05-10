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
import java.util.HashMap;
import java.util.Map;

import org.spongycastle.crypto.InvalidCipherTextException;

/**
 * 
 * @author ZweiD
 */
public class BinaryMode implements CrypdroidMode {

	private static final BinaryMode INSTANCE = new BinaryMode();
	public static BinaryMode instance() { return INSTANCE; }

	/*
	 * call like: m_binary.encrypt(plain, pw, cipher, "filename", "text.txt", "key", value);
	 */
	@Override public void encrypt(InputStream plain, String password, OutputStream cipher, Object... options)
			throws IOException, InvalidCipherTextException
	{
		assert 0 != options.length : "no arguments passed via options, was expecting some";
		final Map<String, Object> o = map(options);
		
		o.get("filename");
	}
	
	/*
	 * call like: m_binary.decrypt(cipher, pw, plain, returnMap);
	 */
	@Override public void decrypt(InputStream cipher, String password, OutputStream plain, Object... options)
			throws IOException, InvalidCipherTextException
	{
		assert 1 == options.length && options[0].getClass().equals(HashMap.class) : "expected to be passed a HashMap";
		@SuppressWarnings("unchecked")
		final Map<String, Object> o = (Map<String, Object>) options[0];
		
		o.put("filename", "trolololo");
	}

	private static Map<String, Object> map(Object...elements) {
		assert 0 == elements.length % 2 : "called with an odd number of arguments";
		for(int i = 0; i < elements.length; i += 2)
			assert String.class.equals(elements[i].getClass()) : i + ". argument is not of the key class (String)";
		
		final Map<String, Object> map = new HashMap<String, Object>(elements.length / 2);
		for(int i = 0; i < elements.length; i += 2) map.put((String) elements[i], elements[i+1]);
		
		return map;
	}

}
