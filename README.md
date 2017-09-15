Floodlight With Anomaly Detector
====================================

This is my thesis design. I implement VeriFlow, which is a anomaly Detector. VeriFLow is embedded in Floodlight controller. If you are not familiar with Floodlight, I recommend you to visit these reference: [github](https://github.com/floodlight/floodlight),  [Documents](https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/overview)

## Getting Started

### Prerequisites
* JDK 8
* Maven to build
* Python development package
* Eclipse IDE

### Installing

To download Java 8, please refer to [these instructions](http://www.webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html).

To download the latest version of Eclipse, please refer to [these instructions](https://eclipse.org/downloads/)

To download python development package
```
sudo apt-get install build-essential ant maven python-dev
```

To download and install Floodlight
```
$ git clone <the master repository URL>
$ cd floodlight
$ git submodule init
$ git submodule update
$ sudo mkdir /var/lib/floodlight
$ sudo chmod 777 /var/lib/floodlight
$ git checkout -b VeriFlow origin/VeriFlow
```

### Usage
Use Maven to compile, then execute floodlight with Java.
```
$ mvn clean install -DskipTests
$ java -Dlogback.configurationFile=./logback.xml -jar ./target/floodlight.jar
```

The above only execute the Floodlight controller, but VeriFlow is not active by default.
If you want to use VeriFlow, you should execute a helper program (follow [this guide](pass)).

## Eclipse IDE

### Developing Floodlight in Eclipse
Build Floodlight as a maven project.
```diff
$ mvn eclipse:eclipse
```
Import maven project in eclipse.
 * Open eclipse and create a new workspace
 * File -> Import -> Maven -> Existing Maven Projects. Then click "Next".
 * From "Select root directory" click "Browse". Select the parent directory where you placed floodlight earlier.
 * Check the box for "Floodlight". No other Projects should be present and none should be selected.
 * Click Finish.

### Running Floodlight in Eclipse
Please follow the "Running Floodlignt in Eclipse" in [this  guide](https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/pages/1343544/Installation+Guide).

## Description
This paragraph describe the modification of this project on Floodlight

### Difference
* src/main/java/veriflow (new package)
* src/main/java/record (new package)
* src/main/java/net/floodlightcontroller/experimentApp (new module)
* src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule (Floodlight config file)
* src/main/resources/floodlightdefault.properties (Floodlight config file)
* src/main/java/net/core/internal/Controller.java (Floodlight main program)
* src/main/java/net/core/internal/OFSwitch.java (Floodlight main program)
* src/main/java/net/routing/ForwardingBase.java (Floodlignt built-in module)
