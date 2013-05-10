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
package de.zweipunktfuenf.crypdroid.activities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.zweipunktfuenf.crypdroid.R;
import de.zweipunktfuenf.crypdroid.provider.CrypProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ActionChooserActivity extends SherlockActivity {

	//================================================================================
	// Constants
	//================================================================================
	//----  Extra Keys  --------------------------------------------------------------
	public static final String EXTRA_MODE = "mode";

	//----  Extra Params  ------------------------------------------------------------
	public static final short MODE_CIPHER = 1;
	public static final short MODE_PLAIN = 2;

	//----  Internal  ----------------------------------------------------------------
	private static final int REQUEST_SAVE = 1;
	private static final String CIPHER_MIME_TYPE = "application/crypdroid";

	
	//================================================================================
	// Properties
	//================================================================================
	private short mode;
	

	//================================================================================
	// Application Life Cycle
	//================================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_action_chooser);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    Bundle b = getIntent().getExtras();
	    if(null != b) {
	    	
	    	setProperties(b);
	    	init();
	    	
	    }
	}
	
	@Override
	protected void onDestroy() {
		// cannot rely on this method getting called (see Javadoc)
		Log.d(this.getClass().getSimpleName(), "cleaning internal files");
		
		CrypdroidActivity.clearInternalData(this);
		
		super.onDestroy();
	}

	//================================================================================
	// UI Event Listener
	//================================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	public void onClickSend(View clicked) {
		Intent send = new Intent(android.content.Intent.ACTION_SEND);
		send.setType("text/plain");
		send.putExtra(android.content.Intent.EXTRA_TEXT, new String(readDataFile()));
		startActivity(Intent.createChooser(send,
			getString(R.string.send_with)
		));
	}
	
	public void onClickSendAttachment(View clicked) {
		Intent send = new Intent(Intent.ACTION_SEND);
		send.putExtra(Intent.EXTRA_STREAM, Uri.parse(CrypProvider.URI + CrypProvider.FILE_ENCRYPTED));
		send.setType(CIPHER_MIME_TYPE);
		
		// can't use this, because FLAG_GRANT_READ_URI_PERMISSION only grants read permission
		// for the URI in setData and setData sets the receiver for an email
//		send.setData(Uri.parse(CrypProvider.URI));
//		send.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		// for API 16 and greater it should be possible to use
		// setClipData instead of setData and therefore avoid the problem
		// since FLAG_GRANT_READ_URI_PERMISSION also grants permissions
		// for URIs included in the Intent as clip-data
//		send.setClipData(new ClipData(
//			"data_uri",
//			new String[] {"text/plain"},
//			new ClipData.Item(Uri.parse(CrypProvider.URI))
//		));
		
		// cannot manually grant the read permission because I don't know the
		// third party App URI in advance
//		getApplicationContext().grantUriPermission("com.package.viewer", Uri.parse(CrypProvider.URI), Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		startActivity(Intent.createChooser(send,
			getString(R.string.send_with)
		));
	}
	
	public void onClickShow(View clicked) {
		Intent show = null;
		
		switch(mode) {
			case MODE_CIPHER:
				
				show = new Intent(this, TextActivity.class);
			    show.putExtra(TextActivity.EXTRA_MODE, TextActivity.MODE_SHOW);
			    show.putExtra(TextActivity.EXTRA_TEXT, new String(readDataFile()));
			    startActivity(show);
		    	break;
		    	
			case MODE_PLAIN:
				
//				if(null == mimeType) {
					// no known mime type, display as text
					show = new Intent(this, TextActivity.class);
					show.putExtra(TextActivity.EXTRA_MODE, TextActivity.MODE_SHOW);
				    show.putExtra(TextActivity.EXTRA_TEXT, new String(readDataFile()));
			    	startActivity(show);
//				} else {
//					show = new Intent(android.content.Intent.ACTION_VIEW);
//					show.setDataAndType(Uri.parse(CrypProvider.URI + CrypdroidActivity.FILE_TEMP_OUT), mimeType);
//					startActivity(show);
//				}
				break;
				
		}
	}
	
	public void onClickSave(View clicked) {
		Intent fv = new Intent(this, FileviewActivity.class);
		
		fv.putExtra(FileviewActivity.EXTRA_MODE, FileviewActivity.MODE_SAVE);
		fv.putExtra(FileviewActivity.EXTRA_ROOT, FileviewActivity.EXTERNAL_STORAGE);
		fv.putExtra(FileviewActivity.EXTRA_FILENAME,
			System.currentTimeMillis() + (mode == MODE_PLAIN ? ".txt" : ".crypdroid")
		);
		
		startActivityForResult(fv, REQUEST_SAVE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case REQUEST_SAVE: if(RESULT_OK == resultCode) {
				File file;
				{// find unique filename
					String dir = (String) data.getExtras()
						.getSerializable(FileviewActivity.EXTRA_SELECTED_FILE)
						+ "/";
					
//					String fname = (null == filename || filename.isEmpty())
//						? "" + System.currentTimeMillis()
//						+ (mode == MODE_PLAIN ? ".txt" : "")
//						: filename;
					String fname = data.getExtras().getString(FileviewActivity.EXTRA_FILENAME);
					
					String[] parts = fname.split("\\.(?=[^\\.]+$)");
					String fn = parts[0];
					String ext = parts.length == 2 ? parts[1] : "";
					
					String test = fn + (ext.isEmpty() ? "" : ("." + ext));
					file = new File(dir + "/" + test);

					int i = 1;
					while(file.exists()) {
						test = fn + "(" + ++i + ")"
						     + (ext.isEmpty() ? "" : ("." + ext));
						file = new File(dir + "/" + test);
					}
				}
				
				FileInputStream fis = null;
				FileOutputStream fos = null;
				try {
					file.createNewFile();
					fos = new FileOutputStream(file);
					fis = openFileInput(CrypdroidActivity.FILE_TEMP_OUT);

					int n;
					byte[] buffer = new byte[1024];
					while(-1 < (n = fis.read(buffer))) fos.write(buffer, 0, n);

					Toast.makeText(this,
						R.string.file_saved,
						Toast.LENGTH_LONG
					).show();
					
				} catch(FileNotFoundException e) {
					Toast.makeText(this, R.string.error_internal, Toast.LENGTH_LONG).show();
					Log.e("ActionChooserActivity#onActivityResult", "" + e.getClass(), e);
				} catch (IOException e) {
					Toast.makeText(this, R.string.error_internal, Toast.LENGTH_LONG).show();
					Log.e("ActionChooserActivity#onActivityResult", "" + e.getClass(), e);
				} finally {
					if(null != fis) try { fis.close(); } catch(IOException e) {
						Log.d("ActionChooserActivity#onActivityResult", "error while closing internal_out", e);
					}
					if(null != fos) try { fos.close(); } catch(IOException e) {
						Log.d("ActionChooserActivity#onActivityResult", "error while closing file output", e);
					}
				}
			} break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	//================================================================================
	// Private Methods
	//================================================================================
	private void setProperties(Bundle b) {
		mode = b.getShort(EXTRA_MODE);
		if(0 == mode) Log.e(this.getClass().getSimpleName(),
			"Starting Intent does not contain a mode"
		);
//		
//		filename = b.getString(EXTRA_FILENAME);
//		if(mode == MODE_PLAIN && null == filename)
//			Log.e(this.getClass().getSimpleName(),
//				"Starting Intent does not contain a filename"
//			);
//		
//		if(null != filename) {
//			// tried to use MimeTypeMap.getFileExtensionFromUrl()
//			// but this can only be used for URLs, see:
//			// http://code.google.com/p/android/issues/detail?id=5510
//			String ext = filename.substring(filename.lastIndexOf(".") + 1);
//			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
//			
//			Log.d(this.getClass().getSimpleName(), filename + ": " + ext + ": " + mimeType);
//		}
	}
	
	private void init() {
		switch(mode) {
			case MODE_CIPHER:
				((ImageView) findViewById(R.id.image_data))
					.setImageResource(R.drawable.ic_file_encrypted);
				((TextView) findViewById(R.id.text_data_large)).setText(R.string.text_data_cipher_large);
				((TextView) findViewById(R.id.text_data_small)).setText(R.string.text_data_cipher_small);
				break;
			case MODE_PLAIN:
//				if(this.filename.isEmpty()) {
					((ImageView) findViewById(R.id.image_data))
						.setImageResource(R.drawable.ic_text);
					((TextView) findViewById(R.id.text_data_large)).setText(R.string.text_data_plain_text_large);
					((TextView) findViewById(R.id.text_data_small)).setText(R.string.text_data_plain_text_small);
//				} else {
//					((ImageView) findViewById(R.id.image_data))
//						.setImageResource(R.drawable.ic_file_photo);
//					((TextView) findViewById(R.id.text_data_large)).setText(filename);
//					((TextView) findViewById(R.id.text_data_small)).setText(R.string.text_data_plain_file_small);
//				}
				
				findViewById(R.id.button_send).setVisibility(View.GONE);
				findViewById(R.id.button_send_as_attachment).setVisibility(View.GONE);
				findViewById(R.id.text_warn).setVisibility(View.GONE);
				break;
		}
	}
	
	@Deprecated
	private byte[] readDataFile() {
		FileInputStream fis = null;
		try {
			fis = openFileInput(CrypdroidActivity.FILE_TEMP_OUT);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			int size;
			byte[] buffer = new byte[4096];
			while(-1 != (size = fis.read(buffer))) baos.write(buffer, 0, size);
			
			baos.flush();
			return baos.toByteArray();
			
		} catch(FileNotFoundException e) {
			Toast.makeText(this, R.string.error_internal, Toast.LENGTH_LONG).show();
			Log.e("ActionChooserActivity#readDataFile", "" + e.getClass(), e);
			return null;
		} catch (IOException e) {
			Toast.makeText(this, R.string.error_internal, Toast.LENGTH_LONG).show();
			Log.e("ActionChooserActivity#readDataFile", "" + e.getClass(), e);
			return null;
		} finally {
			if(null != fis) try { fis.close(); }
			catch (IOException e) {
				Log.d("ActionChooserActivity#readDataFile", "error while closing internal_out", e);
			}
		}
	}

	//================================================================================
	// Private Classes
	//================================================================================
	

}
