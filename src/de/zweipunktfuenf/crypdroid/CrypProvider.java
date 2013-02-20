package de.zweipunktfuenf.crypdroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class CrypProvider extends ContentProvider {
	
	public static final String URI = "content://de.zweipunktfuenf.crypdroid.crypprovider/file/encrypted.crypdroid";
	public static final String MIME_TYPE = "application/de.zweipunktfuenf.crypdroid";
	
	private static final FilenameFilter FILE_TO_PROVIDE = new FilenameFilter() {
		@Override public boolean accept(File dir, String filename) {
			return Crypter.INTERNAL_OUT.equals(filename);
		}
	};

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
    	String s = uri.toString();
    	String ext = s.substring(s.lastIndexOf(".") + 1);
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return null != mimeType ? mimeType : MIME_TYPE;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
    		throws FileNotFoundException {
    	File[] internals = getContext().getFilesDir().listFiles(FILE_TO_PROVIDE);
    	if(1 != internals.length) {
    		Log.e(this.getClass().getSimpleName(),
				"did not find output file (len="+internals.length+")"
			);
    		throw new FileNotFoundException();
    	}
    	
        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(
    		internals[0],
    		ParcelFileDescriptor.MODE_READ_ONLY
		);
        
        return parcel;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }
}
