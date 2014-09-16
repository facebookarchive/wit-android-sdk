/*
    Copyright 2013 Wit Inc. All rights reserved.
 */

package ai.wit.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;

import ai.wit.sdk.model.WitResponse;

/**
 * Wit class that allow request to Wit server.
 * Recognize intent from text or trigger a voice recognition popin
 * Created by Wit on 5/27/13.
 */
public class Wit implements IWitCoordinator {


    protected static final int RESULT_SPEECH = 1;
    String _accessToken;
    IWitListener _witListener;
    WitMic _witMic;

    public Wit(String accessToken, IWitListener witListener) {
        _accessToken = accessToken;
        _witListener = witListener;
    }

    public void startListening() throws IOException {
        _witListener.witDidStartListening();
        _witMic = new WitMic(this);
        _witMic.startRecording();
        PipedInputStream in = _witMic.getInputStream();
        streamRawAudio(in, "signed-integer", 16, WitMic.SAMPLE_RATE, ByteOrder.LITTLE_ENDIAN);
    }

    public void stopListening() {

        _witMic.stopRecording();
        _witListener.witDidStopListening();
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
           _witListener.witDidGraspIntent(null, null, null, 0, new Error("InputStream null"));
       }
       else {
           String contentType = String.format("audio/raw;encoding=%s;bits=%s;rate=%s;endian=%s",
                   encoding, String.valueOf(bits), String.valueOf(rate), order == ByteOrder.LITTLE_ENDIAN ? "little" : "big");
           WitSpeechRequestTask request = new WitSpeechRequestTask(_accessToken, contentType) {
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
            _witListener.witDidGraspIntent(null, null, null, 0, new Error("Input Text null"));
        WitMessageRequestTask request = new WitMessageRequestTask(_accessToken) {
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
            _witListener.witDidGraspIntent(null, null, null, 0, errorDuringRecognition);
        } else if (response == null) {
            _witListener.witDidGraspIntent(null, null, null, 0, new Error("Response null"));
        } else {
            Log.d("Wit", "didGraspIntent Correctly " + response.getOutcome().get_intent());
            _witListener.witDidGraspIntent(response.getOutcome().get_intent(),
                    response.getOutcome().get_entities(),
                    response.getBody(), response.getOutcome().get_confidence(), null);
        }
    }
}
