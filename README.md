[![License: MIT](https://img.shields.io/github/license/rozjin/sea)](https://github.com/rozjin/sea/blob/main/LICENCE.md)

# 🌊 Sea ⚓ 

* Sea is a Web framework that works with one thread per connection.
* Sea uses Project Loom's virtual threads to cheaply and quickly spawn request handlers.
* The framework is extremely lightweight, and is designed to run behind a reverse proxy, e.g nginx.
* Configuration is done by default via annotations, and request handlers are regular Java Beans.

## Documentation
* WIP

## TODO
* Basic Documentation, Javadoc too.
* Configurable error handling.
* Dependency Injection.

## Contributors
Contributions are welcome.
To compile Sea, you must use Java 19+
1. Clone: ``git clone git@github.com:rozjin/sea.git``
2. Build: ``./gradlew clean build``

## Running
Sea relies on preview features of the JVM, to enable them,
pass ``--enable-preview`` as an argument to the JVM. 

### IntelliJ
For IntelliJ, this can be done under Settings -> Java Compiler -> Additional command line parameters: ``--enable-preview``

### Gradle
For gradle, add to your build.gradle: 
```groovy
compileJava {
    options.compilerArgs += ["--enable-preview"]
}
```

