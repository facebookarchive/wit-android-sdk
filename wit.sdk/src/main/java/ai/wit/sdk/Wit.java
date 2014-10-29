/*
    Copyright 2013 Wit Inc. All rights reserved.
 */

package ai.wit.sdk;


import android.content.Context;
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
    private String _accessToken;
    private IWitListener _witListener;
    private WitMic _witMic;
    private PipedInputStream _in;
    private JsonObject _context = new JsonObject();
    private Context _androidContext;

    /**
     * Configure the voice activity detection algorithm:
     * - Wit.vadConfig.disable
     * - Wit.vadConfig.detectSpeechStop (default)
     * - Wit.vadConfig.full
     */
    public vadConfig vad = vadConfig.detectSpeechStop;

    /**
     * Instantiating the Wit instance.
     * @param accessToken the Wit access Token
     * @param witListener The class implementing the IWitListener interface to receive callback from
     *                    the wit SDK
     */
    public Wit(String accessToken, IWitListener witListener) {
        _accessToken = accessToken;
        _witListener = witListener;
    }

    /**
     * Starts a new recording session. witDidGraspIntent() will be called once completed.
     * @throws IOException
     */
    public void startListening() throws IOException {
        WitContextSetter witContextSetter = new WitContextSetter(_context, _androidContext);
        _witMic = new WitMic(this, vad);
        _witMic.startRecording();
        _in = _witMic.getInputStream();
        if (vad != vadConfig.full) {
            voiceActivityStarted();
        } else {
            _witListener.witActivityDetectorStarted();
        }
    }

    /**
     * Stops the current recording if any, which will lead to a call to witDidGraspIntent().
     */
    public void stopListening() {

        _witMic.stopRecording();
        _witListener.witDidStopListening();
    }

    @Override
    public void voiceActivityStarted() {
        streamRawAudio(_in, "signed-integer", 16, WitMic.SAMPLE_RATE, ByteOrder.LITTLE_ENDIAN);
        _witListener.witDidStartListening();
    }

    /**
     * Start / stop the audio processing. Once the API response is received, witDidGraspIntent() method will be called.
     * @throws IOException
     */
    public void toggleListening() throws IOException {
        if (_witMic == null || !_witMic.isRecording()) {
            startListening();
        } else {
            stopListening();
        }
    }


    /**
     * Stream audio data from a InputStream to the Wit API.
     * Once the API response is received, witDidGraspIntent() method will be called.
     * @param audio The audio stream to send over to Wit.ai
     * @param encoding The encoding for this raw audio // Android usually uses 'signed-integer'
     * @param bits The bits of the audio // Android usually uses 16
     * @param rate The rate of the audio // Android usually uses 8000
     * @param order The byte order of the audio // Android usually uses LITTLE_ENDIAN
     */
    public void streamRawAudio(InputStream audio, String encoding, int bits, int rate, ByteOrder order) {
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
     * Sends a String to wit.ai for interpretation. Same as sending a voice input, but with text.
     * @param text text to extract the meaning from.
     */
    public void captureTextIntent(String text) {

        if (text == null)
            _witListener.witDidGraspIntent(null, null, new Error("Input Text null"));
        WitContextSetter witContextSetter = new WitContextSetter(_context, _androidContext);
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

    /**
     * Set the context for the next requests. Look at our http api documentation
     * to get more information about context (https://wit.ai/docs/http/20140923#context-link)
     * The reference_time property is automatically set by the SDK (if null)
     * The (GPS) location property is set by the SDK if it is enabled using the method
     * enableContextLocation (if null)
     *
     * @param context a JsonObject - here is an example of how to build it:
     *                        context.addProperty("timezone", "America/Los_Angeles");
     *                        OtherJsonObject = new JsonObject();
     *                        OtherJsonObject.addProperty("latitude", -35.23);
     *                        OtherJsonObject.addProperty("longitude", 59.10);
     *                        context.add("location", OtherJsonObject);
     */
    public void setContext(JsonObject context) {
        _context = context;
    }

    /**
     * Enabling the context location will add the GPS coordinates to the _context object to all
     * Wit requests (speech and text requests).
     * This can help the Wit API to resolve some entities like the Location entity
     * @param androidContext android.context.Context needed to call the
     *                       android.location.LocationManager
     */
    public void enableContextLocation(Context androidContext) {
        _androidContext = androidContext;
    }
}
