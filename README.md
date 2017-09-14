Floodlight With Anomaly Detector
====================================

This is my thesis design. I develop a context-aware, event-based anomaly detector (CEAD). CEAD is embedded in Floodlight controller. If you are not familiar with Floodlight, I recommend you to visit its [github](https://github.com/floodlight/floodlight) and [Documents](https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/overview)

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

## Developing Floodlight in Eclipse
Build Floodlight as a maven project.
Please follow the **Eclipse IDE** paragraph in [this Installation Guide](https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/pages/1343544/Installation+Guide).

**Caution!!** please replace the General project with Maven project
~~ant eclipse~~
```diff
- ant eclipse
+ mvn eclipse:eclipse
```


### Usage
