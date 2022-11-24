# Release process

1. Update the `version` in `build.gradle.kts`
2. Add the release version to `CHANGELOG.md`
3. Use the `buildPlugin` gradle command to generate the plugin artifact
4. Create a release via the GitHub UI
    -   Set the tag to match the version
    -   Set the title to match the version
    -   Copy the contents of the release changelog to the description field
    -   Attach the plugin artifact to the release
5. Login to Jetbrains Marketplace and upload the plugin
