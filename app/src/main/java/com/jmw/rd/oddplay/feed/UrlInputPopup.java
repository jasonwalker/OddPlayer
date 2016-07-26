package com.jmw.rd.oddplay.feed;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.download.DownloadController;
import com.jmw.rd.oddplay.download.NoWifiException;
import com.jmw.rd.oddplay.storage.StorageUtil;
import com.jmw.rd.oddplay.widgets.Dialog;
import com.jmw.rd.oddplay.widgets.PopupDialogFragment;
import com.jmw.rd.oddplay.widgets.SmallButton;

public class UrlInputPopup extends PopupDialogFragment {
    private Context context;
    private FeedController feedController;
    private ClipboardManager clipboard;

    public static UrlInputPopup newInstance() {
        UrlInputPopup popup = new UrlInputPopup();
        Bundle args = new Bundle();
        popup.setArguments(args);
        return popup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getActivity();
        feedController = new FeedController(context);
        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        @SuppressLint("InflateParams")
        final View dialogLayout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.get_url_dialog, null);
        Button searchFeedButton = (Button) dialogLayout.findViewById(R.id.feedSearchButton);
        searchFeedButton.setOnTouchListener(new SearchFeedListener());
        Button manuallyEnterUrlButton = (Button) dialogLayout.findViewById(R.id.urlDialogAdd);
        manuallyEnterUrlButton.setOnTouchListener(new AddUrlListener());
        Button pasteUrlButton = (Button) dialogLayout.findViewById(R.id.urlDialogPaste);
        pasteUrlButton.setOnTouchListener(new PasteUrlListener());
        Button cancelButton = (SmallButton) dialogLayout.findViewById(R.id.urlDialogCancel);
        cancelButton.setOnTouchListener(new CancelTouchListener());
        Button scanQRButton = (Button) dialogLayout.findViewById(R.id.urlDialogScanQRCode);
        scanQRButton.setOnTouchListener(new ScanQRCodeListener());
        Button quickStartButton = (Button) dialogLayout.findViewById(R.id.quickStart);
        quickStartButton.setOnTouchListener(new QuickStartListener());
        return dialogLayout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            if (resultCode == Activity.RESULT_OK) {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
                if (scanResult != null) {
                    String result = scanResult.getContents();
                    feedController.broadcastAddFeed(result);
                    dismiss();
                } else {
                    Dialog.showOK(context, context.getString(R.string.qrScanReturnedNull));
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Dialog.showOK(context, context.getString(R.string.qrCodeScanCancelled));
            }
        } catch (Exception e) {
            Dialog.showOK(context, String.format(context.getString(R.string.qrCodeScanException), e.getMessage()));
        }
    }

    public final class FragmentIntentIntegrator extends IntentIntegrator {
        private final Fragment fragment;

        public FragmentIntentIntegrator(Fragment fragment) {
            super(fragment.getActivity());
            this.fragment = fragment;
        }

        @Override
        protected void startActivityForResult(Intent intent, int code) {
            fragment.startActivityForResult(intent, code);
        }
    }

    private class ScanQRCodeListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                IntentIntegrator integrator = new FragmentIntentIntegrator(UrlInputPopup.this);
                integrator.initiateScan();
            }
            return false;
        }
    }

    public void showEnterUrlPopup(String text) {
        FeedUrlStringPopup popup = FeedUrlStringPopup.newInstance(text);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        popup.show(transaction, "enterURL");
    }

    private class SearchFeedListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if (Utils.isDataOn(getActivity())) {
                    FeedsSearchInputPopup feedSearchInput = FeedsSearchInputPopup.newInstance();
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    feedSearchInput.show(transaction, "feedSearchdialog");
                } else {
                    if (StorageUtil.getStorage(getActivity()).fast.getUseOnlyWIFI()) {
                        Dialog.showOK(getActivity(), "Please enable WIFI to search for feeds");
                    } else {
                        Dialog.showOK(getActivity(), "Please enable WIFI or Mobile data to search for feeds");
                    }
                }
            }
            return false;
        }
    }

    private class AddUrlListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                showEnterUrlPopup("http://");
            }
            return false;
        }
    }

    private class PasteUrlListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                String text = "";
                if (clipboard.hasPrimaryClip()) {
                    text = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
                }
                showEnterUrlPopup(text);
            }
            return false;
        }
    }

    private static class Podcast {
        public Podcast(String name, String url) {
            this.name = name;
            this.url = url;
        }
        public final String name;
        public final String url;
        @Override
        public String toString() {
            return name;
        }
    }

    private class QuickStartListener implements View.OnTouchListener {


        private final Podcast[] podcastList = new Podcast[]{
                new Podcast("Startup", "http://feeds.hearstartup.com/hearstartup"),
                new Podcast("Back Story", "http://feeds.feedburner.com/BackStoryRadio"),
                new Podcast("Mystery Show", "http://feeds.gimletmedia.com/mysteryshow"),
                new Podcast("Serial", "http://feeds.serialpodcast.org/serialpodcast"),
                new Podcast("Planet Money", "http://www.npr.org/rss/podcast.php?id=510289"),
                new Podcast("This American Life", "http://feeds.thisamericanlife.org/talpodcast"),
                new Podcast("Invisibilia", "http://www.npr.org/rss/podcast.php?id=510307"),
                new Podcast("TED Radio Hour", "http://www.npr.org/rss/podcast.php?id=510298"),
                new Podcast("Damn Interesting", "http://feeds.feedburner.com/damn-interesting-podcast"),
                new Podcast("More or Less", "http://downloads.bbc.co.uk/podcasts/radio4/moreorless/rss.xml"),
                new Podcast("99 Percent Invisible", "http://feeds.99percentinvisible.org/99percentinvisible"),
                new Podcast("Reply All", "http://feeds.hearstartup.com/hearreplyall"),
                new Podcast("The Moth", "http://feeds.themoth.org/themothpodcast"),
                new Podcast("Freakonomics", "http://feeds.feedburner.com/freakonomicsradio"),
                //new Podcast("Radio Diaries", "http://feed.radiodiaries.org/radio-diaries"),
                //new Podcast("EconTalk", "http://econlib.org/library/EconTalk.xml"),
                //new Podcast("Science Friday", "http://npr.org/rss/podcast.php?id=510221"),
                //new Podcast("Commonwealth Club of California", "http://audio.commonwealthclub.org/audio/podcast/weekly.xml"),
                new Podcast("RadioLab", "http://radiolab.org/feeds/podcast/")
        };

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                DialogInterface.OnClickListener confirmAddFeedsListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        dismiss();
                        int counter = 0;
                        feedController.registerFeedAddedListener(new FeedAddedListener());
                        for (Podcast podcast : podcastList) {
                            if (StorageUtil.getStorage(context).getFeed(podcast.url) == null) {
                                feedController.broadcastAddFeed(podcast.url);
                                counter++;
                            }
                        }
                        if (counter == 0) {
                            Dialog.showOK(context, context.getString(R.string.noNewPodcastsToAdd));
                        }
                    }
                    }
                };
                Dialog.showYesNo(context, String.format(context.getString(R.string.sureAboutAddingPodcasts),
                        TextUtils.join(", ", podcastList)), confirmAddFeedsListener, confirmAddFeedsListener);
            }
            return false;
        }

        private class FeedAddedListener extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String addedUrl = intent.getStringExtra(FeedController.URL_ADDRESS);
                if (podcastList[podcastList.length-1].url.equalsIgnoreCase(addedUrl)) {
                    try {
                        new DownloadController(context).downloadAllEpisodes();
                    } catch(NoWifiException e) {
                        Dialog.showOK(context, context.getString(R.string.noDownloadBecauseNoData));
                    }
                    feedController.unregisterFeedAddedListener(this);
                }
            }
        }
    }



    private class CancelTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dismiss();
            return false;
        }
    }
}
