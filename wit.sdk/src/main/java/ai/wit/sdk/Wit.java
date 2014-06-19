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

import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;

import ai.wit.sdk.model.WitResponse;

/**
 * Wit class that allow request to Wit server.
 * Recognize intent from text or trigger a voice recognition popin
 * Created by Wit on 5/27/13.
 */
public class Wit extends Fragment implements RecognitionListener {

    protected static final int RESULT_SPEECH = 1;
    String _accessToken;
    IWitListener _witListener;
    Intent _recIntent;
    SpeechRecognizer _speechRecognizer;

    public Wit() {
    }

    public void setAccessToken(String accessToken) {
        _accessToken = accessToken;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        _recIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        _recIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        _recIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                ((Activity) _witListener).getApplicationContext().getPackageName());
        _recIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Wit analysing...");
        _recIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        _speechRecognizer = SpeechRecognizer.createSpeechRecognizer(((Activity) _witListener).getApplicationContext());
        _speechRecognizer.setRecognitionListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Context context = ((Activity) _witListener).getApplicationContext();
        int resId = context.getResources().getIdentifier("wit_button", "layout", context.getPackageName());
        View button = inflater.inflate(resId, container, false);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    triggerRec(false);
                }
            });
        }

        return button;
    }

    /**
     * Trigger the recording programmatically
     * @param handFree when set to true, will not display any UI.
     */
    public void triggerRec(boolean handFree) {
        if (handFree) {
            _speechRecognizer.startListening(_recIntent);
        } else {
            startActivityForResult(_recIntent, RESULT_SPEECH);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            _witListener = (IWitListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement IWitListener");
        }
    }

    public void setWitListener(IWitListener listener) {
        _witListener = listener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == Activity.RESULT_OK && null != data) {
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    captureTextIntent(text != null ? text.get(0) : null);
                }
                break;
            }
        }
    }

    /**
     * Returns the meaning extracted from an audio stream
     * @param audio The audio stream to send over to WIT.AI
     * @param contentType The content-type of the audio
     */
    public void streamSpeech(InputStream audio, String contentType){
        if (audio == null) {
            _witListener.witDidGraspIntent(null, null, null, 0, new Error("InputStream null"));
        }
        else {
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

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        _witListener.witDidGraspIntent(null, null, null, 0, new Error("Input Text null"));
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        captureTextIntent(data != null ? (String) data.get(0) : null);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

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
