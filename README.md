# <img src="/preview/logo.png" title="logo" height="80" width="80" /> Paparazzi Plugin

IntelliJ iDEA / Android Studio plugin for Android projects using the <a href="https://github.com/cashapp/paparazzi">
Paparazzi</a> library that allows recording, verifying and viewing the snapshots within the IDE.

<img src="/preview/screenshare.gif" alt="preview" title="preview"/>

Install
-----
You can install the plugin from `Preferences` -> `Plugins` and search for the plugin. You can also download the 
plugin from the <a href="https://plugins.jetbrains.com/plugin/20517-paparazzi">intelliJ iDEA Marketplace</a>.

Features
-----

- View previously-recorded golden snapshots for the currently opened test class
- View golden snapshots of the current focussed test method
- View failure diffs for the current test class or method
- Record, Verify and Delete snapshots for individual tests or for entire test class
- Zoom options for Actual Size and Fit to Window
- Fully supported for test files written in Java or Kotlin

Testing
-----
Any changes made to the plugin should be tested against the
<a href="https://github.com/cashapp/paparazzi/tree/master/sample">sample paparazzi</a> android project.

Contributing
-----
We welcome contributions, and if you're interested, have a look at the [CONTRIBUTING](CONTRIBUTING.md) document.

License
-----
paparazzi-plugin is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full text.
