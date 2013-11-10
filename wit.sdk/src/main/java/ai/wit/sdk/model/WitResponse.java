package ai.wit.sdk.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Class that represent the Wit response from the server.
 * It uses Gson annotation for the deserialization
 * Created by Wit on 5/30/13.
 */
public class WitResponse implements Serializable {

    @SerializedName("msg_id")
    private String _msgId;

    @SerializedName("outcome")
    private WitOutcome _outcome;

    @SerializedName("msg_body")
    private String _body;

    public String getMsgId() {
        return _msgId;
    }

    public WitOutcome getOutcome() {
        return _outcome;
    }

    public String getBody() {
        return _body;
    }
}