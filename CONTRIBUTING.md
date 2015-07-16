# How To Contribute

If you have an interest in contributing to this project, here is some advice for packaging your contribution. Thahks!

## Contributing Bug Reports

Most likely the most common contribution to a library like this will be bug reports. These come in two varieties: stuff that
probably pertains to camera hardware, and other sorts of bugs.

### Bug Reports about Camera Picture Output

If pictures taken using this library are coming out incorrectly (garbled, incorrect colors, too-small sizes, etc.), the
best way to document that bug is by running the `demo`/ app from the project and attaching the ZIP archive it generates
to your [issue](https://github.com/commonsguy/cwac-cam2/issues). The `demo/` app not only takes four pictures (two per
orientation, two per camera) and includes them in the ZIP archive, but the ZIP also contains a JSON file with some
hardware details to help identify your hardware.

If you cannot reproduce the bug using the demo app, but you can reproduce using your own app, include the
`CameraActivity.IntentBuilder` configuration code that you are using, along with any other code snippets that you think
might pertain to the problem, when you post your [issue](https://github.com/commonsguy/cwac-cam2/issues).

### Bug Reports about Camera Preview Output

Unfortunately, the `demo/` app is not presently capturing previews and including them in the ZIP. If you can
reproduce the preview bug using the `demo/` app, try to take a screenshot or otherwise capture an image of what
you are seeing in the preview that is a problem, and include it in your
[issue](https://github.com/commonsguy/cwac-cam2/issues).

If you cannot reproduce the problem with the `demo/` app, but you can reproduce using your own app, include the
`CameraActivity.IntentBuilder` configuration code that you are using, along with any other code snippets that you think
might pertain to the problem, and your screenshot illustrating the preview problem,
when you post your [issue](https://github.com/commonsguy/cwac-cam2/issues).

### Other Bug Reports

If you are encountering other problems, particularly those that would not appear to be hardware-specific (e.g.,
the same problem occurs on multiple devices or is not really tied to camera hardware), just file
an [issue](https://github.com/commonsguy/cwac-cam2/issues) with your best explanation of the problem and
the steps to reproduce it. If you cannot reproduce it using the `demo/` or `demo-playground/` sample projects,
be sure to include code that can reproduce it.

## Contributing Feature Requests

If you have an idea for how to make the library better, feel free to file an [issue](https://github.com/commonsguy/cwac-cam2/issues).
You will have better luck in getting the feature implemented if you can "sell" the library author
on how this feature will help the library author.

## Contributing Code

This project is not set up to accept much in the way of pull requests or other code contributions, for
licensing and contractual reasons, not technical ones.

If you send over a pull request, and it is a *de minimis* fix (i.e., there is no other reasonable way to
affect the change other than how you did it), your pull request may be accepted and merged in directly.
Otherwise, pull requests or other code samples will be used as inspiration for a fresh implementation,
rather than used directly.
