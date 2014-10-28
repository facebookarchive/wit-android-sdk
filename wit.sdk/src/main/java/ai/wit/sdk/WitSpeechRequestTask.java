/*
    Copyright 2013 Wit Inc. All rights reserved.
 */
package ai.wit.sdk;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.net.Uri.Builder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The request class.
 * The only purpose of this class is to call Wit async and return the result without any post processing.
 * Created by Wit on 5/30/13.
 */
public class WitSpeechRequestTask extends AsyncTask<InputStream, String, String> {


    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String ACCEPT_HEADER = "Accept";
    private final String CONTENT_TYPE_HEADER = "Content-Type";
    private final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";
    private final String ACCEPT_VERSION = "application/vnd.wit." + WitRequest.version;
    private final String BEARER_FORMAT = "Bearer %s";
    private String _accessToken;
    private String _contentType;
    private JsonObject _context;

    public WitSpeechRequestTask(String accessToken, String contentType, JsonObject context) {
        _accessToken = accessToken;
        _contentType = contentType;
        _context = context;
    }

    @Override
    protected String doInBackground(InputStream... speech) {
        String response = null;
        try {
            Log.d("Wit", "Requesting SPEECH ...." + _contentType);
            URL url = new URL(buildUri());
            Log.d("Wit", "Posting speech to " + url.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty(AUTHORIZATION_HEADER, String.format(BEARER_FORMAT, _accessToken));
            urlConnection.setRequestProperty(ACCEPT_HEADER, ACCEPT_VERSION);
            urlConnection.setRequestProperty(CONTENT_TYPE_HEADER, _contentType);
            urlConnection.setRequestProperty(TRANSFER_ENCODING_HEADER, "chunked");
            urlConnection.setChunkedStreamingMode(0);

            try {
                OutputStream out = urlConnection.getOutputStream();
                int n;
                byte[] buffer = new byte[1024];
                while((n = speech[0].read(buffer)) > -1) {
                    out.write(buffer, 0, n);   // Don't allow any extra bytes to creep in, final write
                }
                out.close();
                Log.d("Wit", "Done sending data");
                int statusCode = urlConnection.getResponseCode();
                InputStream in;
                if (statusCode != 200) {
                    in = urlConnection.getErrorStream();
                }
                else {
                    in = new BufferedInputStream(urlConnection.getInputStream());
                }
                response = IOUtils.toString(in);
                in.close();
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            Log.d("Wit", "An error occurred during the request: " + e.getMessage());
        }
        return response;
    }

    protected String buildUri() {
        WitRequest witRequest = new WitRequest();
        Builder uriBuilder;


        uriBuilder = witRequest.getBase();
        uriBuilder.appendPath("speech");
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
