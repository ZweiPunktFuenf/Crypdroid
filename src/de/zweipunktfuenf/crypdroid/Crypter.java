/**
 * 
 */
package de.zweipunktfuenf.crypdroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.ParametersWithIV;

import android.content.Context;
import android.util.Base64;
import android.util.Log;


/**
 * 
 * @author ZweiD
 */
public class Crypter {

	//----  Cipher Consts  -----------------------------------------------------------
	private static final int ITERATIONS = 1000;
	private static final int KEY_LENGTH = 256;
	private static final byte[] SALT = {
		//0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 //TODO
	};
	
	// arbitrary buffer size (2^12)
	private static final int BUFFER_LENGTH = 4096;

	protected static final String INTERNAL_IN = "temp_in";
	protected static final String INTERNAL_OUT = "temp_out";

	
	public static void encrypt(
			InputStream plaintext,
			String filename,
			String password,
			Context app
	)	throws
			IOException,
			InvalidCipherTextException
	{
		OutputStream ciphertext = null;
		
		try {
			ciphertext = app.openFileOutput(INTERNAL_OUT, Context.MODE_PRIVATE);
			
			// generate key with PKCS #5
			char[] pw = password.toCharArray();
			PBEParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
			generator.init(
				PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(pw),
				SALT, ITERATIONS
			);
			
			// generate random initialization-vector
			byte[] iv = new byte[16];
			new SecureRandom().nextBytes(iv);
			
			// Initialize the AES cipher
			BlockCipher engine = new AESEngine();
			CBCBlockCipher cbc = new CBCBlockCipher(engine);
			PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(cbc);
			cipher.init(true,
				new ParametersWithIV(
					generator.generateDerivedParameters(KEY_LENGTH),
					iv
				)
			);
	
			// create buffers
			byte[] inBuffer = new byte[BUFFER_LENGTH];
			byte[] outBuffer = new byte[BUFFER_LENGTH];
			BufferedBase64Coder b64 = new BufferedBase64Coder();
			
			{// prepend iv (not encrypted)
				byte[] enc = Base64.encode(iv, Base64.DEFAULT);
				ciphertext.write(enc, 0, 24); // cut off \n at the end
			}
			
			{// encrypt and write filename
				byte[] fn;
				try { fn = (filename + "/").getBytes("UTF8"); }
				catch (UnsupportedEncodingException e) {
					Log.e(Crypter.class.getSimpleName(),
						"no UTF8 encoding available to encode filename",
						e
					);
					// should be UTF8 anyway
					fn = (filename + "/").getBytes();
				}
				int len = fn.length;
				int n = cipher.processBytes(fn, 0, len, outBuffer, 0);
				ciphertext.write(b64.encode(outBuffer, n));
			}
	
			{// encrypt and write plaintext
				int len = plaintext.read(inBuffer);
				while(-1 != len) {
					int n = cipher.processBytes(inBuffer, 0, len, outBuffer, 0);
					ciphertext.write(b64.encode(outBuffer, n));
		
					len = plaintext.read(inBuffer);
				}
				
				
				int n = cipher.doFinal(outBuffer, 0);
				ciphertext.write(b64.encode(outBuffer, n));
				ciphertext.write(b64.finalizeEncode());
			}
			
		} finally {
			try {
				if(null != plaintext) plaintext.close();
				if(null != ciphertext) ciphertext.close();
			} catch(IOException ignore) {
				Log.i(Crypter.class.getSimpleName(),
					"error while closing streams",
					ignore
				);
			}
		}
	}
	
	public static String decrypt(
			InputStream ciphertext,
			String password,
			Context app
	)	throws
			IOException,
			InvalidCipherTextException
	{
		OutputStream plaintext = null;
		
		try {
			plaintext = app.openFileOutput(INTERNAL_OUT, Context.MODE_PRIVATE);
			
			// generate key with PKCS #5
			char[] pw = password.toCharArray();
			PBEParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
			generator.init(
				PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(pw),
				SALT, ITERATIONS
			);
			
			byte[] iv;
			{// read initialization-vector
				byte[] enc = new byte[24];
				ciphertext.read(enc);
				iv = Base64.decode(enc, Base64.DEFAULT);
			}
			
			// Initialize the AES cipher
			BlockCipher engine = new AESEngine();
			CBCBlockCipher cbc = new CBCBlockCipher(engine);
			PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(cbc);
			cipher.init(false,
				new ParametersWithIV(
					generator.generateDerivedParameters(KEY_LENGTH),
					iv
				)
			);
	
			byte[] inBuffer = new byte[BUFFER_LENGTH];
			byte[] outBuffer = new byte[BUFFER_LENGTH];
			BufferedBase64Coder b64 = new BufferedBase64Coder();
			

			String filename = "";
			{// read and decrypt filename
				int len = ciphertext.read(inBuffer);
				byte[] dec = b64.decode(inBuffer, len);
				
				int n = cipher.processBytes(dec, 0, dec.length, outBuffer, 0);
				int c = (byte) '/'; // same value as in UTF-8
				
				
				int off = 0;
				while(off < n && c != outBuffer[off]) off++;
				if(off == n) throw new InvalidParameterException(
					"invalid ciphertext: either not encrypted with Crypdroid, " +
					"damaged data or not a ciphertext"
				);
				
				byte[] fn = new byte[off];
				System.arraycopy(outBuffer, 0, fn, 0, off);
				filename = new String(fn, "UTF8");
				
			 // write the rest to plaintext
				plaintext.write(outBuffer, off + 1, n - off);
			}
			
			{// read and decrypt ciphertext
				int len = ciphertext.read(inBuffer);
				while(-1 != len) {
					byte[] dec = b64.decode(inBuffer, len);
					int n = cipher.processBytes(dec, 0, dec.length, outBuffer, 0);
					plaintext.write(outBuffer, 0, n);
		
					len = ciphertext.read(inBuffer);
				}
				
				byte[] dec = b64.finalizeDecode();
				if(0 < dec.length) {
					int n = cipher.processBytes(dec, 0, dec.length, outBuffer, 0);
					plaintext.write(outBuffer, 0, n);
				}
				
				int n = cipher.doFinal(outBuffer, 0);
				plaintext.write(outBuffer, 0, n);
			}
			
			return filename;
		} finally {
			try {
				plaintext.close();
				ciphertext.close();
			} catch(IOException ignore) {
				Log.i(Crypter.class.getSimpleName(),
					"error while closing streams",
					ignore
				);
			}
		}
	}
	
	public static void clearInternalFiles(Context app) {

	    app.deleteFile(INTERNAL_IN);
	    app.deleteFile(INTERNAL_OUT);
	}
	
	static class BufferedBase64Coder {

		// Base64: 3 bytes -> 4 characters
		// ==> max 2 bytes / 3 characters to buffer
		byte[] buffer = new byte[3];
		short n = 0;
		
		byte[] encode(byte[] bytes, int len) {
			return Base64.encode(buffer(bytes, len, (short) 3), Base64.NO_WRAP);
		}
		byte[] finalizeEncode() {
			return Base64.encode(buffer, 0, n, Base64.NO_WRAP);
		}
		
		byte[] decode(byte[] bytes, int len) {
			return Base64.decode(buffer(bytes, len, (short) 4), Base64.NO_WRAP);
		}
		byte[] finalizeDecode() {
			Log.e("find me", "gebuffered: "+n);
			Log.e("find me", Arrays.toString(buffer) + "  " + new String(buffer, 0, n));
			return Base64.decode(buffer, 0, n, Base64.NO_WRAP);
		}
		
		
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
}
