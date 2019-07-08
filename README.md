"Failfast" repositories - a gradle plugin
=========================================

Gradle plugin to ease the usage of a proxy maven repository with fallback to regular maven repositories when the proxy is inaccessible.

Usage example
-------------

```groovy
apply plugin: "com.sagiantebi.failfast.repos"

failfastRepositories {
    //if the following repository is reachable, it will be the only one in use.
    failFast(ofRepository("https://repo.maven.apache.org/maven2/"))
    //these are used as the repositories when the failFast ones are not available.
    fallback(Arrays.asList(ofRepository("https://maven.google.com"), ofRepository("https://jcenter.bintray.com"))) 
}

```