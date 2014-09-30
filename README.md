Wit-Android
===========

The Wit.ai Android SDK is a the easiest way to integrate Wit.ai features into your Android application.

The SDK can capture intents and entities:

- by listening to the microphone
- From a text (using a Java `String`)
- From an audio stream (java `java.io.InputStream`)

If you are looking for a very detailed / quick start tutorial on how to use this SDK, look at the documentation on our [website](https://wit.ai/docs/android/quickstart)


How to build
------------

To build the jar and resources files

```bash
$ gradle jar
```

This will output 2 files `wit.sdk.jar` and `wit.res.zip` into `wit.sdk/output/` directory

Using the Wit.ai SDK
--------------------

Copy the jar file `wit.sdk.jar` under the `app/libs` directory of your project.


###Initialization

```Java
import ai.wit.sdk.Wit;
import ai.wit.sdk.IWitListener;
```

Implement the `IWitListener` interface:

```java
public class MyActivity extends ActionBarActivity implements  IWitListener {
    Wit _wit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		//...
        String accessToken = "YOUR WIT ACCESS TOKEN";
        _wit = new Wit(accessToken, this);
	}
	
    @Override
    public void witDidGraspIntent(String intent, HashMap<String, JsonElement> entities, String body, double confidence, Error error) {
        Log.d("MyActivity", "Wit did grasp intent!");
    }
    
    @Override
    public void witDidStartListening() {
        TextView t = (TextView) findViewById(R.id.monitor);
        t.setText("Listening Started!");


    }

    @Override
    public void witDidStopListening() {
        TextView t = (TextView) findViewById(R.id.monitor);
        t.setText("Listening Stopped!");
    }

}
```

- `Wit(String accessToken, IWitListener witListener)`
- `witDidGraspIntent` is called when the result from the Wit API is returned.
- `witDidStartListening` is called when Wit starts to listen to the microphone
- `witDidStopListening` is called when Wit stops to listen to the microphone

###Listening to the microphone

To start listening on the microphone, call the `startListening()` funciton on the Wit instance:

```java
_wit.startListening()
```

To stop listening on the microphone, you have two options:
- Use the voice activity detection feature builtin in the SDK : `_wit.detectSpeechStop = true;`
- Call the stop function: `_wit.stopListening();`

The function `witDidGraspIntent` will be called on the class implementing the `IWitListener` interface when the result is returned by the api.



###From Text / String
To capture an intent from a string, call the `_wit.captureTextIntent(String text)` function. 
```java
String text = "Wake me up tomorrow at 7am";
_wit.captureTextIntent(text);
```
The function `witDidGraspIntent` will be called on the class implementing the `IWitListener` interface when the result is returned by the api.

###From an audio stream

To capture an intent from an audio stream, call the `streamRawAudio`.

Here is the prototype of the function: `streamRawAudio(InputStream audio, String encoding, int bits, int rate, ByteOrder order)` and here is how the SDK use it internally to send the audio stream from the microphone:

```java
PipedInputStream in = [...];
streamRawAudio(in, "signed-integer", 16, 16000, ByteOrder.LITTLE_ENDIAN);
```
The function `witDidGraspIntent` will be called on the class implementing the `IWitListener` interface when the result is returned by the api.


Gradle dependencies
-------------------

```
    compile('com.google.code.gson:gson:2.2.4')
    compile('org.apache.directory.studio:org.apache.commons.io:2.4')
    compile('com.android.support:appcompat-v7:+')
```

