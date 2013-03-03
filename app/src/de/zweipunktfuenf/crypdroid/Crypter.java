/**
 * 
 */
package de.zweipunktfuenf.crypdroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

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
		// to be compatible with CT2, this has to be an empty byte array (len == 0)
		//0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
	};
	
	// arbitrary buffer size (2^12)
	private static final int BUFFER_LENGTH = 4096;

	protected static final String INTERNAL_IN = "temp_in";
	protected static final String INTERNAL_OUT = "out"; // unencoded output
	protected static final String INTERNAL_ENC = "out_enc"; // encoded output

	public static void encrypt(
			InputStream plaintext,
			String filename,
			String password,
			Context app /*,
			BufferedEncoder encoder = new BufferedBase64()*/
	)	throws
			IOException,
			InvalidCipherTextException
	{	encrypt(plaintext, filename, password, app, new BufferedBase64()); }
	
	public static void encrypt(
			InputStream plaintext,
			String filename,
			String password,
			Context app,
			BufferedEncoder encoder
	)	throws
			IOException,
			InvalidCipherTextException
	{
		OutputStream ciphertext = null;
		OutputStream enc_cipher = null;
		
		try {
			ciphertext = app.openFileOutput(INTERNAL_OUT, Context.MODE_PRIVATE);
			enc_cipher = app.openFileOutput(INTERNAL_ENC, Context.MODE_PRIVATE);

			byte[] iv = new byte[16];
			PaddedBufferedBlockCipher cipher;
			{// cipher setup
				// generate key with PKCS #5
				char[] pw = password.toCharArray();
				PBEParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
				generator.init(
					PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(pw),
					SALT, ITERATIONS
				);
				
				// generate random initialization-vector
				new SecureRandom().nextBytes(iv);
				
				// Initialize the AES cipher
				BlockCipher engine = new AESEngine();
				CBCBlockCipher cbc = new CBCBlockCipher(engine);
				cipher = new PaddedBufferedBlockCipher(cbc);
				cipher.init(true,
					new ParametersWithIV(
						generator.generateDerivedParameters(KEY_LENGTH),
						iv
					)
				);
			}
	
			// create buffers
			byte[] inBuffer = new byte[BUFFER_LENGTH];
			byte[] outBuffer = new byte[BUFFER_LENGTH];
			
			{// prepend iv (not encrypted)
				// cut off \n at the end ==> len == 24
				enc_cipher.write(Base64.encode(iv, Base64.DEFAULT), 0, 24);
				ciphertext.write(iv, 0, 16);
			}
			
			if(null != filename) {// encrypt and write filename
				byte[] fn;
				try { fn = (filename + "/").getBytes("UTF8"); }
				catch (UnsupportedEncodingException e) {
					Log.e(Crypter.class.getSimpleName(),
						"no UTF8 encoding available to encode filename",
						e
					);
					fn = (filename + "/").getBytes(); // should be UTF8 anyway
				}
				int n = cipher.processBytes(fn, 0, fn.length, outBuffer, 0);
				enc_cipher.write(encoder.encode(outBuffer, n));
				ciphertext.write(outBuffer, 0, n);
			}
	
			{// encrypt and write plaintext
				int len = plaintext.read(inBuffer);
				while(-1 != len) {
					int n = cipher.processBytes(inBuffer, 0, len, outBuffer, 0);
					enc_cipher.write(encoder.encode(outBuffer, n));
					ciphertext.write(outBuffer, 0, n);
		
					len = plaintext.read(inBuffer);
				}
				
				
				int n = cipher.doFinal(outBuffer, 0);
				enc_cipher.write(encoder.encode(outBuffer, n));
				enc_cipher.write(encoder.finalizeEncode());
				ciphertext.write(outBuffer, 0, n);
			}
			
		} finally {
			try {
				if(null != plaintext ) plaintext .close();
				if(null != ciphertext) ciphertext.close();
				if(null != enc_cipher) enc_cipher.close();
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
			Context app,
			boolean isText
	)	throws
			IOException,
			InvalidCipherTextException
	{
		OutputStream plaintext = null;
		BufferedEncoder encoder = new BufferedBase64();
		
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
			

			String filename = "";
			if(!isText) {// read and decrypt filename
				int len = ciphertext.read(inBuffer);
				byte[] dec = encoder.decode(inBuffer, len);
				
				int n = cipher.processBytes(dec, 0, dec.length, outBuffer, 0);
				int c = (byte) '/'; // same value as in UTF-8
				
				
				int off = 0;
				while(off < n && c != outBuffer[off]) off++;
				if(off == n) {
					off = 0; // ==> filename == ""
					// assume there is no filename, if there
					// is no '/' in the first block
				}
				
				byte[] fn = new byte[off];
				System.arraycopy(outBuffer, 0, fn, 0, off);
				filename = new String(fn, "UTF8");
				
			 // write the rest to plaintext
				plaintext.write(outBuffer, off + 1, n - off);
			}
			
			{// read and decrypt ciphertext
				int len = ciphertext.read(inBuffer);
				while(-1 != len) {
					byte[] dec = encoder.decode(inBuffer, len);
					int n = cipher.processBytes(dec, 0, dec.length, outBuffer, 0);
					plaintext.write(outBuffer, 0, n);
		
					len = ciphertext.read(inBuffer);
				}
				
				byte[] dec = encoder.finalizeDecode();
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
	    app.deleteFile(INTERNAL_ENC);
	    
	}


	//================================================================================
	// Private Classes
	//================================================================================
	
	public static interface BufferedEncoder {
		byte[] encode(byte[] data, int len);
		byte[] finalizeEncode();
		
		byte[] decode(byte[] data, int len);
		byte[] finalizeDecode();
	}
	
	/**
	 * Allows on-the-fly Base64 en- and decode of a buffered stream.
	 */
	private static class BufferedBase64 implements BufferedEncoder {

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
		
		// internal bufer handling
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
