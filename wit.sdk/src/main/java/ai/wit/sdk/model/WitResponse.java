package ai.wit.sdk.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class that represent the Wit response from the server.
 * It uses Gson annotation for the deserialization
 * Created by Wit on 5/30/13.
 */
public class WitResponse implements Serializable {

    @SerializedName("msg_id")
    private String _msgId;

    @SerializedName("outcomes")
    private ArrayList<WitOutcome> _outcomes;


    public String getMsgId() {
        return _msgId;
    }

    public ArrayList<WitOutcome> getOutcomes() {
        return _outcomes;
    }
}