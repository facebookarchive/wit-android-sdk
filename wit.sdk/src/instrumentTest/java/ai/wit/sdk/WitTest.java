package ai.wit.sdk;

import android.app.Activity;
import android.test.AndroidTestCase;

import com.google.gson.JsonElement;

import java.util.HashMap;

/*
    Copyright 2013 Wit Inc. All rights reserved.
 */

/**
 * Created by Wit on 7/14/13.
 */
public class WitTest extends AndroidTestCase{

    @Override
    protected void setUp() throws java.lang.Exception
    {
        super.setUp();
    }

    public void testWitCaptureTextCorrectlyWithTextNull() {
        Wit wit = new Wit();
        WitListenerTest listener = new WitListenerTest();
        wit.onAttach(listener);
        wit.captureTextIntent(null);
        assertEquals(null, listener._intent);
        assertEquals(null, listener._body);
        assertEquals(null, listener._entities);
        assertEquals(0.0, listener._confidence);
        assertNotNull(listener._error);
    }

    private class WitListenerTest extends Activity implements IWitListener {

        String _intent;
        HashMap<String, JsonElement> _entities;
        String _body;
        double _confidence;
        Error _error;

        @Override
        public void witDidGraspIntent(String intent, HashMap<String,JsonElement> entities, String body, double confidence, Error error) {
            _intent = intent;
            _entities = entities;
            _body = body;
            _confidence = confidence;
            _error = error;
        }
    }
}
