package com.jmw.rd.oddplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.jmw.rd.oddplay.download.DownloadController;
import com.jmw.rd.oddplay.episode.EpisodeController;
import com.jmw.rd.oddplay.episode.EpisodeViewPage;
import com.jmw.rd.oddplay.feed.FeedsViewPage;
import com.jmw.rd.oddplay.play.PlayController;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.widgets.Dialog;
import com.jmw.rd.oddplay.download.NoWifiException;
import com.jmw.rd.oddplay.widgets.RotatingRefreshIcon;
import com.jmw.rd.oddplay.play.PlayViewPage;
import com.jmw.rd.oddplay.settings.SettingsViewPage;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity implements
        OnItemClickListener {
    private DownloadProgressReceiver downloadProgressReceiver;
    private RotatingRefreshIcon spinningRefreshButton;
    private static final Object refreshButtonLock = new Object();
    private DrawerLayout drawerLayout = null;
    private ActionBarDrawerToggle toggle = null;
    private PlayViewPage playView;
    private EpisodeViewPage episodesView;
    private FeedsViewPage feedsView = null;
    private SettingsViewPage settingsView = null;
    private Runnable runnable;
    private MenuItem playIcon;
    private MenuItem refreshIcon;
    private PlayStateReceiver playStateReceiver;
    private Fragment displayingFragment;
    private DownloadController downloadController;
    private PlayController playController;
    private EpisodeController episodeController;
    private AlertDialog attemptingDownloadStopDialog;

    private void strictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                //.detectDiskReads()
                //.detectDiskWrites()
                //.detectNetwork()   // or .detectAll() for all detectable problems
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                //.detectLeakedSqlLiteObjects()
                //.detectLeakedClosableObjects()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //strictMode();
        episodesView = new EpisodeViewPage();
        playView = new PlayViewPage();
        InitViews initViews = new InitViews();
        initViews.execute();
        ListView drawer = (ListView) findViewById(R.id.drawer);
        drawer.setAdapter(new ArrayAdapter<>(
                this,
                R.layout.drawer_row,
                getResources().getStringArray(R.array.drawer_rows)));
        drawer.setOnItemClickListener(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new DrawerListener(this, drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close);
        drawerLayout.setDrawerListener(toggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        playController = new PlayController(MainActivity.this);
        playStateReceiver = new PlayStateReceiver();
    }

    private class InitViews extends AsyncTask<Void, Void, Void> {
        private Storage storage;
        private int feedsSize;
        @Override
        protected Void doInBackground(Void... unused) {
            try {
                storage = StorageUtil.getStorage(MainActivity.this);
                //get storage to cache number episodes
                storage.getNumberEpisodes();
                //initialize storage location, getSelectedStorage will choose first external if available
                //otherwise default to internal
                storage.setSelectedStorage(storage.getSelectedStorage());
                feedsSize = storage.getFeeds().size();
                //creates dir if needed
                storage.getEpisodesDir();
            } catch(ResourceAllocationException e) {
                Dialog.showOK(MainActivity.this, getString(R.string.problemInitializing));
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            if (getFragmentManager().findFragmentById(R.id.content) == null) {
                if (feedsSize == 0) {
                    feedsView = new FeedsViewPage();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content, feedsView).commit();
                    setTitle(getString(R.string.titleFeeds));
                    displayingFragment = feedsView;
                    Dialog.showOK(MainActivity.this, getString(R.string.getStarted));
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content, playView).commit();
                    setTitle(getString(R.string.titlePlay));
                    displayingFragment = playView;
                }
            }
            downloadController = new DownloadController(MainActivity.this);
            episodeController = new EpisodeController(MainActivity.this);
            downloadProgressReceiver = new DownloadProgressReceiver();
            downloadController.registerForDownloadEvent(downloadProgressReceiver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        refreshIcon = menu.findItem(R.id.action_refresh_episodes);
        playIcon = menu.findItem(R.id.action_toggle_play_episode);
        playIcon.setVisible(false);
        playController.promptForPlayInfo();
        return true;
    }

    private void displayDownloadingIcon() {
        synchronized (refreshButtonLock) {
            if (refreshIcon != null) {
                spinningRefreshButton = new RotatingRefreshIcon(this, refreshIcon);
                spinningRefreshButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadController.stopDownloading(getString(R.string.emergencyStopByUser));
                        attemptingDownloadStopDialog = Dialog.showOK(MainActivity.this, getString(R.string.attemptingToStopDownload));
                    }
                });
            }
        }
    }

    // returns true if successfully started downloading
    private boolean startEpisodeDownload() {
        try {
            downloadController.downloadAllEpisodes();
            return true;
        } catch (NoWifiException e) {
            if (StorageUtil.getStorage(this).fast.getUseOnlyWIFI()) {
                Dialog.showOK(this, getString(R.string.enableWIFIToDownloadEpisodes));
            } else {
                Dialog.showOK(this, getString(R.string.enableWIFIOrMobileToDownloadEpisodes));
            }
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playController.registerForPlayState(playStateReceiver);
        // check to see if we received an intent to add a feed from subscribeonandroid.com
        handleSubscribeIntent(getIntent());
        String startupMessage = StorageUtil.getStorage(this).fast.getStartupMessage();
        if (!startupMessage.equals("")) {
            StorageUtil.getStorage(this).fast.setStartupMessage("");
            Dialog.showOK(this, startupMessage);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSubscribeIntent(intent);
    }

    private void handleSubscribeIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String dataString = intent.getDataString();
        if (dataString == null) {
            return;
        }
        setIntent(null);
        String url = null;
        for (String extra : new String[]{"www.subscribeonandroid.com/", "subscribeonandroid.com/"}) {
            int index = dataString.indexOf(extra);
            if (index != -1) {
                url = dataString.replace(extra, "");
                break;
            }
        }
        if (url != null) {
            if (feedsView == null) {
                feedsView = new FeedsViewPage();
            }
            feedsView.setUrlToAdd(url);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, feedsView).commit();
            setTitle(getString(R.string.titleFeeds));
            this.displayingFragment = feedsView;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        playController.guiIsPausing();
        playController.unregisterReceiver(playStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadController.unregisterForDownloadEvent(this.downloadProgressReceiver);
        this.downloadProgressReceiver = null;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return (true);
        }
        final int id = item.getItemId();

        if (id == R.id.action_refresh_episodes) {
            if (startEpisodeDownload()) {
                displayDownloadingIcon();
            }
        } else if (id == R.id.action_delete_episodes) {
            deletePreviousEpisodes(this);
        } else if (id == R.id.action_toggle_play_episode) {
            playController.togglePlay();
            playController.promptForPlayInfo();
        }
        return super.onOptionsItemSelected(item);
    }

    private void deletePreviousEpisodes(final Context context) {
        final Storage storage = StorageUtil.getStorage(context);
        final int currentNumber = storage.fast.getCurrentEpisodeNumber();
        if (currentNumber > 0) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        DeletePreviousEpisodesTask deleteTask = new DeletePreviousEpisodesTask(currentNumber);
                        deleteTask.execute();
                    }
                }
            };
            Dialog.showYesNo(context, String.format(getString(R.string.confirmDeleteEpisodesBeforeCurrent),
                    currentNumber), dialogClickListener, dialogClickListener);
        } else {
            Dialog.showOK(context, getString(R.string.noEpisodesToDelete));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View row,
                            int position, long id) {
        switch (position) {
            case 0:
                showPlay();
                break;
            case 1:
                showEpisodes();
                break;
            case 2:
                showFeeds();
                break;
            case 3:
                showSettings();
                break;
            default:
                showPlay();
                break;
        }
        // onDrawerClose will process runnable instantiated by method calls above
        drawerLayout.closeDrawers();
    }

    private void showItemInMainContentArea(Fragment fragment, String title, boolean showPlay) {
        if (!fragment.isVisible()) {
            setTitle(title);
            this.displayingFragment = fragment;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.jump_in, R.anim.fade_out);
            transaction.replace(R.id.content, fragment);
            transaction.commit();
            if (playIcon != null) {
                playIcon.setVisible(showPlay);
            }
            playController.promptForPlayInfo();
        }

    }

    private void showPlay() {
        runnable = new Runnable() {
            @Override
            public void run() {
                showItemInMainContentArea(playView, getString(R.string.titlePlay), false);
            }
        };
    }

    private void showEpisodes() {
        runnable = new Runnable() {
            @Override
            public void run() {
                showItemInMainContentArea(episodesView, getString(R.string.titleEpisodes), true);
            }
        };
    }

    private void showFeeds() {
        runnable = new Runnable() {
            @Override
            public void run() {
            if (feedsView == null) {
                feedsView = new FeedsViewPage();
            }
            showItemInMainContentArea(feedsView, getString(R.string.titleFeeds), true);
            }
        };
    }

    private void showSettings() {
        runnable = new Runnable() {
            @Override
            public void run() {
            if (settingsView == null) {
                settingsView = new SettingsViewPage();
            }
            showItemInMainContentArea(settingsView, getString(R.string.titleSettings), true);
            }
        };
    }

    private void displayNewView() {
        getWindow().getDecorView().getHandler().post(runnable);
        runnable = null;
    }

    private class DeletePreviousEpisodesTask extends AsyncTask<Void, Integer, Void> {
        private final int number;

        public DeletePreviousEpisodesTask(int numbers) {
            this.number = numbers;
        }

        @Override
        protected Void doInBackground(Void... unused) {
            try {
                episodeController.deleteEpisodesBefore(number);
            } catch (ResourceAllocationException e){
                // can't do much here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (MainActivity.this.displayingFragment == episodesView) {
                episodesView.refreshEpisodesScroll();
            }
        }
    }

    private class DownloadProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean finished = intent.getBooleanExtra(DownloadController.INFO_DOWNLOAD_FINISHED, false);
            if (finished) {
                if (attemptingDownloadStopDialog != null) {
                    attemptingDownloadStopDialog.dismiss();
                    attemptingDownloadStopDialog = null;
                }
                synchronized (refreshButtonLock) {
                    if (MainActivity.this.spinningRefreshButton != null) {
                        spinningRefreshButton.stop();
                        spinningRefreshButton = null;
                    }
                }
            } else {
                synchronized (refreshButtonLock) {
                    if (MainActivity.this.spinningRefreshButton == null) {
                        displayDownloadingIcon();
                    }
                }
            }
            episodesView.displayDownloadBroadcast(intent);
        }
    }

    private class DrawerListener extends ActionBarDrawerToggle {
        public DrawerListener(Activity activity, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerClosed(View drawerview) {
            super.onDrawerClosed(drawerview);
            displayNewView();
        }
    }

    private class PlayStateReceiver extends BroadcastReceiver {

        public PlayStateReceiver(){}

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlaying = intent.getBooleanExtra(PlayController.STATE_PLAYING, false);
            if (playIcon != null) {
                if (isPlaying) {
                    playIcon.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_menu_pause));
                } else {
                    playIcon.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_menu_play_clip));
                }
            }
        }
    }
}
