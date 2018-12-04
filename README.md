# Overview

bTools is a set of programs for identifying and tracking insects in image sequences with the help of a tiny matrix barcode, and for detecting mouth-to-mouth exchange of liquid food among honey bees. 

# Getting started

If you want executable binaries, download them from our [most recent release](https://github.com/gernat/btools/releases/) and then follow the instructions on our [website](http://www.beemonitoring.igb.illinois.edu/index.html#software).

If you want to experiment with the code, choose a branch and download or clone the repository. Import the `scr` folder in your favorite IDE and add the JAR packages in the `lib` folder to the Java classpath. You are ready to go.

## Main classes

`BCodeMaker.java` - draws a set of 2048 unique bCodes.  
`BCodeDetector.java` - detects bCodes in digital images.  
`Converter.java` - converts bCode detection results to human-readable plain text.  
`Indexer.java` - indexes bCode detection results.  
`TrophallaxisDetector.java` - detects liquid food exchange among bees.

# Documentation

Brief documentation is available on the [bTools website](http://www.beemonitoring.igb.illinois.edu/index.html#software).

# Versions

bTools uses [Semantic Versioning](http://www.semver.org). The [development branch](https://github.com/gernat/btools/tree/development) is the latest version of bTools, while the [master branch](https://github.com/gernat/btools/tree/master) contains the latest stable release. For information about each release, check out the [Changelog](https://github.com/gernat/btools/blob/development/CHANGELOG).

# Bug reports

If you find a bug, please submit it as an [Issue](https://github.com/gernat/btools/issues).

# Contributing

Please consider contributing your improvements and additions by sending a [pull request](https://github.com/gernat/btools/pulls).

# License

This software is distributed under the [GNU Affero General Public License v3 license](https://github.com/gernat/btools/blob/development/LICENSE) and uses third party software that is distributed under its [own terms](https://github.com/gernat/btools/blob/development/LICENSE-3RD-PARTY). 

# Contact

If you have questions about bTools, please contact [Tim](mailto:gernat@illinois.edu).