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
package de.zweipunktfuenf.crypdroid.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import de.zweipunktfuenf.crypdroid.activities.CrypdroidActivity;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class CrypProvider extends ContentProvider {
	
	public static final String URI = "content://de.zweipunktfuenf.crypdroid.crypprovider/file/";
	public static final String FILE_ENCRYPTED = "encrypted.crypdroid";
	public static final String MIME_TYPE = "application/de.zweipunktfuenf.crypdroid";

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
    public ParcelFileDescriptor openFile(final Uri uri, String mode)
    		throws FileNotFoundException
    {
    	File[] internals = getContext().getFilesDir().listFiles(new FilenameFilter() {
    		@Override public boolean accept(File dir, String filename) {
    			return CrypdroidActivity.FILE_TEMP_OUT.equals(filename);
    		}
    	});
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
