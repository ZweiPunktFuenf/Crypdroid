package de.zweipunktfuenf.crypdroid;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class StartActivity extends SherlockActivity {

	//================================================================================
	// Constants
	//================================================================================
	

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
	    Crypter.clearInternalFiles(this);
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

	//================================================================================
	// Private Methods
	//================================================================================
	

	//================================================================================
	// Private Classes
	//================================================================================
	

}
