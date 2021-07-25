# Gradle plugin for creating multi-release JAR libraries

A plugin for assisting in the creation of multi-release JAR libraries.

With the use of a multi-release JAR, a "base" version is utilized and then version-specific functionality is overridden or supplemented as necessary.

## Requirements

### Minimum
The minimum requirement for utilizing the plugin is the use of Gradle 7.0 or higher.
Earlier versions are not tested or verified.
Attempting to use the plugin with an earlier version will likely result in errors.

## Applying to a project

Groovy

    plugins {
      id 'net.kemuri9.gradle.mrjar' version '1.0.0'
    }

Kotlin

    plugins {
      id("net.kemuri9.gradle.mrjar") version "1.0.0"
    }

## Use

By applying the plugin, the `mrjar` extension is registered and can be utilized to control the behavior of the project.

### Adding additional versions of java for compilation

Additional versions of Java to compile are added with the `addVersion` functionality that takes the version number and the `Version` configuration block for the version specified. `addVersion` can be used multiple times for the same version number, if the configuration for a `Version` needs to be split across different blocks.

### Configuring the base version

The "base" version can additionally be configured via the extension through the use of the `baseVersion` function that takes the `Version` configuration block to apply to the base version.

### Examples

Examples can be found from the test case projects located in the [testprojects](testprojects) folder

### Javadoc

The latest javadoc can be viewed from the at [https://kemuri-9.github.io/gradle-plugin-mrjar/current/](https://kemuri-9.github.io/gradle-plugin-mrjar/current/)

## Language support

The plugin has extension points that define the behaviors for each JVM-based language.
There is built in support for Java and Groovy.

### Recognizing other JVM-based languages

Registration of new languages can be done via either of
* Registering the implementation through `net.kemuri9.gradle.mrjar.languages.LanguageSupport` ServiceLoader
* Manually through the `addLanguage(LanguageSupport support)` functionality on the registered `MRJarExtension` `mrjar`

### Replacing existing support

The existing support for a language can be replacing by manually registering the implementation through the `addLanguage(LanguageSupport support)` functionality on the registered `MRJarExtension` `mrjar`.
Attempting to use the ServiceLoader methodology may not work as desired as ServiceLoader has no priority concept.
