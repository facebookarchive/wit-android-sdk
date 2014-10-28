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

    void witDidGraspIntent(ArrayList<WitOutcome> outcomes,  String messageId, Error error);
    void witDidStartListening();
    void witDidStopListening();
    void witActivityDetectorStarted();

    /**
     *
     * @return a unique UUID or a null
     */
    String witGenerateMessageId();
}
