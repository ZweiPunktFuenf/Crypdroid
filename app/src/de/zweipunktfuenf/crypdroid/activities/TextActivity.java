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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.zweipunktfuenf.crypdroid.R;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class TextActivity extends SherlockActivity {

	//================================================================================
	// Constants
	//================================================================================
	//----  Extra Keys  --------------------------------------------------------------
	public static final String EXTRA_MODE = "mode";
	public static final String EXTRA_TEXT = "text";

	//----  Extra Params  ------------------------------------------------------------
	public static final short MODE_WRITE = 1;
	public static final short MODE_SHOW = 2;
	

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
	    setContentView(R.layout.activity_text);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    Bundle b = getIntent().getExtras();
	    if(null != b) {
	    	
	    	setProperties(b);
	    	init();
	    	
	    } else {
        	Log.e(this.getClass().getSimpleName(),
        		"Starting Intent does not contain a Bundle."
        	);
        }
	}

	
	//================================================================================
	// UI Event Listener
	//================================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onClickNext(View clicked) {
    	String text = ((EditText) findViewById(R.id.edit_text)).getText().toString();
    	
    	Intent crypter = new Intent(this, CrypterActivity.class);
    	Bundle extras = new Bundle();
    	extras.putString(EXTRA_TEXT, text);
    	crypter.putExtras(extras);
    	startActivity(crypter);
    }
    
    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void onClickCopy(View clicked) {
    	String text = ((EditText) findViewById(R.id.edit_text)).getText().toString();
    	
    	if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
    	    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    	    clipboard.setText(text);
    	} else {
    	    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
    	    android.content.ClipData clip = android.content.ClipData.newPlainText("Crypdroidtext", text);
    	    clipboard.setPrimaryClip(clip);
    	}
    	
    	Toast.makeText(this, R.string.notify_text_copied, Toast.LENGTH_SHORT).show();
    }

    
	//================================================================================
	// Private Methods
	//================================================================================
	private void setProperties(Bundle b) {
		mode = b.getShort(EXTRA_MODE);
		if(0 == mode) Log.e(this.getClass().getSimpleName(),
			"Starting Intent does not contain a mode"
		);
		if(MODE_SHOW == mode) {
			((EditText) findViewById(R.id.edit_text)).setText(b.getString(EXTRA_TEXT));
		}
	}
	
	private void init() {
		switch(mode) {
			case MODE_SHOW:
				findViewById(R.id.button_next).setVisibility(View.GONE);
				break;
			case MODE_WRITE:
				findViewById(R.id.button_copy).setVisibility(View.GONE);
				break;
		}
	}

	//================================================================================
	// Private Classes
	//================================================================================

}
