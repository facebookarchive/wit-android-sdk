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

import ai.wit.sdk.IWitListener;

/**
 * The request class.
 * The only purpose of this class is to call Wit async and return the result without any post processing.
 * Created by Wit on 5/30/13.
 */
public class WitSpeechRequestTask extends AsyncTask<InputStream, String, String> {

    private final String WIT_SPEECH_URL = "https://api.wit.ai/speech";
    private final String VERSION = "20140501";
    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String ACCEPT_HEADER = "Accept";
    private final String CONTENT_TYPE_HEADER = "Content-Type";
    private final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";
    private final String ACCEPT_VERSION = "application/vnd.wit." + VERSION;
    private final String BEARER_FORMAT = "Bearer %s";
    private String _accessToken;
    private String _contentType;
    private IWitListener _witListener;

    public WitSpeechRequestTask(String accessToken, String contentType, IWitListener witListener) {
        _accessToken = accessToken;
        _contentType = contentType;
        _witListener = witListener;
    }

    @Override
    protected String doInBackground(InputStream... speech) {
        String response = null;
        String messageId = _witListener.witGenerateMessageId();
        String fullUrl = WIT_SPEECH_URL;
        try {
            Log.d("Wit", "Requesting SPEECH ...." + _contentType);
            if (messageId != null) {
                 fullUrl = WIT_SPEECH_URL + "?msg_id=" + messageId;
            }
            URL url = new URL(fullUrl);
            Log.d("Wit", "Posting speech to " + WIT_SPEECH_URL);
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
            Log.e("Wit", "An error occurred during the request, did you set your token correctly? Is the content-type correct ?", e);
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
    }
}
