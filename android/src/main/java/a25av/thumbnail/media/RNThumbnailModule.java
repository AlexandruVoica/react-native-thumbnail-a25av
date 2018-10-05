
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

      Context context = getReactApplicationContext();

      Uri fileUri = Uri.parse(filePath);

      String fullPath = Environment.getExternalStorageDirectory() + "/thumb";
      Log.d("RNThumbnail", fullPath);
      String fileName = "thumb-" + getMD5(filePath) + ".jpeg";

      File cache = new File(fullPath, fileName);

      if (cache.exists()) {
        Integer cacheSize = Integer.parseInt(String.valueOf(cache.length()));
        Log.d("RNThumbnail", cache + " exists");
        Log.d("RNThumbnail", cache + " size " + cacheSize);

        if (cacheSize == 0) {
          Boolean fileDeleted = cache.delete();
          if (fileDeleted) {
            Log.d("RNThumbnail", "file deleted");
          } else {
            Log.d("RNThumbnail", "file was NOT deleted");
          }
        } else {
          WritableMap map = Arguments.createMap();
          map.putString("path", (Uri.fromFile(cache)).toString());
          promise.resolve(map);
          return;
        }
      }

      MediaMetadataRetriever retriever = new MediaMetadataRetriever();

      try {
        if (filePath.startsWith("http")) {
          retriever.setDataSource(filePath, new HashMap<String, String>());
        } else {
          retriever.setDataSource(context, fileUri);
        }
      } catch (Exception e) {
        Log.d("RNThumbnail", "MediaMetadataRetriever exception");
      }

      Bitmap image = retriever.getFrameAtTime(100000, MediaMetadataRetriever.OPTION_CLOSEST);

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

      WritableMap map = Arguments.createMap();

      map.putString("path", (Uri.fromFile(file)).toString());
      map.putDouble("width", image.getWidth());
      map.putDouble("height", image.getHeight());
      map.putInt("size", Integer.parseInt(String.valueOf(file.length())));

      promise.resolve(map);

		} catch (Exception e) {
			Log.d("RNThumbnail", "exception " + e, e);
			promise.reject("E_RNThumnail_ERROR", "Exception " + e);
		}
    }
}
