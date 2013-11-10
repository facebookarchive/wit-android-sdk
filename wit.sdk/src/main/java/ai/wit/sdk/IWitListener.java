/*
    Copyright 2013 Wit Inc. All rights reserved.
 */
package ai.wit.sdk;

import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * Created by Wit on 7/13/13.
 */
public interface IWitListener {
    void witDidGraspIntent(String intent, HashMap<String, JsonObject> entities, String body, double confidence, Error error);
}
