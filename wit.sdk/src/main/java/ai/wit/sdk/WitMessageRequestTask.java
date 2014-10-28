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

    public WitMessageRequestTask(String accessToken, JsonObject context) {
        _accessToken = accessToken;
        _context = context;
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
            Log.d("Wit", "Requesting ...." + text[0]);
            String getUrl = buildUri(message);
            Log.d(getClass().getName(), "URL IS: " + getUrl);
            URL url = new URL(getUrl);
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

    protected String buildUri(String message) {
        WitRequest witRequest = new WitRequest();
        Uri.Builder uriBuilder;

        uriBuilder = witRequest.getBase();
        uriBuilder.appendPath("message");
        uriBuilder.appendQueryParameter("q", message);
        if (_context != null) {
            Gson gson = new Gson();
            String jsonContext = gson.toJson(_context);
            uriBuilder.appendQueryParameter("context", jsonContext);
        }

        return uriBuilder.build().toString();
    }

    @Override
    protected void onPostExecute(String result) {
    }
}
