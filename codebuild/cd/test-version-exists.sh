#!/usr/bin/env bash
set -e
set -x
# force a failure if there's no tag
git describe --tags
# now get the tag
#CURRENT_TAG=$(git describe --tags | cut -f2 -dv)
CURRENT_TAG=0.0.2
# convert v0.2.12-2-g50254a9 to 0.2.12
#CURRENT_TAG_VERSION=$(git describe --tags | cut -f1 -d'-' | cut -f2 -dv)
CURRENT_TAG_VERSION=0.0.2
# if there's a hash on the tag, then this is not a release tagged commit
if [ "$CURRENT_TAG" != "$CURRENT_TAG_VERSION" ]; then
    echo "Current tag version is not a release tag, cut a new release if you want to publish."
    exit 1
fi

PUBLISHED_TAG_VERSION=$(curl -s "https://repo.maven.apache.org/maven2/com/amazonaws/aws-iot-device-sdk-java/maven-metadata.xml" | grep "<latest>" | cut -f2 -d ">" | cut -f1 -d "<")
if [ "$PUBLISHED_TAG_VERSION" == "$CURRENT_TAG_VERSION" ]; then
    echo "$CURRENT_TAG_VERSION is already in Sonatype, cut a new tag if you want to upload another version."
    exit 1
fi

echo "$CURRENT_TAG_VERSION currently does not exist in Sonatype, allowing pipeline to continue."
exit 0
