#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

set -e

# OS specific support.
cygwin=false
darwin=false
mingw=false
msys=false
case "$(uname)" in
    CYGWIN*) cygwin=true ;;
    Darwin*)  darwin=true ;;
    MINGW*)   mingw=true ;;
    MSYS*)    msys=true ;;
esac

# Determine the location of the script
if [ -n "$BASH_SOURCE" ]; then
    THIS_DIR=$(cd "$(dirname "$BASH_SOURCE")" && pwd)
elif [ -n "$ZSH_VERSION" ]; then
    THIS_DIR=$(cd "$(dirname "$0")" && pwd)
else
    THIS_DIR=$(cd "$(dirname "$0")" && pwd)
fi

MAVEN_WRAPPER_PROPERTIES="$THIS_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DISTRIBUTION_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/distributionUrl=//')
else
    echo "ERROR: $MAVEN_WRAPPER_PROPERTIES not found"
    exit 1
fi

MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MAVEN_WRAPPER_JAR="$THIS_DIR/.mvn/wrapper/maven-wrapper.jar"

# Download the wrapper jar if needed
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    WRAPPER_URL=$(grep "wrapperUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/wrapperUrl=//')
    if [ -n "$WRAPPER_URL" ]; then
        echo "Downloading $WRAPPER_URL"
        if command -v wget > /dev/null; then
            wget --no-verbose -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL" || rm -f "$MAVEN_WRAPPER_JAR"
        elif command -v curl > /dev/null; then
            curl -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL" -f -L || rm -f "$MAVEN_WRAPPER_JAR"
        fi
    fi
fi

# Determine JAVA_HOME
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif type java > /dev/null 2>&1; then
    JAVA_CMD="java"
else
    echo "ERROR: JAVA_HOME not set and java not found on PATH"
    exit 1
fi

# Determine Maven distribution directory name from URL
DIST_FILENAME=$(basename "$DISTRIBUTION_URL")
DIST_NAME="${DIST_FILENAME%-bin.zip}"
MAVEN_HOME="$MAVEN_USER_HOME/wrapper/dists/$DIST_NAME/$DIST_NAME"

if [ ! -d "$MAVEN_HOME" ]; then
    DIST_DIR="$MAVEN_USER_HOME/wrapper/dists/$DIST_NAME"
    mkdir -p "$DIST_DIR"
    DIST_ZIP="$DIST_DIR/$DIST_FILENAME"

    if [ ! -f "$DIST_ZIP" ]; then
        echo "Downloading $DISTRIBUTION_URL"
        if command -v wget > /dev/null; then
            wget --no-verbose -O "$DIST_ZIP" "$DISTRIBUTION_URL" || { rm -f "$DIST_ZIP"; exit 1; }
        elif command -v curl > /dev/null; then
            curl -o "$DIST_ZIP" "$DISTRIBUTION_URL" -f -L || { rm -f "$DIST_ZIP"; exit 1; }
        else
            echo "ERROR: wget or curl is required to download Maven"
            exit 1
        fi
    fi

    echo "Unzipping $DIST_ZIP"
    unzip -q "$DIST_ZIP" -d "$DIST_DIR"
fi

MAVEN_HOME="$MAVEN_USER_HOME/wrapper/dists/$DIST_NAME/$DIST_NAME"
exec "$MAVEN_HOME/bin/mvn" "$@"
