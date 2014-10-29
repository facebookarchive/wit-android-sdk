/*
    Copyright 2013 Wit Inc. All rights reserved.
 */
package ai.wit.sdk;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;

import ai.wit.sdk.model.WitOutcome;

/**
 * Created by Wit on 7/13/13.
 */
public interface IWitListener {

    /**
     * Called when the Wit request is completed.
     * @param outcomes ArrayList of model.WitOutcome - null in case of error
     * @param messageId String containing the message id - null in case of error
     * @param error - Error, null if there is no error
     */
    void witDidGraspIntent(ArrayList<WitOutcome> outcomes,  String messageId, Error error);

    /**
     * Called when the streaming of the audio data to the Wit API starts.
     * The streaming to the Wit API starts right after calling one of the start methods when
     * detectSpeechStop is equal to Wit.vadConfig.disabled or Wit.vadConfig.detectSpeechStop.
     * If Wit.vad is equal to Wit.vadConfig.full, the streaming to the Wit API starts only when the SDK
     * detected a voice activity.
     */
    void witDidStartListening();


    /**
     * Called when Wit stop recording the audio input.
     */
    void witDidStopListening();

    /**
     * When using the hands free voice activity detection option (Wit.vadConfig.full), this callback will be called when the microphone started to listen
     * and is waiting to detect voice activity in order to start streaming the data to the Wit API.
     * This function will not be called if the Wit.vad is not equal to Wit.vadConfig.full
     */
    void witActivityDetectorStarted();

    /**
     * Using this function allow the developer to generate a custom message id.
     * Example: return "CUSTOM-ID" + UUID.randomUUID.toString();
     * If you want to let the Wit API generate the message id, you can just return null;
     * @return a unique (String) UUID or a null
     */
    String witGenerateMessageId();
}
