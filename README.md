# Simple Webview Boilerplate for Android Studio

This is an Android Studio Universal project that allows you to wrap your website or web app in a super simple Android app.

It is compatible with Android devices running Ice Cream Sandwich and higher (API 22).

I might make this boilerplate fully backwards-compatible down the road.

# Setup
- Clone this repo, or hit "Download ZIP" in the Github sidebar.

### Local mode (recommended)
- Replace the stuff in `web_content` with your own mobile website.
- Hit Run!

### Internet mode
- In `MainActivity.java` (see below screenshot), switch around the commented out URL lines.
- Set your URL...
- Hit Run!

![MainActivity.java location within the boilerplate](https://github.com/nabilfreeman/android-webview-boilerplate/raw/new-version/readme-screenshots/files.png)

### Bonus settings

You can enable/disable Javascript, and also enable-disable the opening of a full browser when a user clicks a link in `MainActivity.java` (see above screenshot).

# Why?

So you can easily build an Android app in HTML/CSS/JS without using Cordova. And, as awesome as the Chrome home screen bookmark stuff is, for certain projects it might make a lot more sense if people can search through the Google Play / Amazon store.

HTML web apps are totally awesome for layout content and typography. Tons of big apps like Instagram use HTML layouts and CSS styling for parts of their apps.

If you are a web developer who wants to release an Android app, this should help you cut some corners when it comes to learning Java.

# Ideal use

Single page web app with on-screen/no navigation. For example, a game, interactive page, web based slideshow, etc.
