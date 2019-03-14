<p align="center">
	<img src="https://ds9bjnn93rsnp.cloudfront.net/assets/temp/crashlife_logo_github-45fd44376f131c331d787105fbe6814d5c3e9149372d0a26b891924cefa08032.png" width=300 />
</p>

[![Twitter](https://img.shields.io/badge/twitter-@BuglifeApp-blue.svg)](https://twitter.com/buglifeapp)

Crashlife is an awesome crash reporting SDK & web platform for Android apps. Here's how it works:

1. Your app crashes and relaunches
2. Crashlife sends the crash report to the Crashlife web dashboard
3. There is no step 3.

You can also find Crashlife for iOS [here](https://github.com/buglife/crashlife-ios) (coming soon!)


---

|   | Main Features |
|---|---------------|
| 📖 | Open source |
| 🏃🏽‍♀️ | Fast & lightweight |
| 📜 | Custom attributes  |
| ℹ️ | Captured footprints and logs, with info / warning / error levels |

## Installation

1. Add `crashlife-android` as a dependency to your app's `build.gradle`.

	```groovy
	dependencies {
		implementation 'com.buglife.sdk:crashlife-android:1.0.4'
	}
	```

2. Create an `Application` subclass in your project if you don't already have one. (And don't forget to add it to your app's `AndroidManifest.xml`.)

3. Add the following lines to the end of the `onCreate()` method in your app's `Application` subclass:
	
	```java
	Crashlife.initWithApiKey(this, "YOUR_API_KEY_HERE");
	```

4. Build & run on a device; and crash your app (hopefully deliberately)!

5. On relaunch, your crash report will be submitted; then go to the Crashlife web dashboard. 

## Usage

### Crash reporting

Once initialized, Crashlife will watch for both uncaught JVM exceptions, as well as C/C++ crashes from your native code. Crash reports will be saved to disk and sent on next launch. If the crash report can't be submitted for any reason, it will persist on disk until it can be sent at launch.

Additionally, Crashlife supports logging caught exceptions and error, warning, and info messages to the web dashboard as individual events. 


### Caught exception reporting

Crashlife can log caught exceptions will a full stack trace. From your catch block, call

```java
Crashlife.logException(exception);
```

You can also pass a new (not yet-thrown) exception, however for performance reasons, it will not contain a stack trace. Crashlife will attempt to send the exception event immediately, and will cache it in case it is unable to do so. 

### Error/Warning/Info events

Crashlife supports logging messages of the severity of your choice: Crash, Error, Warning, and Info.

```java
// Log an error
Crashlife.logError("An error occurred: ...");

// Log a warning
Crashlife.logWarning("Warning: doing something dangerous...");

// Log an informational message
Crashlife.logInfo("Note: something weird is going on...");

// Or you can use the parameterized method:
Crashlife.log(Event.Severity.CRASH, "Some other process we care about died.");
```

### Footprints

In order to aid in reproducing crashes, you can include footprints indicating what code paths were followed in order to reach the crash or error. These footprints can include their own attributes to avoid cluttering up the custom attributes. These footprints will not be sent to the Crashlife web dashboard unless a report is made. 

```java
// Leave a footprint with no metadata
Crashlife.leaveFootprint("User navigated to screen 2");

// Leave a footprint with metadata
HashMap<String, String> attributes = new HashMap<>();
attributes.put("Developer", "You");
attributes.put("App", "Awesome");
Crashlife.leaveFootprint("User did something else", attributes);
```

### Custom Attributes

#### Adding custom attributes

You can include custom attributes (i.e. key-value pairs) to your crash reports and logged events, as such:

```java
Crashlife.putAttribute("Artist", "2Pac");
Crashlife.putAttribute("Song", "California Love");
```

#### Removing attributes

To clear an attribute, set its value to null.

```java
Crashlife.putAttribute("Artist", null);
```


### User Identification

You may set a string representing the user’s name, database ID or other identifier:

```java
String username = ...; // the current username
Crashlife.setUserIdentifier(username);
```


## License

```
Copyright (C) 2019 Buglife, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

