package com.jmw.rd.oddplay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnection implements AutoCloseable {

    private HttpURLConnection connection;

    public HttpConnection(String url) throws IOException {
        URL actualUrl = new URL(url);
        connection = (HttpURLConnection) actualUrl.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(5000);
    }

    @Override
    public void close() {
        connection.disconnect();
    }

    public int getContentLength() {
        return connection.getContentLength();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return connection.getOutputStream();
    }
}