# Tested Spring Snippets

## Run Diktat or Spotless

:heavy_exclamation_mark: If you are using **Java 16+**, you need to add `--add-opens java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED` flag to the JVM.

Either by
```
export MAVEN_OPTS="--add-opens java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED"
```
or by Mavens `.mvn/jvm.config` file since 3.3.1+.

See also diktat [README.md](https://github.com/saveourtool/diktat#run-with-maven-using-diktat-maven-plugin).


