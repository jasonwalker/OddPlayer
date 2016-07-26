package com.jmw.rd.oddplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.Storage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImageHolder {
    private static final int RESCALE_MAX_SIZE = 250;

    private static final Map<String, Bitmap> images = new HashMap<>();

    private static Bitmap rescale(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        if (height == 0 || width == 0) {
            return null;
        }
        if (height > width) {
            return Bitmap.createScaledBitmap(bitmap, (int) ((((float) width) / height) * RESCALE_MAX_SIZE), RESCALE_MAX_SIZE, false);
        } else {
            return Bitmap.createScaledBitmap(bitmap, RESCALE_MAX_SIZE, (int) ((((float) height) / width) * RESCALE_MAX_SIZE), false);
        }
    }

    public static Bitmap getImageFromFeedUrl(Storage storage, String url)  {
        try {
            Bitmap bitmap = null;
            if (!images.containsKey(url)) {
                File imageFile = storage.getFeedImageFile(url);
                if (imageFile != null) {
                    bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                }
                if (bitmap == null) {
                    images.put(url, null);
                } else {
                    bitmap = rescale(bitmap);
                    images.put(url, bitmap);
                }
            } else {
                bitmap = images.get(url);
            }
            return bitmap;
        } catch (ResourceAllocationException e){
            return null;
        }
    }

    public static Bitmap getImageResource(Context context, int resource) {
        String resourceString = Integer.toString(resource);
        if (!images.containsKey(resourceString)) {
            Bitmap image = BitmapFactory.decodeResource(context.getResources(), resource);
            images.put(resourceString, image);
        }
        return images.get(resourceString);
    }

}