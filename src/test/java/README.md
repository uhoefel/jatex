Currently, there is a bug in junit/eclipse, when issueing 
```java
requires org.junit.jupiter.api;
```
in the `module-info.java` no tests can be run from within eclipse.
If you want to run the tests, add the following lines to the VM arguments in the Run Configuration:
```java
--add-exports org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED 
--add-exports org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED
```