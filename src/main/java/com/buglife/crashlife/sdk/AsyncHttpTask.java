/*
 * Copyright (C) 2019 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.crashlife.sdk;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

final class AsyncHttpTask extends AsyncTask<String, Void, Void> {
    static final String BASE_URL = "https://buglife.com";
    private final String mBody;
    private final Runnable mSuccess;
    private final Runnable mFailure;
    private final String mEndpoint;


    AsyncHttpTask(String body, Runnable success, Runnable failure, String endpoint) {
        mBody = body;
        mSuccess = success;
        mFailure = failure;
        mEndpoint = endpoint;
    }

    @Override
    protected Void doInBackground(String... params) {
        post(mBody);
        return null;
    }

    private void post(String body) {
        URL url;
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        try {
            url = new URL(BASE_URL + mEndpoint);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            mainThreadHandler.post(mFailure);
            return;
        }

        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection)url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            mainThreadHandler.post(mFailure);
            return;
        }

        try {
            int status = post(connection, body);
            if (status == 201 || status == 200 || status == 202 || status == 204) {
                mainThreadHandler.post(mSuccess);
            }
        } catch (AsyncHttpTask.NetworkException e) {
            e.printStackTrace();
            mainThreadHandler.post(mFailure);
        } finally {
            connection.disconnect();
        }
    }

    private int post(HttpURLConnection openConnection, String body) throws AsyncHttpTask.NetworkException {
        openConnection.addRequestProperty("Content-Type", "application/json");
        openConnection.setDoOutput(true);

        try {
            openConnection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
            throw new AsyncHttpTask.NetworkException("Unable to post", e);
        }

        OutputStream outputStream;

        try {
            outputStream = openConnection.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            throw new AsyncHttpTask.NetworkException("Unable to post", e);
        }

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        try {
            bufferedWriter.write(body);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new AsyncHttpTask.NetworkException("Unable to post", e);
        }

        try {
            return openConnection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            throw new AsyncHttpTask.NetworkException("Unable to post", e);
        }
    }

    class NetworkException extends Exception {
        NetworkException(@NonNull String message) {
            super(message);
        }

        NetworkException(@NonNull String message, @Nullable Throwable cause) {
            super(message, cause);
        }
    }
}
