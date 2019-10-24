# AWS IoT Device SDK for Java
The **AWS IoT Device SDK for Java** enables Java developers to access the AWS 
IoT Platform through [MQTT or MQTT over the WebSocket protocol][aws-iot-protocol]. 
The SDK is built with [AWS IoT device shadow support][aws-iot-thing], providing 
access to thing shadows (sometimes referred to as device shadows) using shadow methods, including GET, UPDATE, and DELETE.
It also supports a simplified shadow access model, which allows developers to 
exchange data with their shadows by just using getter and setter methods without
having to serialize or deserialize any JSON documents.

To get started, use the Maven repository or download the [latest JAR file][latest-jar].

* [Overview](#overview)
* [Install the SDK](#install-the-sdk)
* [Use the SDK](#use-the-sdk)
* [Sample Applications](#sample-applications)
* [API Documentation](#api-documentation)
* [License](#license)
* [Support](#support)

## Overview
This document provides instructions for installing and configuring the AWS 
IoT device SDK for Java. It also includes some examples that demonstrate the use of different 
APIs.

### MQTT Connection Types
The SDK is built on top of the [Paho MQTT Java client library][paho-mqtt-java-download].
Developers can choose from two types of connections to connect to
the AWS IoT service:

 * MQTT (over TLS 1.2) with X.509 certificate-based mutual authentication
 * MQTT over WebSocket with AWS Signature Version 4 authentication

For MQTT over TLS (port 8883), a valid certificate and private key are required 
for authentication. For MQTT over WebSocket (port 443), a valid AWS Identity and Access Management (IAM)
access key ID and secret access key pair is required for authentication.

### Thing Shadows
A thing shadow represents the cloud counterpart of a physical device or thing. 
Although a device is not always online, its thing shadow is. A thing shadow
stores data in and out of the device in a JSON based document. When the device is offline, its shadow document is still
accessible to the application. When the device comes back online, 
the thing shadow publishes the delta to the device (which the device didn't
see while it was offline). 

The SDK implements the protocol for applications to retrieve, update, and 
delete shadow documents mentioned [here][aws-iot-thing]. 
When you use the simplified access model, you have the option to enable strict document versioning. To reduce the overhead of subscribing to shadow topics
for each method requested, the SDK automatically subscribes to all of the method 
topics when a connection is established.

#### Simplified Shadow Access Model
Unlike the shadow methods, which operate on JSON documents, the simplified 
shadow access model allows developers to access their shadows with getter and 
setter methods. 

To use this feature, you must extend the device class ```AWSIotDevice```,
use the annotation ```AWSIotDeviceProperty``` to mark class member variables to be
managed by the SDK, and provide getter and setter methods for accessing these 
variables. The getter methods will be used by the SDK to report to the shadow
periodically. The setter methods will be invoked whenever there is a change 
to the desired state of the shadow document. For more information, see [Use the SDK](#use-the-sdk)
later in this document.

## Install the SDK

### Minimum Requirements
To use the SDK, you will need Java 1.7+. 

### Install the SDK Using Maven
The recommended way to use the AWS IoT Device SDK for Java in your project is 
to consume it from Maven. Simply add the following dependency to the POM file
of your Maven project.

```xml
<dependencies>
  <dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-iot-device-sdk-java</artifactId>
    <version>1.3.3</version>
  </dependency>
</dependencies>
```

The sample applications included with the SDK can also be installed using the following dependency definition.

```xml
<dependencies>
  <dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-iot-device-sdk-java-samples</artifactId>
    <version>1.3.3</version>
  </dependency>
</dependencies>
```

### Install the SDK Using the Latest JAR
The latest JAR files can be downloaded [here][latest-jar]. You can simply extract 
and copy the JAR files to your project's library directory, and then update your IDE to
include them to your library build path. 

You will also need to add two libraries the SDK depends on:
 * Jackson 2.x, including [Jackson-core] [jackson-core] and [Jackson-databind] [jackson-databind] 
 * Paho MQTT client for Java 1.1.x. [download instructions][paho-mqtt-java-download]

### Build the SDK from the GitHub Source
You can build both the SDK and its sample applications from the source 
hosted at GitHub. 

```sh
$ git clone https://github.com/aws/aws-iot-device-sdk-java.git
$ cd aws-iot-device-sdk-java
$ mvn clean install -Dgpg.skip=true
```

## Use the SDK
The following sections provide some basic examples of using the SDK to access the 
AWS IoT service over MQTT. For more information about each API, see the [API documentation][api-docs].

### Initialize the Client
To access the AWS IoT service, you must initialize ```AWSIotMqttClient```. The 
way in which you initialize the client depends on the connection
type (MQTT or MQTT over WebSocket) you choose. In both cases,
a valid client endpoint and client ID are required for setting up the connection.  

* Initialize the Client with MQTT (over TLS 1.2):
For this MQTT connection type (port 8883), the AWS IoT service requires TLS 
mutual authentication, so a valid client certificate (X.509)
and RSA keys are required. You can use the 
[AWS IoT console][aws-iot-console] or the AWS command line tools to generate certificates and keys. For the SDK,
only a certificate file and private key file are required.

```java
String clientEndpoint = "<prefix>.iot.<region>.amazonaws.com";       // replace <prefix> and <region> with your own
String clientId = "<unique client id>";                              // replace with your own client ID. Use unique client IDs for concurrent connections.
String certificateFile = "<certificate file>";                       // X.509 based certificate file
String privateKeyFile = "<private key file>";                        // PKCS#1 or PKCS#8 PEM encoded private key file

// SampleUtil.java and its dependency PrivateKeyReader.java can be copied from the sample source code.
// Alternatively, you could load key store directly from a file - see the example included in this README.
KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);

// optional parameters can be set before connect()
client.connect();
```

* Initialize the Client with MQTT Over WebSocket:
For this MQTT connection type (port 443), you will need valid IAM credentials 
to initialize the client. This includes an AWS access key ID and secret 
access key. There are a number of ways to get IAM credentials (for example, by creating 
permanent IAM users or by requesting temporary credentials through the Amazon Cognito
service). For more information, see the developer guides for these services. 

As a best practice for application security, do not embed 
credentials directly in the source code.

```java
String clientEndpoint = "<prefix>.iot.<region>.amazonaws.com";       // replace <prefix> and <region> with your own
String clientId = "<unique client id>";                              // replace with your own client ID. Use unique client IDs for concurrent connections.

// AWS IAM credentials could be retrieved from AWS Cognito, STS, or other secure sources
AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken);

// optional parameters can be set before connect()
client.connect();
```   

### Publish and Subscribe
After the client is initialized and connected, you can publish messages and subscribe
to topics.

To publish a message using a blocking API:

```java
String topic = "my/own/topic";
String payload = "any payload";

client.publish(topic, AWSIotQos.QOS0, payload);
```

To publish a message using a non-blocking API:

```java
public class MyMessage extends AWSIotMessage {
    public MyMessage(String topic, AWSIotQos qos, String payload) {
        super(topic, qos, payload);
    }

    @Override
    public void onSuccess() {
        // called when message publishing succeeded
    }

    @Override
    public void onFailure() {
        // called when message publishing failed
    }

    @Override
    public void onTimeout() {
        // called when message publishing timed out
    }
}

String topic = "my/own/topic";
AWSIotQos qos = AWSIotQos.QOS0;
String payload = "any payload";
long timeout = 3000;                    // milliseconds

MyMessage message = new MyMessage(topic, qos, payload);
client.publish(message, timeout);
```

To subscribe to a topic:

```java
public class MyTopic extends AWSIotTopic {
    public MyTopic(String topic, AWSIotQos qos) {
        super(topic, qos);
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        // called when a message is received
    }
}

String topicName = "my/own/topic";
AWSIotQos qos = AWSIotQos.QOS0;

MyTopic topic = new MyTopic(topicName, qos);
client.subscribe(topic);
```

### Shadow Methods
To access a shadow using a blocking API:

```java
String thingName = "<thing name>";                    // replace with your AWS IoT Thing name

AWSIotDevice device = new AWSIotDevice(thingName);

client.attach(device);
client.connect();

// Delete existing shadow document
device.delete();

// Update shadow document
State state = "{\"state\":{\"reported\":{\"sensor\":3.0}}}";
device.update(state);

// Get the entire shadow document
String state = device.get();
```

To access a shadow using a non-blocking API:

```java
public class MyShadowMessage extends AWSIotMessage {
    public MyShadowMessage() {
        super(null, null);
    }

    @Override
    public void onSuccess() {
        // called when the shadow method succeeded
        // state (JSON document) received is available in the payload field
    }

    @Override
    public void onFailure() {
        // called when the shadow method failed
    }

    @Override
    public void onTimeout() {
        // called when the shadow method timed out
    }
}

String thingName = "<thing name>";      // replace with your AWS IoT Thing name

AWSIotDevice device = new AWSIotDevice(thingName);

client.attach(device);
client.connect();

MyShadowMessage message = new MyShadowMessage();
long timeout = 3000;                    // milliseconds
device.get(message, timeout);
```

### Simplified Shadow Access Model
To use the simplified shadow access model, you need to extend the device class 
```AWSIotDevice```, and then use the annotation class ```AWSIotDeviceProperty```
to mark the device attributes and provide getter and setter methods for them. 
The following very simple example has one attribute, ```someValue```, defined.
The code will report the attribute to the shadow, identified by ***thingName*** 
every 5 seconds, in the ***reported*** section of the shadow document. The SDK
will call the setter method ```setSomeValue()``` whenever there's 
a change to the ***desired*** section of the shadow document.

```java
public class MyDevice extends AWSIotDevice {
    public MyDevice(String thingName) {
        super(thingName);
    }

    @AWSIotDeviceProperty
    private String someValue;

    public String getSomeValue() {
        // read from the physical device
    }

    public void setSomeValue(String newValue) {
        // write to the physical device
    }
}

MyDevice device = new MyDevice(thingName);

long reportInterval = 5000;            // milliseconds. Default interval is 3000.
device.setReportInterval(reportInterval);

client.attach(device);
client.connect();
```

### DiscoveryInfoProvider

This is the client class for device discovery process with AWS IoT Greengrass. You can initialize and configure the client like this:

```java
import com.amazonaws.services.iot.client.greengrass.DiscoveryInfoProvider;

String clientEndpoint = "<prefix>.iot.<region>.amazonaws.com";       // replace <prefix> and <region> with your own
String thingName = "<thing name>";                                   // replace with your AWS IoT Thing name
String certificateFile = "<certificate file>";                       // X.509 based certificate file
String privateKeyFile = "<private key file>";                        // PKCS#1 or PKCS#8 PEM encoded private key file

// SampleUtil.java and its dependency PrivateKeyReader.java can be copied from the sample source code.
// Alternatively, you could load key store directly from a file - see the example included in this README.
KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
DiscoveryInfoProvider discoveryInfoProvider = new DiscoveryInfoProvider(clientEndpoint, pair.keyStore, pair.keyPassword);
```

To perform the discovery process for a Greengrass Aware Device (GGAD) that belongs to a deployed group, your code should look like this:

```java
DiscoveryInfo discoveryInfo = discoveryInfoProvider.discover(thingName);

// I know nothing about the group/core I want to connect to. I want to iterate through all cores and find out.
List<CoreConnectivityInfo> cores = discoveryInfo.getAllCores();
List<Certificate> trustedCAs = discoveryInfo.getAllCas();

// Connecting logic ...
ConnectivityInfo connectivityInfo = cores.get(0).getConnectivity().get(0);
String coreEndpoint = connectivityInfo.getHostAddress() + ":" + connectivityInfo.getPortNumber();

AWSIotMqttClient client = new AWSIotMqttClient(coreEndpoint, thingName, pair.keyStore, pair.keyPassword, trustedCAs);
client.connect();
```
For more information about discovery information access at group/core/connectivity info set level, 
please refer to the API documentation for `com.amazonaws.services.iot.client.greengrass.*`, 
[Greengrass Discovery documentation](http://docs.aws.amazon.com/greengrass/latest/developerguide/gg-discover-api.html) or 
[Greengrass overall documentation](http://docs.aws.amazon.com/greengrass/latest/developerguide/what-is-gg.html).

### Other Topics 
#### Enable Logging
The SDK uses ```java.util.logging``` for logging. To change
the logging behavior (for example, to change the logging level or logging destination), you can
specify a property file using the JVM property
```java.util.logging.config.file```. It can be provided through JVM arguments like so: 

```sh
-Djava.util.logging.config.file="logging.properties" 
```

To change the console logging level, the property file ***logging.properties*** 
should contain the following lines: 

```
# Override of console logging level 
java.util.logging.ConsoleHandler.level=INFO 
```

#### Load KeyStore from File to Initialize the Client
You can load a KeyStore object directly from JKS-based keystore files.
You will first need to import X.509 certificate and the private key into the keystore 
file like so:

```sh
$ openssl pkcs12 -export -in <certificate-file> -inkey <private-key-file> -out p12.keystore -name alias
(type in the export password)

$ keytool -importkeystore -srckeystore p12.keystore -srcstoretype PKCS12 -srcstorepass <export-password> -alias alias -deststorepass <keystore-password> -destkeypass <key-password> -destkeystore my.keystore 
```

After the keystore file ***my.keystore*** is created, you can use it to 
initialize the client like so:

```java
String keyStoreFile = "<my.keystore>";                               // replace with your own key store file
String keyStorePassword = "<keystore-password>";                     // replace with your own key store password
String keyPassword = "<key-password>"                                // replace with your own key password

KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());

String clientEndpoint = "<prefix>.iot.<region>.amazonaws.com";       // replace <prefix> and <region> with your own
String clientId = "<unique client id>";                              // replace with your own client ID. Use unique client IDs for concurrent connections.

AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, keyStore, keyPassword);
```

#### Use ECC-Based Certificates
You can use Elliptic Curve Cryptography (ECC)-based certificates to initialize the client. To create an ECC key and certificate, see [this blog post][aws-iot-ecc-blog]. After you have created and registered the key and certificate, use the following command to convert
the ECC key file to PKCK#8 format.

```sh
$ openssl pkcs8 -topk8 -nocrypt -in ecckey.key -out ecckey-pk8.key
(type in the key password)
```

You can then use the instruction described in [this section](#initialize-the-client) to initialize the client
with just this one change.

```java
// SampleUtil.java and its dependency PrivateKeyReader.java can be copied from the sample source code.
// Alternatively, you could load key store directly from a file - see the example included in this README.
KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, "EC");
```

## Sample Applications
There are three samples applications included with the SDK. The easiest way to 
run these samples is through Maven, which will take care of getting the 
dependencies.

* Publish/Subscribe sample:
This sample consists of two publishers publishing one message per second to a 
topic. One subscriber subscribing to the same topic receives and prints the 
messages. 

* Shadow sample:
This sample consists of a simple demo of the simplified shadow access 
model. The device contains two attributes: window state and room temperature. 
Window state can be modified (therefore, controlled) remotely through 
***desired*** state. To demonstrate this control function, you can use the AWS 
IoT console to modify the desired window state, and then see its change from the
sample output.

* Shadow echo sample: 
This sample consists of a simple demo that uses Shadow methods to send a shadow 
update and then retrieve it back every second.

### Arguments for the Sample Applications
To run the samples, you will also need to provide the following arguments 
through the command line:

* clientEndpoint: client endpoint, in the form of ```<prefix>.iot.<region>.amazonaws.com```
* clientId: client ID
* thingName: AWS IoT thing name (not required for the Publish/Subscribe sample)

You will also need to provide either set of the following arguments for authentication.
For an MQTT connection, provide these arguments: 

* certificateFile: X.509 based certificate file (For Just-in-time registration, this
is the concatenated file from both the device certificate and CA certificate. For more information
about Just-in-Time Registration, please see [this blog][Just-in-Time-Registration].
* privateKeyFile: private key file
* keyAlgorithm: (optional) RSA or EC. If not specified, RSA is used.

For an MQTT over WebSocket connection, provide these arguments: 

* awsAccessKeyId: IAM access key ID
* awsSecretAccessKey: IAM secret access key
* sessionToken: (optional) if temporary credentials are used

### Run the Sample Applications
You can use the following commands to execute the sample applications (assuming 
TLS mutual authentication is used).

* To run the Publish/Subscribe sample, use the following command:
```sh
$ mvn exec:java -pl aws-iot-device-sdk-java-samples -Dexec.mainClass="com.amazonaws.services.iot.client.sample.pubSub.PublishSubscribeSample" -Dexec.args="-clientEndpoint <prefix>.iot.<region>.amazonaws.com -clientId <unique client id> -certificateFile <certificate file> -privateKeyFile <private key file>"
```

* To run the Shadow sample, use the following command:
```sh
$ mvn exec:java -pl aws-iot-device-sdk-java-samples -Dexec.mainClass="com.amazonaws.services.iot.client.sample.shadow.ShadowSample" -Dexec.args="-clientEndpoint <prefix>.iot.<region>.amazonaws.com -clientId <unique client id> -thingName <thing name> -certificateFile <certificate file> -privateKeyFile <private key file>"
```

* To run the Shadow echo sample, use the following command: 
```sh
$ mvn exec:java -pl aws-iot-device-sdk-java-samples -Dexec.mainClass="com.amazonaws.services.iot.client.sample.shadowEcho.ShadowEchoSample" -Dexec.args="-clientEndpoint <prefix>.iot.<region>.amazonaws.com -clientId <unique client id> -thingName <thing name> -certificateFile <certificate file> -privateKeyFile <private key file>"
```

### Sample Source Code
You can get the sample source code either from the GitHub repository as described 
[here](#build-the-sdk-from-the-github-source) or from [the latest SDK binary][latest-jar].
They both provide you with Maven project files that you can use to build and run the samples
from the command line or import them into an IDE, such as Eclipse.

The sample source code included with the latest SDK binary is shipped with a modified Maven
project file (pom.xml) that allows you to build the sample source indepedently, without the
need to reference the parent POM file as with the GitHub source tree. 

## API Documentation
You'll find the API documentation for the SDK [here][api-docs].

## License
This SDK is distributed under the [Apache License, Version 2.0][apache-license-2]. For more information, see
LICENSE.txt and NOTICE.txt.

## Support
If you have technical questions about the AWS IoT Device SDK, use the [AWS IoT Forum][aws-iot-forum].
For any other questions about AWS IoT, contact [AWS Support][aws-support].

[aws-iot-protocol]: http://docs.aws.amazon.com/iot/latest/developerguide/protocols.html
[aws-iot-thing]: http://docs.aws.amazon.com/iot/latest/developerguide/iot-thing-shadows.html
[aws-iot-forum]: https://forums.aws.amazon.com/forum.jspa?forumID=210
[aws-iot-console]: http://aws.amazon.com/iot/
[latest-jar]: https://s3.amazonaws.com/aws-iot-device-sdk-java/aws-iot-device-sdk-java-LATEST.zip
[jackson-core]: https://github.com/FasterXML/jackson-core
[jackson-databind]: https://github.com/FasterXML/jackson-databind
[paho-mqtt-java-download]: https://eclipse.org/paho/clients/java/
[api-docs]: http://aws-iot-device-sdk-java-docs.s3-website-us-east-1.amazonaws.com/
[aws-iot-ecc-blog]: https://aws.amazon.com/blogs/iot/elliptic-curve-cryptography-and-forward-secrecy-support-in-aws-iot-3/
[aws-support]: https://aws.amazon.com/contact-us
[apache-license-2]: http://www.apache.org/licenses/LICENSE-2.0
[Just-in-Time-Registration]: https://aws.amazon.com/blogs/iot/just-in-time-registration-of-device-certificates-on-aws-iot/
