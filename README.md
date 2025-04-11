# ![Logo](app/logo.png) FlickBoard - The Predictable Keyboard

(Logo by @MisterE123)

[![Discord](https://img.shields.io/discord/1207841722327896104?logo=discord&logoColor=white&label=chat%20on%20discord)](https://discord.gg/tVp8MGaeUr)

FlickBoard is a keyboard for Android that doesn't try to correct or predict you.
It doesn't have an opinion on the correct spelling or tone. It certainly doesn't use any AI.
Because it trusts you to write what you meant to write.

Instead, FlickBoard uses swipes to get away with using vastly fewer (and larger!) buttons than
a traditional QWERTY keyboard, helping you build muscle memory, minimize typos, and touch type
faster.

Type the letters in the center of a key by tapping it. Type the keys in the corners by swiping in
that direction. It's that simple!

FlickBoard is inspired by [MessagEase] and [Thumb-Key].

## How do I install it?

| <a href="https://android.izzysoft.de/repo/apk/se.nullable.flickboard"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid"/></a> | <a href="https://f-droid.org/packages/se.nullable.flickboard"><img src="https://f-droid.org/badge/get-it-on.svg" alt="Get it on F-Droid"></a> | <a href="https://play.google.com/store/apps/details?id=se.nullable.flickboard"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play"/></a> |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

The latest release builds are also available
from [Codeberg Releases](https://codeberg.org/natkr/flickboard/releases), but I recommend
downloading from one of the above instead to get automatic updates.

If downloading from a source other than Google Play, you can verify using `apksigner verify` that
the
certificate matches the following SHA-256 digest:
`51612a908aaf90aff66d1260a01992b3dde73a3ee3a94069412be2012272020f`.
This should also match the digest listed at [natkr.com](https://natkr.com/flickboard/certificate/).

You can also build it yourself, by running the following on a computer that is connected to your
phone:

```bash
$ ./gradlew :app:installPlainDebug
```

This requires the phone to have USB debugging enabled, and your computer to have the [Android SDK]
installed.

## What does it look like?

| ![Screenshot of the keyboard in use](fastlane/metadata/android/en-US/images/phoneScreenshots/2.png) | ![Screenshot of settings panel](fastlane/metadata/android/en-US/images/phoneScreenshots/1.png) |
|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|

## Why not MessagEase?

It's proprietary, and suddenly started requiring a subscription.

## Why not Thumb-Key?

Back in the day, Thumb-Key didn't support MessagEase's more advanced gestures like circles and
U-bends.

They do now, so this section is mostly historical, but FlickBoard's detection might still feel
different, in ways that people may or may not prefer.

[MessagEase]: https://www.exideas.com/ME/index.php

[Thumb-Key]: https://github.com/dessalines/thumb-key

[Android SDK]: https://developer.android.com/studio
