Wit-Android 3.2.0
===========

The Wit.ai Android SDK is the easiest way to integrate Wit.ai features into your Android application.

The SDK can capture intents and entities from multiple sources:

- the microphone of the device
- text (using a Java `String`)
- an audio stream (using a Java `java.io.InputStream`)

How to build
------------

To build the jar and resources files

```bash
$ gradle buildzip
```

This will output a zip file (`wit.sdk/build/wit-android-sdk.zip`) containing: 

- `wit.sdk.jar`
- `jniLibs` build of our C library for each platform
- `layout` and `drawable-*` resources


How to use
----------

You will find on our website a quick start guide and a fully detailed documentation of our Android SDK: [https://wit.ai/docs/android/3.2.0](https://wit.ai/docs/android/3.2.0)
