/*
    Copyright 2013 Wit Inc. All rights reserved.
 */

package ai.wit.sdk;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.nio.ByteOrder;

import ai.wit.sdk.model.WitResponse;

/**
 * Wit class that allow request to Wit server.
 * Recognize intent from text or trigger a voice recognition popin
 * Created by Wit on 5/27/13.
 */
public class Wit implements IWitCoordinator {

    public enum vadConfig {
        disabled,
        detectSpeechStop,
        full
    }

    private static final String TAG = "Wit";
    protected static final int RESULT_SPEECH = 1;
    String _accessToken;
    IWitListener _witListener;
    WitMic _witMic;
    public vadConfig vad = vadConfig.detectSpeechStop;
    PipedInputStream _in;
    private JsonObject _context;

    public Wit(String accessToken, IWitListener witListener) {
        _accessToken = accessToken;
        _witListener = witListener;
    }

    public void startListening() throws IOException {
        _witMic = new WitMic(this, vad);
        _witMic.startRecording();
        _in = _witMic.getInputStream();
        if (vad != vadConfig.full) {
            voiceActivityStarted();
        } else {
            _witListener.witActivityDetectorStarted();
        }
    }

    public void stopListening() {

        _witMic.stopRecording();
        _witListener.witDidStopListening();
    }

    @Override
    public void voiceActivityStarted() {
        streamRawAudio(_in, "signed-integer", 16, WitMic.SAMPLE_RATE, ByteOrder.LITTLE_ENDIAN);
        _witListener.witDidStartListening();
    }

    public void toggleListening() throws IOException {
        if (_witMic == null || !_witMic.isRecording()) {
            startListening();
        } else {
            stopListening();
        }
    }


    /**
     * Returns the meaning extracted from a Raw stream
     * @param audio The audio stream to send over to WIT.AI
     * @param encoding The encoding for this raw audio // Android usually uses 'signed-integer'
     * @param bits The bits of the audio // Android usually uses 16
     * @param rate The rate of the audio // Android usually uses 8000
     * @param order The byte order of the audio // Android usually uses LITTLE_ENDIAN
     */
    public void streamRawAudio(InputStream audio, String encoding, int bits, int rate, ByteOrder order){
       if (audio == null ) {
           _witListener.witDidGraspIntent(null, null, new Error("InputStream null"));
       }
       else {
           Log.d(getClass().getName(), "streamRawAudio started.");
           String contentType = String.format("audio/raw;encoding=%s;bits=%s;rate=%s;endian=%s",
                   encoding, String.valueOf(bits), String.valueOf(rate), order == ByteOrder.LITTLE_ENDIAN ? "little" : "big");
           WitSpeechRequestTask request = new WitSpeechRequestTask(_accessToken, contentType, _context, _witListener) {
               @Override
               protected void onPostExecute(String result) {
                   processWitResponse(result);
               }
           };

           request.execute(audio);
       }
    }

    /**
     * Returns the meaning extracted from the text input
     * @param text text to extract the meaning from.
     */
    public void captureTextIntent(String text) {
        if (text == null)
            _witListener.witDidGraspIntent(null, null, new Error("Input Text null"));
        WitMessageRequestTask request = new WitMessageRequestTask(_accessToken, _context, _witListener) {
            @Override
            protected void onPostExecute(String result) {
                processWitResponse(result);
            }
        };
        request.execute(text);
    }

    private void processWitResponse(String result) {
        WitResponse response = null;
        Error errorDuringRecognition = null;
        Log.d("Wit", "Wit : Response " + result);
        try {
            Gson gson = new Gson();
            response = gson.fromJson(result, WitResponse.class);
            Log.d("Wit", "Gson : Response " + gson.toJson(response));
        } catch (Exception e) {
            Log.e("Wit", "Wit : Error " + e.getMessage());
            errorDuringRecognition = new Error(e.getMessage());
        }
        if (errorDuringRecognition != null) {
            _witListener.witDidGraspIntent(null, null, errorDuringRecognition);
        } else if (response == null) {
            _witListener.witDidGraspIntent(null, null, new Error("Response null"));
        } else if (response.getOutcomes().size() == 0) {
            _witListener.witDidGraspIntent(null, null, new Error("No outcome"));
        }
        else {
            Log.d(TAG, "Wit did grasp " + response.getOutcomes().size() +" outcome(s)");
            _witListener.witDidGraspIntent(response.getOutcomes(), response.getMsgId(), null);
        }
    }

    public void setContext(JsonObject jo) {
        _context = jo;
    }
}
