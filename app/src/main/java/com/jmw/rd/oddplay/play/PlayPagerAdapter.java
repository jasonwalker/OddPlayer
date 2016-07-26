package com.jmw.rd.oddplay.play;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;


public class PlayPagerAdapter extends FragmentPagerAdapter {
    private final Storage storage;
    private volatile int bufferedCount = 0;
    private volatile Long[] allIds;

    public PlayPagerAdapter(FragmentManager mgr, Context context) {
        super(mgr);
        this.storage = StorageUtil.getStorage(context);
        new CacheDataTask().execute();
    }


    private class CacheDataTask extends AsyncTask<Void, Object, Void> {
        @Override
        protected Void doInBackground(Void... unused) {
            allIds = storage.getAllIds();
            bufferedCount = allIds.length;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            PlayPagerAdapter.super.notifyDataSetChanged();
        }
    }


    private Bitmap rescale(Bitmap bitmap) {
        final int maxSize = 150;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        if (height == 0 || width == 0) {
            return null;
        }
        if (height > width) {
            return Bitmap.createScaledBitmap(bitmap, (int) ((((float) width) / height) * maxSize), maxSize, false);
        } else {
            return Bitmap.createScaledBitmap(bitmap, maxSize, (int) ((((float) height) / width) * maxSize), false);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= allIds.length) {
            return -1;
        }
        return allIds[position];

    }

    @Override
    public Fragment getItem(int position) {
        if (position < 0 || position >= allIds.length) {
            return null;
        }
        return PlayFragment.newInstance(position, allIds[position]);
    }

    @Override
    public int getCount() {
        return bufferedCount;
    }

    @Override
    public int getItemPosition(Object object) {
        final PlayFragment fragment = (PlayFragment) object;
        if (fragment == null) {
            return PagerAdapter.POSITION_NONE;
        }
        int location = -1;
        for (int i = 0, len = allIds.length ; i < len ; i++) {
            if (allIds[i].equals(fragment.getEpisodeId())) {
                location = i;
                break;
            }
        }
        if (location < 0) {
            return PagerAdapter.POSITION_NONE;
        }
        if (location == fragment.getPosition()) {
            return PagerAdapter.POSITION_UNCHANGED;
        }
        fragment.setPosition(location);
        return location;
    }

    @Override
    public void notifyDataSetChanged(){
        // can't call new CacheDataTask().execute because may not update in time, just going to have
        // to wait on UI thread...
        allIds = storage.getAllIds();
        bufferedCount = allIds.length;
        super.notifyDataSetChanged();
        //new CacheDataTask().execute();
    }
}
