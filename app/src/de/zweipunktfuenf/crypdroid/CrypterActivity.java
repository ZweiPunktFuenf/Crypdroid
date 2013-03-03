package de.zweipunktfuenf.crypdroid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import org.spongycastle.crypto.InvalidCipherTextException;
import com.actionbarsherlock.app.SherlockActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
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
	private String filename;
	private short mode;

	
	//================================================================================
	// Application Life Cycle
	//================================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_crypter);
	    // the Home Button does not work on this Activity, therefore don't show it
//		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    setProperties(getIntent());
	    
	    init();
	}

	
	//================================================================================
	// UI Event Listener
	//================================================================================
	public void onClickEncrypt(View clicked) {
		try {
			Crypter.encrypt(data, filename, getPassword(), this);
			
			Intent ac = new Intent(this, ActionChooserActivity.class);
			ac.putExtra(ActionChooserActivity.EXTRA_MODE,
				ActionChooserActivity.MODE_CIPHER
			);
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
		try {
			String filename = Crypter.decrypt(data, getPassword(), this, mode == MODE_TEXT);
			
			Intent ac = new Intent(this, ActionChooserActivity.class);
			ac.putExtra(ActionChooserActivity.EXTRA_FILENAME, filename);
			ac.putExtra(ActionChooserActivity.EXTRA_MODE,
				ActionChooserActivity.MODE_PLAIN
			);
			startActivity(ac);
		} catch (InvalidCipherTextException e) {
			Log.e("decrypt", "ungültiger Ciphertext", e);
			Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e("decrypt", "Fehler beim Schreiben der internen Dateien", e);
			Toast.makeText(this, R.string.error_internal_io, Toast.LENGTH_LONG).show();
		} catch (InvalidParameterException e) {
			Log.e("encrypt", "ungültiger Ciphertext", e);
			Toast.makeText(this, R.string.error_not_encrypted, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Log.e("encrypt", "ungültiger Ciphertext", e);
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
			
			Uri uri = i.getData();
			filename = "";
			
			try { data = getContentResolver().openInputStream(uri); }
			catch (FileNotFoundException e) {
				Log.e(this.getClass().getSimpleName(),
					"file to view does not exist",
					e
				);
			}
			
		} else if(b.containsKey(FileviewActivity.EXTRA_SELECTED_FILE)) {

			// got started by FileviewActivity
			mode = MODE_FILE;
			
			String path = b.getString(FileviewActivity.EXTRA_SELECTED_FILE);
			filename = path.substring(path.lastIndexOf("/") + 1);
			
			try { data = new FileInputStream(path); }
			catch (FileNotFoundException e) {
				Log.e(this.getClass().getSimpleName(),
					"selected file does not exist",
					e
				);
			}
			
		} else {
			
			// got started by TextActivity
			mode = MODE_TEXT;
			
			filename = null;
			
			try { data = openFileInput(Crypter.INTERNAL_IN); }
			catch (FileNotFoundException e) {
				Log.e(this.getClass().getSimpleName(),
					"internal file was not created",
					e
				);
			}
			
		}
	}
	
	private void init() {
		switch(mode) {
			case MODE_FILE:
				((ImageView) findViewById(R.id.image_data))
					.setImageResource(R.drawable.ic_file_photo);
				((TextView) findViewById(R.id.text_data_large))
					.setText(filename);
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
	

	//================================================================================
	// Private Classes
	//================================================================================
	
}
