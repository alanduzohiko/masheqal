#!/bin/sh
# Wrapper bootstrap script. The official gradle-wrapper.jar is intentionally not fabricated here.
echo "Gradle wrapper JAR is not bundled in this environment. Use a Gradle/Android IDE that can download Gradle 9.6.1 from the URL in gradle/wrapper/gradle-wrapper.properties." >&2
exit 2
