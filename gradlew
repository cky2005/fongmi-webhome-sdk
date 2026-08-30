#!/usr/bin/env sh

#
# Standard Gradle wrapper launcher script
#

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Resolve APP_HOME
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
APP_HOME=`dirname "$PRG"`
APP_HOME=`cd "$APP_HOME" && pwd -P`

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
        "-Dorg.gradle.appname=gradlew" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"
