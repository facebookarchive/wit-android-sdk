# wit-android-sdk

The SDK can capture intents and entities:

- from the microphone of the device
- from a text (using a Java `String`)
- from an audio stream (using a Java `java.io.InputStream`)

## How to build

To build the jar and resources files

```bash
$ gradle buildzip
```

This will output a zip file (`wit.sdk/build/wit-android-sdk.zip`) containing:

- `wit.sdk.jar`
- `jniLibs` build of our C library for each platform
- `layout` and `drawable-*` resources

## Quick start

See `quickstart.md`

## API


```java
/**
 * Configure the voice activity detection algorithm:
 * - Wit.vadConfig.disable
 * - Wit.vadConfig.detectSpeechStop (default)
 * - Wit.vadConfig.full
 */
public vadConfig vad = vadConfig.detectSpeechStop;
```

```java
/**
 * Constructor
 * @param accessToken the Wit access Token
 * @param witListener The class implementing the IWitListener interface to receive callback from
 *                    the wit SDK
 */    
public Wit(String accessToken, IWitListener witListener)
```

```java
/**
 * Starts a new recording session. witDidGraspIntent() will be called once completed.
 * @throws IOException
 */
public void startListening() throws IOException
```

```java
/**
 * Stops the current recording if any, which will lead to a call to witDidGraspIntent().
 */
public void stopListening()
```

```java

/**
 * Start / stop the audio processing. Once the API response is received, witDidGraspIntent() method will be called.
 * @throws IOException
 */
public void toggleListening() throws IOException
```

```java

/**
 * Stream audio data from a InputStream to the Wit API.
 * Once the API response is received, witDidGraspIntent() method will be called.
 * @param audio The audio stream to send over to Wit.ai
 * @param encoding The encoding for this raw audio // Android usually uses 'signed-integer'
 * @param bits The bits of the audio // Android usually uses 16
 * @param rate The rate of the audio // Android usually uses 8000
 * @param order The byte order of the audio // Android usually uses LITTLE_ENDIAN
 */
public void streamRawAudio(InputStream audio, String encoding, int bits, int rate, ByteOrder order)
```

```java

/**
 * Sends a String to wit.ai for interpretation. Same as sending a voice input, but with text.
 * @param text text to extract the meaning from.
 */
public void captureTextIntent(String text)
```

```java

/**
 * Set the context for the next requests. Look at our http api documentation
 * to get more information about context (https://wit.ai/docs/http/20140923#context-link)
 * The reference_time property is automatically set by the SDK (if null)
 * The (GPS) location property is set by the SDK if it is enabled using the method
 * enableContextLocation (if null)
 *
 * @param context a JsonObject - here is an example of how to build it:
 *                        context.addProperty("timezone", "America/Los_Angeles");
 *                        OtherJsonObject = new JsonObject();
 *                        OtherJsonObject.addProperty("latitude", -35.23);
 *                        OtherJsonObject.addProperty("longitude", 59.10);
 *                        context.add("location", OtherJsonObject);
 */
public void setContext(JsonObject context)
```

```java

/**
 * Enabling the context location will add the GPS coordinates to the _context object to all
 * Wit requests (speech and text requests).
 * This can help the Wit API to resolve some entities like the Location entity
 * @param androidContext android.context.Context needed to call the
 *                       android.location.LocationManager
 */
public void enableContextLocation(Context androidContext)
```

##### Implementing the `IWitListener` interface
```java
public interface IWitListener {
  /**
   * Called when the Wit request is completed.
   * @param outcomes ArrayList of model.WitOutcome - null in case of error
   * @param messageId String containing the message id - null in case of error
   * @param error - Error, null if there is no error
   */
  void witDidGraspIntent(ArrayList<WitOutcome> outcomes,  String messageId, Error error);

  /**
   * Called when the streaming of the audio data to the Wit API starts.
   * The streaming to the Wit API starts right after calling one of the start methods when
   * detectSpeechStop is equal to Wit.vadConfig.disabled or Wit.vadConfig.detectSpeechStop.
   * If Wit.vad is equal to Wit.vadConfig.full, the streaming to the Wit API starts only when the SDK
   * detected a voice activity.
   */
  void witDidStartListening();

  /**
   * Called when Wit stop recording the audio input.
   */
  void witDidStopListening();

  /**
   * When using the hands free voice activity detection option (Wit.vadConfig.full), this callback will be called when the microphone started to listen
   * and is waiting to detect voice activity in order to start streaming the data to the Wit API.
   * This function will not be called if the Wit.vad is not equal to Wit.vadConfig.full
   */
  void witActivityDetectorStarted();

  /**
   * Using this function allow the developer to generate a custom message id.
   * Example: return "CUSTOM-ID" + UUID.randomUUID.toString();
   * If you want to let the Wit API generate the message id, you can just return null;
   * @return a unique (String) UUID or a null
   */
  String witGenerateMessageId();
}
```

###### From the microphone of the device

To start listening on the microphone, call the `startListening()` function on the Wit app:

```java
_wit.startListening()
```

To stop listening on the microphone, you have two options:
- Use the voice activity detection feature builtin in the SDK : `_wit.detectSpeechStop = true;`
- Call the stop function: `_wit.stopListening();`

The function `witDidGraspIntent` will be called on the class implementing the `IWitListener` interface when the result is returned by the api.


##### From Text / String
To capture an intent from a string, call the `_wit.captureTextIntent(String text)` function.

```java
String text = "Wake me up tomorrow at 7am";
_wit.captureTextIntent(text);
```
The function `witDidGraspIntent` will be called on the class implementing the `IWitListener` interface when the result is returned by the api.      

##### From an audio stream

To capture an intent from an audio stream, call the `streamRawAudio`.

Here is the prototype of the function: `streamRawAudio(InputStream audio, String encoding, int bits, int rate, ByteOrder order)` and here is how the SDK use it internally to send the audio stream from the microphone:

```java
PipedInputStream in = [...];
streamRawAudio(in, "signed-integer", 16, 16000, ByteOrder.LITTLE_ENDIAN);
```

The function `witDidGraspIntent` will be called on the class implementing the `IWitListener` interface when the result is returned by the api.        
