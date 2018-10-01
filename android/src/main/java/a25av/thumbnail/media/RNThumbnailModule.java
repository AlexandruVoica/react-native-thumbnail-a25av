
package a25av.thumbnail.media;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.media.MediaMetadataRetriever;

import android.Manifest;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import android.content.Context;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;


public class RNThumbnailModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNThumbnailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNThumbnail";
  }

  public static String getMD5(String string) {
      byte[] hash;

      try {
          hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
      } catch (NoSuchAlgorithmException e) {
          e.printStackTrace();
          return null;
      } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return null;
      }

      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
          if ((b & 0xFF) < 0x10)
              hex.append("0");
          hex.append(Integer.toHexString(b & 0xFF));
      }

      return hex.toString();
  }


  @ReactMethod
  public void get(String filePath, Promise promise) {
  	try {
		if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
			Log.e("RNThumbnail", "permission to READ_EXTERNAL_STORAGE not granted");
		} else {
			Log.d("RNThumbnail", "permission to READ_EXTERNAL_STORAGE granted");
		}

		if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
			Log.e("RNThumbnail", "permission to WRITE_EXTERNAL_STORAGE not granted");
		} else {
			Log.d("RNThumbnail", "permission to WRITE_EXTERNAL_STORAGE granted");
		}

		File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		Log.d("RNThumbnail", storageDir.toString());

		Uri fileUri = Uri.parse(filePath);

		File videoFile = new File(fileUri.getPath());

		// File.exists() can throw exceptions if there's a security problem
		try {
			Boolean fileExists = videoFile.exists();
		} catch (Exception e) {
			Log.e("RNThumbnail", "file exists exception" + e);
		}

		// if file does not exist log the error
		if(fileExists || videoFile.canRead()) {
			Log.e("RNThumbnail", videoFile + " file does not exist");
		}

		// filePath = filePath.replace("content://","");

	    String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/thumb";
	    String fileName = "thumb-" + getMD5(filePath) + ".jpeg";

		File cache = new File(fullPath, fileName);

	    if (cache.exists()) {
	      WritableMap map = Arguments.createMap();
		  map.putString("path", "content://" + fullPath + '/' + fileName);
		  promise.resolve(map);
	      return;
	    }

		MediaMetadataRetriever retriever = new MediaMetadataRetriever();

		try {
			if (filePath.startsWith("http")) {
				retriever.setDataSource(filePath, new HashMap<String, String>());
			} else {
				Log.d("RNThumbnail", filePath);
				retriever.setDataSource(filePath);
				Log.d("RNThumbnail", retriever.toString());
			}
		} catch (Exception e) {
			Log.d("RNThumbnail", "MediaMetadataRetriever exception " + e);
		}

	    Bitmap image = retriever.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST);

		File dir = new File(fullPath);

		if (!dir.exists()) {
			dir.mkdirs();
		}

		OutputStream fOut = null;
		// String fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg";

		File file = new File(fullPath, fileName);
		file.createNewFile();
		fOut = new FileOutputStream(file);

		// 100 means no compression, the lower you go, the stronger the compression
		image.compress(Bitmap.CompressFormat.JPEG, 60, fOut);
		fOut.flush();
		fOut.close();

		// MediaStore.Images.Media.insertImage(reactContext.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

		WritableMap map = Arguments.createMap();

		map.putString("path", "content://" + fullPath + '/' + fileName);
		map.putDouble("width", image.getWidth());
		map.putDouble("height", image.getHeight());

		Log.d("RNThumbnail", map.toString());

		promise.resolve(map);

		} catch (Exception e) {
			Log.d("RNThumbnail", "exception " + e, e);
			promise.reject("E_RNThumnail_ERROR", "Exception " + e);
		}
    }
}
