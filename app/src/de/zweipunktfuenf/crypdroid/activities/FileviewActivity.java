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

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;

import de.zweipunktfuenf.crypdroid.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileviewActivity extends SherlockListActivity {

	//================================================================================
	// Constants
	//================================================================================
	//----  Extra Keys  --------------------------------------------------------------
	public static final String EXTRA_MODE = "mode";
	public static final String EXTRA_ROOT = "root";
	public static final String EXTRA_START_ACTIVITY = "startActivity";
	
	public static final String EXTRA_SELECTED_FILE = "filepath";
	public static final String EXTRA_FILENAME = "filename";

	//----  Extra Params  ------------------------------------------------------------
	public static final short MODE_OPEN = 1;
	public static final short MODE_SAVE = 2;
	
	public static final String EXTERNAL_STORAGE =
		Environment.getExternalStorageDirectory().getPath();


	//================================================================================
	// Properties
	//================================================================================
	private File root;
	private File current;
	private String filename;
	private short mode;
	private Class<Activity> startOnSuccess;

	//================================================================================
	// Application Life Cycle
	//================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fileview);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        Bundle b = getIntent().getExtras();
        if(null != b) {
        	
        	setProperties(b);
        	navigateTo(root);
        	
        } else
        	Log.e("FileviewActivity#onCreate", "Starting Intent does not contain a Bundle.");
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
    
    @Override
    public void onBackPressed() {
    	if(root.equals(current))
    		super.onBackPressed();
    	else
    		navigateTo(current.getParentFile());
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	if(0 == position && mode == MODE_SAVE) {
    		pollForFilename();
    		return;
    	}
    	
    	File selected = (File) l.getItemAtPosition(position);
    	if(selected.isDirectory()) {
    		navigateTo(selected);
    	} else {
    		onFinish(selected);
    	}
    }

	//================================================================================
	// Private Methods
	//================================================================================
	@SuppressWarnings("unchecked")
	private void setProperties(Bundle b) {
		String path = b.getString(EXTRA_ROOT);
		if(null != path) root = new File(path);
		else Log.e("FileviewActivity#setProberties", "Starting Intent does not contain a root");
		
		mode = b.getShort(EXTRA_MODE);
		if(0 == mode)
			Log.e("FileviewActivity#setProberties", "Starting Intent does not contain a mode");
		
		Serializable clazz = b.getSerializable(EXTRA_START_ACTIVITY);
		if(null != clazz) startOnSuccess = (Class<Activity>) clazz;
		else if(mode != MODE_SAVE)
			Log.e("FileviewActivity#setProberties", "Starting Intent does not contain an Activity to start");
		
		filename = b.getString(EXTRA_FILENAME);
		if(null == filename) filename = "";
	}
	
	private void navigateTo(File dir) {
		current = dir;
		getSupportActionBar().setTitle(
			dir.getPath() + (dir.getPath().endsWith("/") ? "" : "/")
		);

		File[] children = dir.listFiles(new FileFilter() {
			@Override public boolean accept(File f) {
				boolean show = !f.isHidden() && f.canRead();
				
				if(mode == MODE_SAVE)
					show &= f.isDirectory() && f.canWrite();
				
				return show;
			}
		});
		
		if(mode == MODE_SAVE) {
			File[] temp = children;
			children = new File[temp.length + 1];
			children[0] = current;
			System.arraycopy(temp, 0, children, 1, temp.length);
		}
		
		setListAdapter(new FileAdapter(this, children, mode == MODE_SAVE));
	}
	
	private void onFinish(File selected) {
		Intent i = null != startOnSuccess
			? new Intent(this, startOnSuccess)
			: new Intent();
		
		i.putExtra(EXTRA_SELECTED_FILE, selected.getPath());
		i.putExtra(EXTRA_FILENAME, filename);
		
		switch(mode) {
			case MODE_OPEN:
				startActivity(i);
				break;
			case MODE_SAVE:
				setResult(Activity.RESULT_OK, i);
				finish();
				break;
		}
	}
	
	private void pollForFilename() {
		final EditText input = new EditText(this);
		input.setText(filename);
		new AlertDialog.Builder(this)
		    .setTitle(R.string.save_dialog_title)
		    .setMessage(R.string.save_dialog_message)
		    .setView(input)
		    .setPositiveButton(R.string.save_dialog_ok, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		            filename = input.getText().toString();
		            onFinish(current); // go to next activity
		        }
		    }).setNegativeButton(R.string.save_dialog_cancel, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	// canceled --> do nothing
		        }
		    }).show();
	}

	//================================================================================
	// Private Classes
	//================================================================================
    private static class FileAdapter extends ArrayAdapter<File> {
    	private static final int ROW_LAYOUT = R.layout.fileview_row;
    	
    	private boolean hasChooseThisHeader;
    	
    	public FileAdapter(Context context, File[] data, boolean save) {
			super(context, ROW_LAYOUT, data);
			hasChooseThisHeader = save;
		}
    	
    	@Override
    	public View getView(int position, View view, ViewGroup parent) {
    		FileListItem item;
    		
    		// create a view for the first item, then reuse it
    		if(null == view) {
    			LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
    			
    			view = inflater.inflate(ROW_LAYOUT, parent, false);
    			
    			item = new FileListItem(
    				(ImageView) view.findViewById(R.id.file_icon),
    				(TextView) view.findViewById(R.id.file_text)
    			);
    			
    			view.setTag(item);
    			
    		} else {
    			item = (FileListItem) view.getTag();
    		}
    		
    		File file = getItem(position);
    		if(0 == position && hasChooseThisHeader) {
    			item.iconView.setImageResource(R.drawable.ic_arrow_right);
    			item.textView.setText(R.string.fileview_save_header_text);
    		} else if(file.isDirectory()) {
    			item.iconView.setImageResource(R.drawable.ic_folder_full);
    			item.textView.setText(file.getName() + "/");
    		} else {
    			item.iconView.setImageResource(R.drawable.ic_file_photo);
    			item.textView.setText(file.getName());
    		}
    		
    		return view;
    	}
    	
    	private static class FileListItem {
        	private final ImageView iconView;
        	private final TextView textView;

			private FileListItem(ImageView icon, TextView text) {
				iconView = icon;
				textView = text;
			}
        }
    }

}
