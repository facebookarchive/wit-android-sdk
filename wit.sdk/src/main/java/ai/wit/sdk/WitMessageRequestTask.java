/*
    Copyright 2013 Wit Inc. All rights reserved.
 */
package ai.wit.sdk;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * The request class.
 * The only purpose of this class is to call Wit async and return the result without any post processing.
 * Created by Wit on 5/30/13.
 */
public class WitMessageRequestTask extends AsyncTask<String, String, String> {

    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String ACCEPT_HEADER = "Accept";
    private final String BEARER_FORMAT = "Bearer %s";
    private final String ACCEPT_VERSION = "application/vnd.wit." + WitRequest.version;
    private String _accessToken;
    private JsonObject _context;
    private IWitListener _witListener;

    public WitMessageRequestTask(String accessToken, JsonObject context, IWitListener witListener) {
        _accessToken = accessToken;
        _context = context;
        _witListener = witListener;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        is.close();

        return sb.toString();
    }

    @Override
    protected String doInBackground(String... text) {
        String response = null;
        try {
            String message = text[0];
            WitRequest witRequest = new WitRequest(_witListener, _context);
            Log.d("Wit", "Requesting ...." + text[0]);
            String urlStr = witRequest
                    .buildUri("message")
                    .appendQueryParameter("q", message)
                    .build()
                    .toString();
            Log.d(getClass().getName(), "URL IS: " + urlStr);
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty(AUTHORIZATION_HEADER, String.format(BEARER_FORMAT, _accessToken));
            urlConnection.addRequestProperty(ACCEPT_HEADER, ACCEPT_VERSION);
            try {
                final InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                response = convertStreamToString(in);
                in.close();
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            Log.e("Wit", "An error occurred during the request, did you set your token correctly?", e);
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
    }
}
