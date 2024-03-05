# Overview

bTools is a set of programs for identifying and tracking insects in images or videos with the help of a tiny matrix barcode, called bCode. It also contains detectors for behaviors such as movement and, for honey bees, flight activity, the mouth-to-mouth exchange of liquid (trophallaxis), and worker egg-laying. 

# Getting started

If you just want to use our software, please download our [most recent release](https://github.com/gernat/btools/releases/) and then follow the instructions in the [wiki](https://github.com/gernat/btools/wiki).

If you want to experiment with the code, please choose a branch and download or clone the repository. Next, import the `scr` folder in your favorite IDE, add the JAR packages in the `lib` folder to the Java classpath, and you are ready to go.

## Main classes

`BCodeMaker.java` - draws a set of 2048 unique bCodes.  
`BCodeDetector.java` - detects bCodes in digital images.  
`Converter.java` - converts raw bCode detection results to a more useful format.  
`Indexer.java` - indexes raw bCode detection results.  
`TrophallaxisDetector.java` - detects liquid food exchange among honey bees.  
`MovementDetector.java` - detects bCode movement.  
`FlightActivityDetector.java` - detects hive exits and returns.  
`EggLayingDetector.java` - detects worker egg-laying.

# Documentation

Brief documentation and a tutorial are available in the [wiki](https://github.com/gernat/btools/wiki).

# Versions

bTools uses [Semantic Versioning](http://www.semver.org). The [development branch](https://github.com/gernat/btools/tree/development) is the latest version of bTools, while the [master branch](https://github.com/gernat/btools/tree/master) contains the latest stable release. For information about each release, please check out the [Changelog](https://github.com/gernat/btools/blob/master/CHANGELOG).

# Bug reports

If you find a bug, please submit it as an [Issue](https://github.com/gernat/btools/issues).

# Contributing

Please consider contributing your improvements and additions by sending a [pull request](https://github.com/gernat/btools/pulls).

# License

This software is distributed under the [GNU Affero General Public License v3 license](https://github.com/gernat/btools/blob/master/LICENSE) and uses third party software that is distributed under its own terms. 

# Contact

If you have questions about bTools, please contact [Tim](mailto:gernat@illinois.edu).