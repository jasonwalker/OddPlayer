package com.jmw.rd.oddplay.episode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.jmw.rd.oddplay.PodPage;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.download.DownloadController;
import com.jmw.rd.oddplay.play.animations.FilterDisplayAnimation;
import com.jmw.rd.oddplay.storage.Episode;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.Storage;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.widgets.Dialog;

import java.util.ArrayList;
import java.util.List;


public class EpisodeViewPage extends PodPage implements EpisodeListAdapter.EpisodeSelectCountListener  {

    private Button moveEpisodeButton;
    private Button deleteEpisodesButton;
    private Storage storage;
    private ListView episodeList;
    private EpisodeListAdapter episodeAdapter;
    private TextView episodeInfo;
    private TextView episodeName;
    private LinearLayout episodeFilter;
    private EditText episodeFilterText;
    private EpisodeListChangedReceiver episodeListChangedReceiver;
    private PlayInfoReceiver infoReceiver;
    private boolean displayCreated = false;
    private Intent lastIntent;

    private EpisodeController episodeController;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = StorageUtil.getStorage(activity);
        episodeController = new EpisodeController(activity);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View result = inflater.inflate(R.layout.main_view_episode, container, false);
        this.episodeList = (ListView) result.findViewById(R.id.episodeList);
        episodeAdapter = new EpisodeListAdapter(activity, true, false, true);
        episodeAdapter.addSelectCountListener(this);
        this.episodeList.setAdapter(episodeAdapter);
        episodeList.setOnItemClickListener(new ListClickListener());
        episodeInfo = (TextView) result.findViewById(R.id.episodeInfoText);
        Button openFilterButton = (Button) result.findViewById(R.id.openFilterButton);
        openFilterButton.setOnTouchListener(new OpenFilterListener());
        episodeName = (TextView) result.findViewById(R.id.episodeNameText);
        episodeFilter = (LinearLayout) result.findViewById(R.id.episodeFilter);
        episodeFilterText = (EditText) result.findViewById(R.id.episodeFilterText);
        episodeFilterText.addTextChangedListener(new EpisodeFilterChangeListener());
        Spinner episodeFilterSpinner = (Spinner) result.findViewById(R.id.episodeFilterSpinner);
        episodeFilterSpinner.setOnItemSelectedListener(new EpisodeFilterSelectedListener());
        this.episodeListChangedReceiver = new EpisodeListChangedReceiver();
        episodeController.registerForEpisodeListChange(this.episodeListChangedReceiver);
        infoReceiver = new PlayInfoReceiver();
        moveEpisodeButton = (Button) result.findViewById(R.id.moveEpisodesButton);
        moveEpisodeButton.setOnTouchListener(new MoveEpisodesListener());
        moveEpisodeButton.setEnabled(false);
        deleteEpisodesButton = (Button) result.findViewById(R.id.deleteEpisodesButton);
        deleteEpisodesButton.setOnTouchListener(new DeleteEpisodesListener());
        deleteEpisodesButton.setEnabled(false);
        PopulateEpisodeAdapterTask populateAdapter = new PopulateEpisodeAdapterTask();
        populateAdapter.execute();
        return result;
    }

    private class PopulateEpisodeAdapterTask extends AsyncTask<Void, Episode, Void> {
        @Override
        protected Void doInBackground(Void... unused) {
            List<Episode> episodes = storage.getAllEpisodes();
            for (Episode episode : episodes) {
                publishProgress(episode);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Episode... episodes) {
            for (Episode episode : episodes) {
                episodeAdapter.add(episode);
            }
            episodeAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void result) {
            refreshEpisodesScroll();
        }
    }

    public void refreshEpisodesScroll() {
        episodeList.setSelection(storage.fast.getCurrentEpisodeNumber());
    }

    @Override
    public void onResume() {
        super.onResume();
        displayCreated = true;
        playController.registerForPlayInfo(this.infoReceiver);
        displayDownloadBroadcast(lastIntent);
    }

    @Override
    public void onPause() {
        super.onPause();
        playController.unregisterReceiver(this.infoReceiver);
        Utils.hideKeyboard(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        displayCreated = false;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        episodeController.unregisterForEpisodeListChange(this.episodeListChangedReceiver);
    }

    private void deleteSelected() {
        final List<Integer> positions = episodeAdapter.getSelectedTruePositions();
        if (positions.size() > 0) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        final DeleteEpisodesTask deleteTask = new DeleteEpisodesTask(positions);
                        deleteTask.execute();
                    }
                }
            };
            Dialog.showYesNo(activity, String.format(activity.getString(R.string.confirmDeleteEpisodes),
                    positions.size()), dialogClickListener, dialogClickListener);
        }
    }

    private void moveToNextAfterCurrent() {
        final MoveEpisodesTask moveEpisodes = new MoveEpisodesTask();
        moveEpisodes.execute();
    }

    public void displayDownloadBroadcast(Intent intent) {
        lastIntent = intent;
        if (!this.displayCreated || intent == null) {
            return;
        }
        final boolean finished = intent.getBooleanExtra(DownloadController.INFO_DOWNLOAD_FINISHED, false);
        if (finished) {
            String message = intent.getStringExtra(DownloadController.INFO_DOWNLOAD_FINISHED_MESSAGE);
            episodeName.setText(message);
            episodeInfo.setText("");
        } else {
            if (intent.getBooleanExtra(DownloadController.INFO_PARSING_FEEDS, false)) {
                String feedInfo = intent.getStringExtra(DownloadController.INFO_DOWNLOAD_FEED_INFO);
                episodeName.setText(feedInfo);
            } else {
                String amt = intent.getStringExtra(DownloadController.INFO_DOWNLOAD_AMOUNT);
                String total = intent.getStringExtra(DownloadController.INFO_DOWNLOAD_TOTAL);
                String name = intent.getStringExtra(DownloadController.INFO_DOWNLOAD_NAME);
                String feedName = intent.getStringExtra(DownloadController.INFO_DOWNLOAD_FEED_INFO);
                String numberInQueue = intent.getStringExtra(DownloadController.INFO_NUMBER_IN_QUEUE);
                String totalInQueue = intent.getStringExtra(DownloadController.INFO_TOTAL_IN_QUEUE);
                episodeInfo.setText(String.format(activity.getString(R.string.episodeNumberOfTotal), numberInQueue, totalInQueue, amt, total));
                episodeName.setText(String.format(activity.getString(R.string.feedAndEpisodeName), feedName, name));
            }
        }
    }

    private class EpisodeFilterChangeListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            episodeAdapter.setCurrentFilterText(s);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class ListClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            DialogFragment popup = EpisodeDetailPopup.newInstance(episodeAdapter.getItem(position), false);
            popup.show(transaction, "dialog");
        }
    }

    /**
     * BUTTON Listeners
     */

    private class OpenFilterListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                FilterDisplayAnimation animation = new FilterDisplayAnimation(activity, episodeFilter, episodeFilterText, 160,
                        episodeFilter.getLayoutParams());
                animation.runAnimation();
            }
            return false;
        }
    }

    private class MoveEpisodesListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                moveToNextAfterCurrent();
            }
            return false;
        }
    }

    private class DeleteEpisodesListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                deleteSelected();
            }
            return false;
        }
    }

    private class MoveEpisodesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... unused) {
            int currentPlayPosition = storage.fast.getCurrentEpisodeNumber();
            ArrayList<Integer> selected = episodeAdapter.getSelectedTruePositions();
            episodeController.shiftEpisodes(selected, currentPlayPosition + 1);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            episodeAdapter.resetNumberSelected();
        }
    }

    private class DeleteEpisodesTask extends AsyncTask<Void, Integer, Void> {
        final List<Integer> numbers;

        public DeleteEpisodesTask(List<Integer> numbers) {
            // these numbers are in descending order
            this.numbers = numbers;
        }

        @Override
        protected Void doInBackground(Void... unused) {
            for (int i = 0; i < numbers.size(); i++) {
                try {
                    int realLocation = numbers.get(i);
                    Episode ep = episodeAdapter.getItemAtTruePosition(realLocation);
                    episodeController.deleteEpisode(ep, realLocation);
                } catch (ResourceAllocationException e) {
                    publishProgress(-1);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            episodeAdapter.getSelectedEpisodes(true);
            episodeAdapter.resetNumberSelected();
            episodeAdapter.runFilter(new FilterFinishedListener());
            episodeAdapter.resetNumberSelected();
        }
    }

    private class EpisodeListChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(EpisodeController.EPISODE_ADDED, false)) {
                Episode episode = intent.getParcelableExtra(EpisodeController.EPISODE_VALUE);
                episodeAdapter.add(episode);
            } else if (intent.getBooleanExtra(EpisodeController.EPISODE_MOVED, false)) {
                ArrayList<Integer> srcPositions = intent.getIntegerArrayListExtra(EpisodeController.EPISODE_MOVE_SOURCE);
                int targetPosition = intent.getIntExtra(EpisodeController.EPISODE_MOVE_TARGET, -1);
                episodeAdapter.moveEpisodes(srcPositions, targetPosition);
            } else if (intent.getBooleanExtra(EpisodeController.EPISODE_REMOVE_BEFORE, false)) {
                int deletedNumber = intent.getIntExtra(EpisodeController.EPISODE_NUMBER, -1);
                for (int i = deletedNumber-1 ; i >= 0 ; i--) {
                    episodeAdapter.remove(i);
                }
                episodeAdapter.getSelectedEpisodes(true);
                episodeAdapter.resetNumberSelected();
            } else if (intent.getBooleanExtra(EpisodeController.EPISODE_DELETED, false)) {
                int deletedNumber = intent.getIntExtra(EpisodeController.EPISODE_NUMBER, -1);
                if (deletedNumber == -1) {
                    Toast.makeText(activity, "Failed to delete episode", Toast.LENGTH_LONG).show();
                } else {
                    episodeAdapter.remove(deletedNumber);
                }
            } else {
                int deletedNumber = intent.getIntExtra(EpisodeController.EPISODE_NUMBER, -1);
                if (deletedNumber >= 0) {
                    episodeAdapter.remove(deletedNumber);
                }
            }
            episodeAdapter.runFilter();
        }
    }

    @Override
    public void countChanged(int count) {
        moveEpisodeButton.setEnabled(count != 0);
        deleteEpisodesButton.setEnabled(count != 0);
    }

    private class EpisodeFilterSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            episodeAdapter.setCurrentFilterSource(pos == 0);
            episodeAdapter.runFilter();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

    private class PlayInfoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            episodeAdapter.notifyDataSetChanged();
        }
    }


    private class FilterFinishedListener implements Filter.FilterListener {
        @Override
        public void onFilterComplete(int count) {
            episodeList.setSelection(storage.fast.getCurrentEpisodeNumber());
        }
    }
}
