CWAC-Cam2: Taking Pictures. Made (Somewhat) Sensible. Again.
============================================================

Taking picturesusing a third-party app is fairly straightforward,
using `ACTION_IMAGE_CAPTURE`. However, different camera
apps have slightly different behavior, meaning that you are prone to getting
inconsistent results.

Taking pictures using the Android SDK camera classes directly is
eminently possible, but is full of edge and corner cases, not to mention
its own set of per-device idiosyncracies. Plus, there are now two
separate APIs for this.

`CWAC-Cam2` is an effort to create an in-app `ACTION_IMAGE_CAPTURE`
workalike, with a bit more configurability. You still integrate by
opening up a separate activity (`CameraActivity`, in this case), but
it is all within your own app, rather than relying upon device- or
user-specific third-party camera apps.

Library Objectives
------------------
The #1 objective of this library is maximum compatibility with hardware. As such,
this library will not be suitable for all use cases.

The targeted use case is an app that might otherwise have relied upon
`ACTION_IMAGE_CAPTURE`, but needs greater reliablilty and somewhat greater
control (e.g., capture images directly to internal storage).

If you are trying to write "a camera app" &mdash; an app whose primary job is
to take pictures &mdash; this library may be unsuitable for you.

Installation
------------
To integrate the core AAR, the Gradle recipe is:

```groovy
repositories {
    maven {
        url "https://repo.commonsware.com.s3.amazonaws.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:cam2:0.1.+'
}
```

The `cam2` artifact depends on some other libraries, available in
JCenter or Maven Central. They should be pulled down automatically
when you integrate in the `cam2` AAR.

You are also welcome to clone this repo and use the `camera/` Android
library project2 in source form.

Basic Usage
-----------

The only supported API at the moment is through
[`CameraActivity` and its `IntentBuilder`](docs/CameraActivity.md).

While there are other `public` classes and methods in the library,
ones that may be exposed as part of a public API in the future,
they are not supported at present.

Tested Devices
--------------

The [compatibility status page](docs/CompatibilityStatus.md) outlines
what devices have been tested with this library by the library author.

Dependencies
------------
The `cam2` artifact depends upon `com.github.clans:fab` (for a floating
action button and floating action menu implementation) and
`de.greenrobot:eventbus` (for internal communications within the
library). Both are listed as dependencies in the AAR artifact metadata
and should be added to your project automatically.

Version
-------
This is version v0.1.0 of this library, which means it is brand new.

Demo
----
There are two demo projects.

One is `demo/`. This illustrates taking pictures using the front
and rear-facing cameras. More importantly, it serves as a way of
collecting information about a device, particularly if you are
going to [file a bug report](CONTRIBUTING.md).

The `demo-playground/` sample project displays a `PreferenceFragment`
where you can tweak various `IntentBuilder` configurations, then tap
on an action bar item to take a picture using those settings. This is
good for experimenting with the `CameraActivity` capabilities.

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+camera). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please read [the contribution guidelines](CONTRIBUTING.md), then
post an [issue](https://github.com/commonsguy/cwac-cam2/issues).
**Be certain to include complete steps for reproducing the issue.**

Do not ask for help via social media.

Release Notes
-------------
- v0.1.0: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

