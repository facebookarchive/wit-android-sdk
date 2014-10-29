package ai.wit.sdk.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * Outcome class for deserialization purpose
 * Created by Wit on 5/30/13.
 */
public class WitOutcome {

    @SerializedName("intent")
    private String _intent;

    @SerializedName("entities")
    private HashMap<String, JsonElement> _entities;

    @SerializedName("_text")
    private String _text;

    @SerializedName("confidence")
    private double _confidence;

    public double get_confidence() {
        return _confidence;
    }

    public String get_intent() {
        return _intent;
    }

    public HashMap<String, JsonElement> get_entities() {
        return _entities;
    }

    public String get_text() {
        return _text;
    }

    //  {
    //        "intent": "restaurant_lookup",
    //        "entities": {
    //    "cuisine": {
    //        "value": "chinese",
    //        "type": "cuisines",
    //        "body": "from Pekin"
    //        },
    //        "location": {
    //        "value": [
    //        90.785,
    //        45.897
    //        ],
    //        "type": "location",
    //        "body": "close to Montmartre"
    //        }

}
