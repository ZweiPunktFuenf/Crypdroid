package de.zweipunktfuenf.crypdroid;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
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
    	
		try {
			FileOutputStream out = openFileOutput(
				Crypter.INTERNAL_IN,
				Context.MODE_PRIVATE  // !important
			);
			
			out.write(text.getBytes("UTF8"));
			out.close();
			
	    	// if saved successfully:
	    	Intent crypter = new Intent(this, CrypterActivity.class);
	    	crypter.putExtras(new Bundle());
	    	startActivity(crypter);	
	    	
		} catch (IOException e) {
			Log.e(this.getClass().getSimpleName(),
				"error while saving text to internal storage",
				e
			);
			Toast.makeText(getApplicationContext(),
				R.string.error_internal,
				Toast.LENGTH_LONG
			).show();
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
	}
	
	private void init() {
		switch(mode) {
			case MODE_SHOW:
				findViewById(R.id.button_next).setVisibility(View.GONE);
				
				try {
					FileInputStream in = openFileInput(Crypter.INTERNAL_OUT);
					StringBuffer str = new StringBuffer();

					int len;
					byte[] buffer = new byte[1024];
					while(-1 != (len = in.read(buffer)))
					    str.append(new String(buffer, 0, len));
					
					((EditText) findViewById(R.id.edit_text)).setText(str);
//					Log.i("cryptext", str.toString());
				} catch(IOException e) {
					Log.e(this.getClass().getSimpleName(),
						"error while reading internal_out",
						e
					);
					
					((EditText) findViewById(R.id.edit_text))
						.setText(R.string.error_internal);
				}
				
				
				break;
			case MODE_WRITE:
		        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				break;
		}
	}

	//================================================================================
	// Private Classes
	//================================================================================
	

}
