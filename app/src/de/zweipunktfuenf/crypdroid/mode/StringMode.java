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
import java.security.SecureRandom;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.encoders.Base64;

import de.zweipunktfuenf.crypdroid.BufferedBase64;

import android.util.Log;

public class StringMode implements CrypdroidMode {
	
	private static final StringMode INSTANCE = new StringMode();
	public static StringMode instance() { return INSTANCE; }

	
	/* 1. generate random iv
	 * 2. Base64 encode iv and prepend to message
	 * 3. use PKCS#5 to generate 256b SessionKey from password
	 * 4. use 256b AES-CBC for encryption
	 * 5. Base64 encode encrypted message
	 */
	@Override public void encrypt(InputStream plain, String password, OutputStream cipher, Object... options)
			throws IOException, InvalidCipherTextException
	{
		if(0 != options.length) Log.w("StringMode#encrypt", "Ignoring arguments passed via options");
		
		final int BUFFER_LENGTH = 4096; // 2^12
		final byte[] inBuffer = new byte[BUFFER_LENGTH];
		final byte[] outBuffer = new byte[BUFFER_LENGTH];
		final int ITERATIONS = 1000;
		final int KEY_LENGTH = 256;
		final byte[] SALT = new byte[0];
		final BufferedBase64 bb64 = new BufferedBase64();
		
		
		// generate key with PKCS #5
		byte[] pw = PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(password.toCharArray());
		PBEParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
		generator.init(pw, SALT, ITERATIONS);
		CipherParameters cipherParams = generator.generateDerivedParameters(KEY_LENGTH);
		
		// generate random initialization-vector
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		
		// Initialize the AES cipher
		BlockCipher engine = new AESEngine();
		CBCBlockCipher cbc = new CBCBlockCipher(engine);
		PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(cbc);
		pbbc.init(true, new ParametersWithIV(cipherParams, iv));
		
		// prepend iv (not encrypted)
		// cut off \n at the end ==> len == 24
		cipher.write(Base64.encode(iv), 0, 24);
		
		// encrypt and write plaintext
		int len; while(-1 != (len = plain.read(inBuffer))) {
			int n = pbbc.processBytes(inBuffer, 0, len, outBuffer, 0);
			cipher.write(bb64.encode(outBuffer, n));
		}
		
		int n = pbbc.doFinal(outBuffer, 0);
		cipher.write(bb64.encode(outBuffer, n));
		cipher.write(bb64.finalizeEncode());
	}
		
	/* 1. read iv from the start of the message
	 * 2. Base64 decode iv
	 * 3. use PKCS#5 to generate 256b SessionKey from password
	 * 5. Base64 decode encrypted message
	 * 5. use 256b AES-CBC for decryption
	 */
	@Override public void decrypt(InputStream cipher, String password, OutputStream plain, Object... options)
			throws IOException, InvalidCipherTextException
	{
		if(0 != options.length) Log.w("StringMode#decrypt", "Ignoring arguments passed via options");
		
		final int BUFFER_LENGTH = 4096; // 2^12
		final byte[] inBuffer = new byte[BUFFER_LENGTH];
		final byte[] outBuffer = new byte[BUFFER_LENGTH];
		final int ITERATIONS = 1000;
		final int KEY_LENGTH = 256;
		final byte[] SALT = new byte[0];
		final BufferedBase64 bb64 = new BufferedBase64();
		
		// read initialization-vector
		byte[] enc = new byte[24];
		cipher.read(enc);
		byte[] iv = new BufferedBase64().decode(enc, enc.length);
		
		// generate key with PKCS #5
		byte[] pw = PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(password.toCharArray());
		PBEParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
		generator.init( pw, SALT, ITERATIONS );
		CipherParameters cipherParams = generator.generateDerivedParameters(KEY_LENGTH);
		
		// late initiation of the AES cipher because we need the iv from the ciphertext
		BlockCipher engine = new AESEngine();
		CBCBlockCipher cbc = new CBCBlockCipher(engine);
		PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(cbc);
		pbbc.init(false, new ParametersWithIV(cipherParams, iv));
		
		
		// read and decrypt ciphertext
		int len; while(-1 != (len = cipher.read(inBuffer))) {
			byte[] dec = bb64.decode(inBuffer, len);
			int n = pbbc.processBytes(dec, 0, dec.length, outBuffer, 0);
			plain.write(outBuffer, 0, n);
		}
		
		byte[] dec = bb64.finalizeDecode();
		if(0 < dec.length) {
			int n = pbbc.processBytes(dec, 0, dec.length, outBuffer, 0);
			plain.write(outBuffer, 0, n);
		}
		
		int n = pbbc.doFinal(outBuffer, 0);
		plain.write(outBuffer, 0, n);
	}
}
