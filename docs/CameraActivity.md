# Using CameraActivity

The simplest way to use this library is to use `CameraActivity`. This
gives you the same "API" as you get with the Android SDK's
`ACTION_IMAGE_CAPTURE`, making it fairly easy for you to get existing
`ACTION_IMAGE_CAPTURE` working with your own local camera activity.

## Getting the Intent

The simplest way to craft the right `Intent` to use is to create
a `CameraActivity.IntentBuilder`, call whatever configuration methods
that you want on that builder, and have it `build()` you an `Intent`.
That `Intent` can be used with `startActivityForResult()`, just as you
might have used it with an `ACTION_IMAGE_CAPTURE` `Intent`.

Under the covers, `CameraActivity.IntentBuilder` is simply packaging a
series of extras on the `Intent`, so you can always put those extras
on yourself if you so choose. The following table lists the available
configuration methods on `CameraActivity.IntentBuilder`, the corresponding
extra names (defined as constants on `CameraActivity`), their default values,
and what their behavior is:

| `IntentBuilder` Method | Extra Key                 | Data Type                                 | Purpose |
|:----------------------:|:-------------------------:|:-----------------------------------------:|---------|
| `debug()`              | `EXTRA_DEBUG_ENABLED`     | `boolean`                                 | Indicate if extra debugging information should be dumped to LogCat (default is `false`) |
| `facing()`             | `EXTRA_FACING`            | `CameraSelectionCriteria.Facing`          | Indicate the preferred camera to start with (`BACK` or `FRONT`, default is `BACK`) |
| `skipConfirm()`        | `EXTRA_CONFIRM`           | `boolean`                                 | Indicate if the user should be presented with a preview of the image and needs to accept it before proceeding (default is to show the confirmirmation screen) |
| `to()`                 | `MediaStore.EXTRA_OUTPUT` | `Uri` (though `to()` also accepts `File`) | Destination for picture to be written, where `null` means to return a thumbnail via the `data` extra (default is `null`) |

## Example Use of `IntentBuilder`

```java
  Intent i=new CameraActivity.IntentBuilder(MainActivity.this)
      .facing(CameraSelectionCriteria.Facing.FRONT)
      .to(new File(testRoot, "portrait-front.jpg"))
      .skipConfirm()
      .debug()
      .build();

  startActivityForResult(i, REQUEST_PORTRAIT_FFC);
```

## Output

If you provide the destination `Uri` via `to()`, the image will be written there, and the `Uri` of the `Intent`
delivered to `onActivityResult()` will be your requested `Uri`.

If you do not provide the destination `Uri`, a thumbnail image will be supplied via the `data` extra on the `Intent` delivered to `onActivityResult()`.

And, of course, the `resultCode` passed to `onActivityResult()` will indicate if the user took a picture or abandoned the operation.

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

Note that `CameraActivity` does not support being exported. Do not add
an `<intent-filter>` to this activity or otherwise mark it as being
exported.
