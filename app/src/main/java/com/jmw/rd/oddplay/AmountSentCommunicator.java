package com.jmw.rd.oddplay;

import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;

public interface AmountSentCommunicator {
    void newDownload();
    void totalToOutputStream(long amt);
    EmergencyDownloadStopException stopNow();
}
