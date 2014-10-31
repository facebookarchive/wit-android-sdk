package ai.wit.sdk;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import android.content.Context;

/**
 * Created by aric on 10/29/14.
 */
public class WitContextSetter {

    private static String KEY_REFTIME = "reference_time";
    private static String KEY_LOCATION = "location";
    private Context _androidContext;

    public WitContextSetter(JsonObject contextData, Context androidContext) {
        _androidContext = androidContext;
        ensureTime(contextData);
        ensureLocation(contextData);
    }

    protected void ensureTime(JsonObject context) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        String nowAsISO8601 = df.format(new Date());
        context.addProperty(KEY_REFTIME, nowAsISO8601);
    }

    protected void ensureLocation(JsonObject context) {
        if (_androidContext == null) {
            return ;
        }
        Location loc = getLocation();
        if (loc == null) {
            return ;
        }
        JsonObject jsonLocation = new JsonObject();
        jsonLocation.addProperty("latitude", loc.getLatitude());
        jsonLocation.addProperty("longitude", loc.getLongitude());
        context.add(KEY_LOCATION, jsonLocation);
    }

    /**
     * This function should only be called when the _androidContext instance variable is set
     * @return Location object
     */
    protected Location getLocation() {
        LocationManager lm = (LocationManager) _androidContext.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        String locationProvider = lm.getBestProvider(criteria, true);
        if (locationProvider == null) {
            return null;
        }
        Location loc = lm.getLastKnownLocation(locationProvider);

        return loc;
    }
}
