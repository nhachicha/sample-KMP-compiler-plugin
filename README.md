Sample Kotlin Compiler Plugin for Multiplatform Project
=======================================================

The goal of this project is to demonstrate how to build a sample Kotlin compiler plugin which modifies a Kotlin class property accessor by adding a prefix to the backing field.

The plugin register an IR transformation that select String properties to be modified to return the current value (backing field) prefixed with a message.

```Kotlin
class Person {
    var name: String? = "foo"
    var age: Int = 0
}
val p = Person()
p.name = "Nabil" // <--- The plugin adds a prefix 'Hello ' to the property name 
assertEquals("Hello Nabil", p.name)

```

Note: Currently this works with Kotlin `1.3.61` since looking up Symbols from other module is broken (since 1.3.7), this could be fixed in Kotlin 1.4 M3, see https://youtrack.jetbrains.com/issue/KT-37255#comment=27-4136756
 
For JVM target IR needs to be enabled.
```Gradle
kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                kotlinOptions.jvmTarget = "1.8"
                kotlinOptions.useIR = true
            }
        }
    }
...
}
```

Building
========

```
./gradlew assemble publishToMavenLocal
```

Usage 
=====

An example KMP project is available under [example](./example) project.

run JVM & macos tests 
```
./gradlew allTests
```