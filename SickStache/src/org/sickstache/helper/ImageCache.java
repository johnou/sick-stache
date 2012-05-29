/*
 * 	SickStache is a android application for managing SickBeard
 * 	Copyright (C) 2012  David Stocking dmstocking@gmail.com
 * 
 * 	http://code.google.com/p/sick-stashe/
 * 	
 * 	SickStache is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 * 	
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 * 	
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sickstache.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.common.collect.MapMaker;

public class ImageCache {
	

	// there is already a folder named cache lol
//	public static final String cacheName = "cache";
	private static final String cacheLogName = "ImageCache";

	public static ImageCache cache;
	
	private Context c;
	private File cacheDir;
	private ConcurrentMap<String,Bitmap> memCache;
	
	public ImageCache( Context c )
	{
		this.c = c;
		this.cacheDir = c.getExternalCacheDir();
		MapMaker maker = new MapMaker();
		maker.initialCapacity(40);
		maker.concurrencyLevel(4);
		maker.softValues();
		this.memCache = maker.makeMap();
	}
	
	public boolean in( String key )
	{
		return inMem(key) || inDisk(key);
	}
	
	public boolean inMem( String key )
	{
		String filename = sanatizeKey(key);
		if ( memCache.containsKey(filename) ) {
			return true;
		}
		return false;
	}
	
	public boolean inDisk( String key )
	{
		String filename = sanatizeKey(key);
		try {
			if ( cacheDir.canRead() ) {
				File tmp = new File( cacheDir, filename );
				return tmp.exists();
//				FileInputStream in = new FileInputStream( tmp );
////				boolean exists = tmp.exists();
//				return true;
			}
		} catch (Exception e) {
			Log.e(cacheLogName, "Error finding if \"" + filename + "\" exists on the disk. ERROR:" + e.getMessage());
		}
		return false;
	}
	
	public boolean add( String key, Bitmap bitmap )
	{
		boolean ret = false;
		String filename = sanatizeKey(key);
		if ( memCache.containsKey(filename) == false ) {
			memCache.put(filename, bitmap);
			ret = true;
		}
		try {
			File tmp = new File( cacheDir, filename );
			if ( tmp.exists() == false ) {
				FileOutputStream out = new FileOutputStream( tmp );
				bitmap.compress(CompressFormat.PNG, 90, out);
				out.close();
				ret = true;
				Log.e(cacheLogName, "Added file \"" + filename + "\" to disk cache.");
			} else {
				Log.e(cacheLogName, "File already existed in cache.");
			}
		} catch (Exception e) {
			Log.e(cacheLogName, "Error adding File. ERROR:" + e.getMessage());
		}
		return ret;
	}
	
	public Bitmap get( String key )
	{
		String filename = sanatizeKey(key);
		if ( memCache.containsKey(filename) ) {
			return memCache.get(filename);
		} else {
			try {
				File dir = c.getExternalCacheDir();
				File tmp = new File( dir.getAbsolutePath(), filename );
				if ( tmp.exists() ) {
					FileInputStream in = new FileInputStream( tmp.getAbsolutePath() );
					// get from disk
					Bitmap map = BitmapFactory.decodeStream(in);
					in.close();
					// re-add to the cache
					memCache.put(key, map);
					return map;
				} else {
					Log.e(cacheLogName, "File \"" + tmp.getAbsolutePath() + "\" does not exist.");
				}
			} catch (Exception e) {
				Log.e(cacheLogName, "Problem getting file. ERROR: " + e.getMessage());
			}
		}
		return null;
	}
	
	public boolean remove( String key )
	{
		String filename = sanatizeKey(key);
		if ( memCache.containsKey(filename) ) {
			memCache.remove(filename);
		}
		try {
			File tmp = new File( cacheDir, filename );
			if ( tmp.exists() ) {
				if ( tmp.delete() ) {
					Log.i(cacheLogName, "Deleted the file \"" + filename + "\" from the disk cache.");
				} else {
					Log.e(cacheLogName, "Failed to delete file \"" + filename + "\" from the disk cache.");
				}
			} else {
				Log.e(cacheLogName, "File did not existed in cache and was not deleted.");
			}
		} catch (Exception e) {
			Log.e(cacheLogName, "Error deleting File. ERROR:" + e.getMessage());
		}
		return true;
	}
	
	public void clear()
	{
		clearMem();
		clearDisk();
	}
	
	public void clearMem()
	{
		memCache.clear();
	}

	public void clearDisk()
	{
		try {
			File dir = c.getExternalCacheDir();
			for ( File f : dir.listFiles() ) {
				if ( f.isFile() ) {
					f.delete();
				}
			}
		} catch (Exception e) {
			Log.e(cacheLogName, "Unable to clear cache. ERROR: " + e.getMessage() );
		}
	}
	
	private String sanatizeKey(String key) {
		return key.replaceAll("[.:/,%?&=]", "_").replaceAll("_+", "_");
	}
}
