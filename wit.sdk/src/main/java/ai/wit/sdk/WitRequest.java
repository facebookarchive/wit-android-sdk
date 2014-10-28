package ai.wit.sdk;

import android.net.Uri.Builder;

/**
 * Created by aric on 10/28/14.
 */
public class WitRequest {
    public static String scheme = "https";
    public static String authority = "api.wit.ai";
    public static String version = "20140923";

    private String _messageId = null;

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

    public void setMessageId(String messageId) {
        _messageId = messageId;
    }

}
