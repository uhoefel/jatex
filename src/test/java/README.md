As we put the tests in the same package as the classes, we need to issue
```java
requires org.junit.jupiter.api;
```
in the `module-info.java`, as otherwise no tests can be run from within eclipse.
If you want to run the tests, add the following lines to the VM arguments in the Run Configuration:
```java
--add-exports org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED 
--add-exports org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED
```