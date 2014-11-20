/*
    Copyright 2013 Wit Inc. All rights reserved.
 */
package ai.wit.sdk;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * The request class.
 * The only purpose of this class is to call Wit async and return the result without any post processing.
 * Created by Wit on 5/30/13.
 */
public class WitMessageVadFail extends AsyncTask<String, String, String> {

    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String ACCEPT_HEADER = "Accept";
    private final String BEARER_FORMAT = "Bearer %s";
    private final String ACCEPT_VERSION = "application/vnd.wit." + WitRequest.version;
    private String _accessToken;
    private IWitListener _witListener;
    private String _sdkVer;
    private String _messageID;
    private int _vadTuning;

    public WitMessageVadFail(String accessToken, String messageID, Wit.VadTuning vadTuning, String sdkVer, IWitListener witListener) {
        _accessToken = accessToken;
        _witListener = witListener;
        _vadTuning = vadTuning.ordinal();
        _messageID = messageID;
        _sdkVer = sdkVer;
    }

    //public static String convertStreamToString(InputStream is) throws Exception {
        //BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        //StringBuilder sb = new StringBuilder();
        //String line;

        //while ((line = reader.readLine()) != null) {
            //sb.append(line);
        //}

        //is.close();

        //return sb.toString();
    //}

    @Override
    protected String doInBackground(String... text) {
        String response = null;
        try {
            WitRequest witRequest = new WitRequest(_witListener, null); //null for no context
            Log.d("Wit", "Reporting CVAD failure ....");
            String urlStr = witRequest
                    .buildUri("speech/vad")
                    .appendQueryParameter("message-id", _messageID)
                    .appendQueryParameter("tuning", ""+_vadTuning)
                    .appendQueryParameter("sdk-ver", _sdkVer)
                    .build()
                    .toString();
            Log.d(getClass().getName(), "URL IS: " + urlStr);
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.addRequestProperty(AUTHORIZATION_HEADER, String.format(BEARER_FORMAT, _accessToken));
            urlConnection.addRequestProperty(ACCEPT_HEADER, ACCEPT_VERSION);
            try {
                final OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                //out.write();
                out.close();
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
