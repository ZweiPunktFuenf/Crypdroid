/**
 * Copyright 2013 Felix Große
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
package de.zweipunktfuenf.crypdroid.activities;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import org.spongycastle.crypto.InvalidCipherTextException;
import com.actionbarsherlock.app.SherlockActivity;

import de.zweipunktfuenf.crypdroid.R;
import de.zweipunktfuenf.crypdroid.mode.StringMode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CrypterActivity extends SherlockActivity {

	//================================================================================
	// Constants
	//================================================================================
	private static final short MODE_CIPHER = 1;
	private static final short MODE_TEXT = 2;
	private static final short MODE_FILE = 3;

	
	//================================================================================
	// Properties
	//================================================================================
	private InputStream data;
	private short mode;

	
	//================================================================================
	// Application Life Cycle
	//================================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_crypter);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    setProperties(getIntent());
	    
	    init();
	}

	
	//================================================================================
	// UI Event Listener
	//================================================================================
	public void onClickEncrypt(View clicked) {
		CrypdroidActivity.clearInternalData(this);
		
		try {
			StringMode.instance().encrypt(data, getPassword(), openOutput());
			
			Intent ac = new Intent(this, ActionChooserActivity.class);
			ac.putExtra(ActionChooserActivity.EXTRA_MODE, ActionChooserActivity.MODE_CIPHER);
			startActivity(ac);
		} catch (InvalidCipherTextException e) {
			Log.e("encrypt", "ungültiger Klartext", e);
			Toast.makeText(this, R.string.error_invalid_plaintext, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e("encrypt", "Fehler beim Schreiben der internen Dateien", e);
			Toast.makeText(this, R.string.error_internal_io, Toast.LENGTH_LONG).show();
		} catch (InvalidParameterException e) {
			Log.e("encrypt", "ungültiger Klartext", e);
			Toast.makeText(this, R.string.error_invalid_plaintext, Toast.LENGTH_LONG).show();
		}
	}
	
	public void onClickDecrypt(View clicked) {
		CrypdroidActivity.clearInternalData(this);
		
		try {
			StringMode.instance().decrypt(data, getPassword(), openOutput());
			
			Intent ac = new Intent(this, ActionChooserActivity.class);
			ac.putExtra(ActionChooserActivity.EXTRA_MODE, ActionChooserActivity.MODE_PLAIN);
			startActivity(ac);
		} catch (InvalidCipherTextException e) {
			Log.e("CrypterActivity#onClickDecrypt", "ungültiger Ciphertext", e);
			Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e("CrypterActivity#onClickDecrypt", "Fehler beim Schreiben der internen Dateien", e);
			Toast.makeText(this, R.string.error_internal_io, Toast.LENGTH_LONG).show();
		} catch (InvalidParameterException e) {
			Log.e("CrypterActivity#onClickDecrypt", "ungültiger Ciphertext", e);
			Toast.makeText(this, R.string.error_not_encrypted, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Log.e("CrypterActivity#onClickDecrypt", "ungültiger Ciphertext", e);
			Toast.makeText(this, R.string.error_not_encrypted, Toast.LENGTH_LONG).show();
		}
	}
	
	public void onClickShowPassword(View clicked) {
		final CheckBox check = (CheckBox) findViewById(R.id.checkShowPassword);
		final EditText edit  = (EditText) findViewById(R.id.edit_password    );
		
		int start = edit.getSelectionStart();
		int end = edit.getSelectionEnd();
		
		if(check.isChecked())
			edit.setTransformationMethod(new SingleLineTransformationMethod());
		else
			edit.setTransformationMethod(new PasswordTransformationMethod());
		
		edit.setSelection(start, end);
	}

	//================================================================================
	// Private Methods
	//================================================================================
	private void setProperties(Intent i) {
		Bundle b = i.getExtras();
		if(null == b) {
			
			// started as viewer for ciphertext
			mode = MODE_CIPHER;
			
			try { data = getContentResolver().openInputStream(i.getData()); }
			catch (FileNotFoundException e) {
				Log.e("CrypterActivity#setProperties", "file to view does not exist", e);
			}
			
		} else if(b.containsKey(FileviewActivity.EXTRA_SELECTED_FILE)) {

			// got started by FileviewActivity
			mode = MODE_FILE;
			
			try { data = new FileInputStream(b.getString(FileviewActivity.EXTRA_SELECTED_FILE)); }
			catch (FileNotFoundException e) {
				Log.e("CrypterActivity#setProperties", "selected file does not exist", e);
			}
			
		} else if(b.containsKey(TextActivity.EXTRA_TEXT)) {
			
			// got started by TextActivity
			mode = MODE_TEXT;
			data = new ByteArrayInputStream(b.getString(TextActivity.EXTRA_TEXT).getBytes());
			
		} else {
			
			// got started by an unknown way
			Log.e("CrypterActivity#setProperties", "got started via an unrecognized way");
			Toast.makeText(this, R.string.error_internal, Toast.LENGTH_LONG).show();
			
		}
	}
	
	private void init() {
		switch(mode) {
			case MODE_FILE:
				((ImageView) findViewById(R.id.image_data))
					.setImageResource(R.drawable.ic_file_photo);
				((TextView) findViewById(R.id.text_data_large))
					.setText(R.string.text_data_crypter_file_large);
				((TextView) findViewById(R.id.text_data_small))
					.setText(R.string.text_data_crypter_file_small);
				break;
			case MODE_CIPHER:
				((ImageView) findViewById(R.id.image_data))
					.setImageResource(R.drawable.ic_file_encrypted);
				((TextView) findViewById(R.id.text_data_large))
					.setText(R.string.text_data_crypter_encrypted_large);
				((TextView) findViewById(R.id.text_data_small))
					.setText(R.string.text_data_crypter_encrypted_small);
				findViewById(R.id.button_encrypt).setVisibility(View.GONE);
				break;
			case MODE_TEXT:
				// values for text are set by default
				break;
		}
		
		onClickShowPassword(null); //init edit_password state
		
//		findViewById(R.id.edit_password).requestFocus();
		//doesn't show the keyboard, workaround to simulate touch click on the view:
		//http://stackoverflow.com/questions/5105354
		findViewById(R.id.edit_password).postDelayed(new Runnable() {
			@SuppressLint("Recycle") @Override public void run() {
				EditText edit = (EditText) findViewById(R.id.edit_password);
				edit.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
				edit.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));                       
			}
		}, 200);
	}
	
	private String getPassword() {
		return ((EditText) findViewById(R.id.edit_password)).getText().toString();
	}
	
	private OutputStream openOutput() throws FileNotFoundException {
		return openFileOutput(CrypdroidActivity.FILE_TEMP_OUT, Context.MODE_PRIVATE);
	}
	

	//================================================================================
	// Private Classes
	//================================================================================
	
}
