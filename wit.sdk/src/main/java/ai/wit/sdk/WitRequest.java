package ai.wit.sdk;

import android.net.Uri.Builder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Created by aric on 10/28/14.
 */
public class WitRequest {
    public static String scheme = "https";
    public static String authority = "api.wit.ai";
    public static String version = "20141022";
    private String _messageId = null;
    private IWitListener _witListener;
    private JsonObject _context;

    public WitRequest(IWitListener witListener, JsonObject context) {
        _witListener = witListener;
        _context = context;

    }

    public Builder getBase()
    {
        Builder uriBuilder = new Builder();

        uriBuilder
                .scheme(scheme)
                .authority(authority)
                .appendQueryParameter("v", version);
        if (_messageId != null) {
            uriBuilder.appendQueryParameter("msg_id", _messageId);
        }

        return uriBuilder;
    }

    protected Builder buildUri(String endpoint) {

        Builder uriBuilder;
        String messageId = _witListener.witGenerateMessageId();

        uriBuilder = this.getBase();
        uriBuilder.appendPath(endpoint);
        if (_context != null) {
            Gson gson = new Gson();
            String jsonContext = gson.toJson(_context);
            uriBuilder.appendQueryParameter("context", jsonContext);
        }
        if (messageId != null) {
            uriBuilder.appendQueryParameter("msg_id", messageId);
        }

        return uriBuilder;
    }
}
