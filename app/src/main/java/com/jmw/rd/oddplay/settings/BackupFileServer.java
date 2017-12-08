package com.jmw.rd.oddplay.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.jmw.rd.oddplay.R;
import com.jmw.rd.oddplay.Utils;
import com.jmw.rd.oddplay.download.EmergencyDownloadStopException;
import com.jmw.rd.oddplay.storage.ResourceAllocationException;
import com.jmw.rd.oddplay.storage.Storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import fi.iki.elonen.NanoHTTPD;

class BackupFileServer extends NanoHTTPD {

    private final Storage storage;
    private final Context context;
    private final int port;
    private final ResultReceiver resultReceiver;

    public BackupFileServer(Context context, Storage storage, int port, ResultReceiver resultReceiver) {
        super(port);
        this.context = context;
        this.storage = storage;
        this.port = port;
        this.resultReceiver = resultReceiver;
        this.setTempFileManagerFactory(new ServerTempManagerFactory());
    }

    private class OutputThread extends Thread {
        private final boolean onlyMetaData;
        private final OutputStream output;

        private OutputThread(OutputStream output, boolean onlyMetaData) {
            this.setDaemon(true);
            this.output = output;
            this.onlyMetaData = onlyMetaData;
        }

        @Override
        public void run() {
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(output))){
                Iterator<Storage.DumpInfo> infos = storage.getDBInputStream(this.onlyMetaData);
                try {
                    while(infos.hasNext()) {
                        Storage.DumpInfo info = infos.next();
                        ZipEntry entry = new ZipEntry(info.getName());
                        zos.putNextEntry(entry);
                        try (InputStream is = info.getInputStream()){
                            Utils.IsToOs(is, zos, null);
                            zos.closeEntry();
                        }
                    }
                } catch (EmergencyDownloadStopException e) {
                    if (resultReceiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(BackupService.ERROR_STRING, context.getString(R.string.emergencyShutdown));
                        resultReceiver.send(BackupService.ERROR, bundle);
                    }
                }
            } catch (IOException | ResourceAllocationException e) {
                if (resultReceiver != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(BackupService.ERROR_STRING, String.format(context.getString(R.string.writingBackupDataFailed), Log.getStackTraceString(e)));
                    resultReceiver.send(BackupService.ERROR, bundle);
                }
            }
        }
    }

    private class ServerTempManagerFactory implements TempFileManagerFactory {
        @Override
        public TempFileManager create() {
            try {
                return new ServerTempFileManager();
            } catch(ResourceAllocationException e) {
                return null;
            }
        }

        private class ServerTempFileManager implements TempFileManager {
            private final String tmpdir;
            private final List<TempFile> tempFiles;

            private ServerTempFileManager() throws ResourceAllocationException{
                tmpdir = BackupFileServer.this.storage.getEpisodesDir().getAbsolutePath();
                tempFiles = new ArrayList<>();
            }

            @Override
            public TempFile createTempFile() throws Exception {
                DefaultTempFile tempFile = new DefaultTempFile(tmpdir);
                tempFiles.add(tempFile);
                return tempFile;
            }

            @Override
            public void clear() {
                for (TempFile file : tempFiles) {
                    try {
                        file.delete();
                    } catch (Exception ignored) {
                    }
                }
                tempFiles.clear();
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        if (Method.GET.equals(method)) {
            if (uri.equalsIgnoreCase("/all") || uri.equalsIgnoreCase("/meta")) {
                boolean onlyMetaData = uri.equalsIgnoreCase("/meta");
                try {
                    PipedOutputStream output = new PipedOutputStream();
                    PipedInputStream input = new PipedInputStream(output);
                    OutputThread outputThread = new OutputThread(output, onlyMetaData);
                    outputThread.start();
                    Response response = new NanoHTTPD.Response(Response.Status.OK, "application/octet-stream", input);
                    response.setChunkedTransfer(true);
                    if (onlyMetaData) {
                        response.addHeader("Content-Disposition", "attachment; filename=oddPlayer_backup_metadata.zip");
                    } else {
                        response.addHeader("Content-Disposition", "attachment; filename=oddPlayer_backup_all.zip");
                    }
                    return response;
                } catch (IOException e) {
                    if (resultReceiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(BackupService.ERROR_STRING, context.getString(R.string.couldNotStartServer));
                        resultReceiver.send(BackupService.ERROR, bundle);
                    }
                    return null;
                }
            } else {
                try {
                    String host = String.format(Locale.US, "http://%s:%d", Utils.getIPAddress(context), this.port);
                    String output = String.format(this.context.getString(R.string.FileBackupIndex), storage.getEpisodesDir().getAbsoluteFile(), host);
                    return new NanoHTTPD.Response(Response.Status.OK, "text/html", output);
                } catch (ResourceAllocationException e) {
                    Bundle bundle = new Bundle();
                    bundle.putString(BackupService.ERROR_STRING, e.getMessage());
                    resultReceiver.send(BackupService.ERROR, bundle);
                }
            }
        } else if (Method.POST.equals(method)) {
            if (uri.equals("/upload")) {
                try {
                    Map<String, String> files = new HashMap<>();

                    try {
                        session.parseBody(files);
                    } catch (IOException ioe) {
                        return new NanoHTTPD.Response(Response.Status.BAD_REQUEST, "text/plain", Log.getStackTraceString(ioe));
                    } catch (ResponseException re) {
                        return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                    }
                    File uploadedFile = new File(files.get("filename"));
                    try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(uploadedFile))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            storage.putBackupData(zipEntry.getName(), zipInput);
                        }
                    }
                    return new NanoHTTPD.Response(Response.Status.OK, "text/plain", String.format(context.getString(R.string.successfulUploadOfLength), uploadedFile.length()));
                } catch(EmergencyDownloadStopException e) {
                    if (resultReceiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(BackupService.ERROR_STRING, context.getString(R.string.emergencyShutdown));
                        resultReceiver.send(BackupService.ERROR, bundle);
                    }
                }
                catch(IOException | ResourceAllocationException e) {
                    if (resultReceiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(BackupService.ERROR_STRING, String.format(context.getString(R.string.backupFailed), Log.getStackTraceString(e)));
                        resultReceiver.send(BackupService.ERROR, bundle);
                    }
                }
            }
        }
        return null;
    }
}
