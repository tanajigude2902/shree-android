#!/bin/sh
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
APP_HOME=`dirname "$0"`
if command -v gradle >/dev/null 2>&1 ; then
    exec gradle "$@"
elif [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    exec "$JAVACMD" -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
else
    gradle "$@"
fi
