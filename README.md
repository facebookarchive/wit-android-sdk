Wit-Android
===========

Wit SDK for Android

How to use
----------

To use our sdk you need to add it as a dependencies of your Android project.

##Gradle dependencies

TODO based on Kuang11 application


##Add the Wit fragment into your application

The next step is to add the wit fragment into your application.
To do so you will have to add the button into the activity view you want to add Wit into. Add the following
snippet inside this file:

```xml
            <fragment
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:name="ai.wit.sdk.Wit"
                tools:layout="@layout/wit_button"
                android:tag="wit_fragment"/>
```

This will add our fragment into your UI. You then need to configure our fragment so it can communicate
with your Wit instance. You need to change your Activity onCreate function and make it look likes this:

```java
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //...

        //Initialize Fragment
        Wit wit_fragment = (Wit) getFragmentManager().findFragmentByTag("wit_fragment");
        if (wit_fragment != null) {
            wit_fragment.setAccessToken(<YOUR_ACCESS_TOKEN>);
        }
    }
```

If you want to configure the button you can easily do so by adding a file named 'wit_button.xml' in
`src/main/res/layout/`. This file should contains a Button/ImageButton or any View type you want.