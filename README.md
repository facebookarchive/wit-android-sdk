Wit-Android
===========

Wit SDK for Android

How to build
------------

To build the jar & res files

```bash
$ gradle jar
```

This will output 2 files `wit.sdk.jar` and `wit.res.zip` into `wit.sdk/output/` directory

How to use
----------

To use our sdk you need to add it as a dependencies of your Android project.

##Gradle dependencies

```
    compile('com.google.code.gson:gson:2.2.4')
    compile('org.apache.directory.studio:org.apache.commons.io:2.4')
    compile('com.android.support:appcompat-v7:+')
```


##Add the Wit fragment into your application

See [Wit Documentation](https://wit.ai/docs/android-tutorial)