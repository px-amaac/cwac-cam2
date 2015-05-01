# Using CameraActivity

The simplest way to use this library is to use `CameraActivity`. This
gives you the same "API" as you get with the Android SDK's
`ACTION_IMAGE_CAPTURE`, making it fairly easy for you to get existing
`ACTION_IMAGE_CAPTURE` working with your own local camera activity.

## Getting the Intent

To get the `Intent` to use, in place of your `ACTION_IMAGE_CAPTURE`
`Intent`, call the static `buildLaunchIntent()` method on the
`CameraActivity` class. There are two flavors of this method, both
taking a `Context` as as parameter.

The single-parameter version of the method will return an `Intent`
object to you. That `Intent` will either be:

- Pointing to `CameraActivity`, or

- Pointing to `ACTION_IMAGE_CAPTURE`, if your app or the device that
it is running on are incompatible with `CameraActivity`

You can then add extras on the `Intent` and use it with `startActivityForResult()`,
just as you would use the regular `ACTION_IMAGE_CAPTURE` `Intent`:

```java
startActivity(CameraActivity.buildLaunchIntent(this));
```

The two-parameter version of this method takes a `boolean` as the
second parameter. `false` indicates that you do not want an
`ACTION_IMAGE_CAPTURE` `Intent`, and instead you want `buildLaunchIntent()`
to throw an exception if the app or device is incompatible with
`CameraActivity`.

## Supported Extras

TBD

## Output

TBD

## Configuring the Manifest Entry

Getting all of the above working requires nothing in your manifest.
However, more often than not, you will want to change aspects of the
activity, such as its theme.

To do that, add your own `<activity>` element to the manifest, pointing
to the `CameraActivity` class, and add in whatever attributes or child
elements that you need.

For example, the following manifest entry sets the theme:

```xml
<activity
      android:name="com.commonsware.cwac.cam2.CameraActivity"
      android:theme="@style/AppTheme"/>
```

