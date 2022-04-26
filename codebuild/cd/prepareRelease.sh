#!/bin/bash

# Fail commands with unbound variables
set -o nounset
# Exit when a commmand fails
set -e

echo "Compiling source code (version $PKG_VERSION)..."
mvn -s $HOME/.m2/settings.xml clean package -Dmaven.test.skip=true

echo "Preparing release artifacts..."
mkdir aws-iot-device-sdk-java-$PKG_VERSION
pushd aws-iot-device-sdk-java-$PKG_VERSION
cp ../LICENSE.txt .
cp ../NOTICE.txt .
jq --slurp --raw-input '{"text": "\(.)", "mode": "markdown"}' < ../README.md | curl --data @- https://api.github.com/markdown > README.html
mkdir lib
cp ../aws-iot-device-sdk-java/target/*.jar lib/
cp ../aws-iot-device-sdk-java-samples/target/*.jar lib/
cp -Lr ../aws-iot-device-sdk-java/target/apidocs documentation
popd


echo "Cleaning up source code for release..."
mvn -s $HOME/.m2/settings.xml clean


echo "Copying clean source code for release..."
pushd aws-iot-device-sdk-java-$PKG_VERSION
cp -Lr ../aws-iot-device-sdk-java-samples samples
pushd samples
mv samples-pom.xml pom.xml
popd
popd


# echo "Compressing release artifacts..."
# mkdir -p $BASE_DIR/var/s3_tarballs
# zip -r $BASE_DIR/var/s3_tarballs/aws-iot-device-sdk-java-$PKG_VERSION.zip aws-iot-device-sdk-java-$PKG_VERSION
# cp $BASE_DIR/var/s3_tarballs/aws-iot-device-sdk-java-$PKG_VERSION.zip $BASE_DIR/var/s3_tarballs/aws-iot-device-sdk-java-LATEST.zip
# echo "print(\"$PKG_VERSION\")" > $BASE_DIR/var/aws-iot-device-sdk-java-Version.py


#echo "Removing temporary files..."
#rm -rf $BASE_DIR/var/tmp/sdk-release

mvn -s $BASE_DIR/var/m2.settings clean deploy -P publishing
mvn -s $BASE_DIR/var/m2.settings nexus-staging:release
