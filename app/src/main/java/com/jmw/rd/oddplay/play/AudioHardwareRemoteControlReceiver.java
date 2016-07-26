package com.jmw.rd.oddplay.play;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class AudioHardwareRemoteControlReceiver extends BroadcastReceiver {
    private static long lastEventTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        PlayController playController = new PlayController(context);
        long val = event.getEventTime() - lastEventTime;
        // fix bug where bluetooth device sending multiple key events for one press
        if (val < 50){
            return;
        }
        lastEventTime = event.getEventTime();

        switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                playController.togglePlay();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                playController.play();
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                playController.pause();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                playController.previous();
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                playController.next();
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                playController.rewind();
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                playController.fastforward();
                break;
            default:
                return;
        }
    }

}