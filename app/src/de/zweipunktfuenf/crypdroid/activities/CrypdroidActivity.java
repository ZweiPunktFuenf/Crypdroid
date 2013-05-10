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

import de.zweipunktfuenf.crypdroid.R;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class CrypdroidActivity extends SherlockActivity {

	//================================================================================
	// Constants
	//================================================================================
	public static final String FILE_TEMP_OUT = "temp_out";

	private static final int REQUEST_LOAD_IMAGE = 1;

	//================================================================================
	// Public Methods
	//================================================================================
	protected static void clearInternalData(Context app) {
		app.deleteFile(FILE_TEMP_OUT);
	}
	
	//================================================================================
	// Properties
	//================================================================================
	

	//================================================================================
	// Application Life Cycle
	//================================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_start);
	    ((TextView) findViewById(R.id.text_help)).setMovementMethod(LinkMovementMethod.getInstance());
	    
	    // clear internal files on startup,
	    // just in case it wasn't shut down probably
	    clearInternalData(this);
	}

	//================================================================================
	// UI Event Listener
	//================================================================================
	public void onClickFile(View clicked) {
		Intent fv = new Intent(this, FileviewActivity.class);
		
		fv.putExtra(FileviewActivity.EXTRA_ROOT, "/");
		fv.putExtra(FileviewActivity.EXTRA_MODE, FileviewActivity.MODE_OPEN);
		fv.putExtra(FileviewActivity.EXTRA_START_ACTIVITY, CrypterActivity.class);
		
		startActivity(fv);
	}
	
	public void onClickText(View clicked) {
		Intent text = new Intent(this, TextActivity.class);
		
		text.putExtra(TextActivity.EXTRA_MODE, TextActivity.MODE_WRITE);
		
		startActivity(text);
	}
	
	//TODO enable for user
    public void onClickImage(View Clicked) {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
        	case REQUEST_LOAD_IMAGE: if(RESULT_OK == resultCode) {
        		if(null == data) return;
        		
        		Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                
                Intent ca = new Intent(this, CrypterActivity.class);
                ca.putExtra(FileviewActivity.EXTRA_SELECTED_FILE, picturePath);
                startActivity(ca);
			} break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
        }
    }

	//================================================================================
	// Private Methods
	//================================================================================
	

	//================================================================================
	// Private Classes
	//================================================================================
	

}
