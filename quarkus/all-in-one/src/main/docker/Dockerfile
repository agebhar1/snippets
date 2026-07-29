FROM registry.access.redhat.com/ubi9/openjdk-21:1.23 AS openjdk

FROM scratch

# COPY follows symlink
# /lib -> /usr/lib
# /lib64 -> /usr/lib64
ADD src/main/docker/symlinks.tar /

COPY --from=openjdk /etc/java /etc/java
COPY --from=openjdk /usr/lib/jvm/java-21-openjdk-21.0.9.0.10-1.el9.x86_64 /usr/lib/jvm/java-21-openjdk-21.0.9.0.10-1.el9.x86_64
# podman compose exec service cat /proc/1/maps | grep '/' | awk '{ print $6 }' | sort -u
COPY --from=openjdk /usr/lib64/ld-linux-x86-64.so.2 /usr/lib64/ld-linux-x86-64.so.2
COPY --from=openjdk /usr/lib64/libc.so.6 /usr/lib64/libc.so.6
COPY --from=openjdk /usr/lib64/libdl.so.2 /usr/lib64/libdl.so.2
COPY --from=openjdk /usr/lib64/libm.so.6 /usr/lib64/libm.so.6
COPY --from=openjdk /usr/lib64/libnspr4.so /usr/lib64/libnspr4.so
COPY --from=openjdk /usr/lib64/libnss3.so /usr/lib64/libnss3.so
COPY --from=openjdk /usr/lib64/libnssutil3.so /usr/lib64/libnssutil3.so
COPY --from=openjdk /usr/lib64/libplc4.so /usr/lib64/libplc4.so
COPY --from=openjdk /usr/lib64/libplds4.so /usr/lib64/libplds4.so
COPY --from=openjdk /usr/lib64/libpthread.so.0 /usr/lib64/libpthread.so.0
COPY --from=openjdk /usr/lib64/librt.so.1 /usr/lib64/librt.so.1
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_ADDRESS /usr/lib/locale/C.utf8/LC_ADDRESS
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_COLLATE /usr/lib/locale/C.utf8/LC_COLLATE
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_CTYPE /usr/lib/locale/C.utf8/LC_CTYPE
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_IDENTIFICATION /usr/lib/locale/C.utf8/LC_IDENTIFICATION
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_MEASUREMENT /usr/lib/locale/C.utf8/LC_MEASUREMENT
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_MESSAGES/SYS_LC_MESSAGES /usr/lib/locale/C.utf8/LC_MESSAGES/SYS_LC_MESSAGES
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_MONETARY /usr/lib/locale/C.utf8/LC_MONETARY
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_NAME /usr/lib/locale/C.utf8/LC_NAME
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_NUMERIC /usr/lib/locale/C.utf8/LC_NUMERIC
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_PAPER /usr/lib/locale/C.utf8/LC_PAPER
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_TELEPHONE /usr/lib/locale/C.utf8/LC_TELEPHONE
COPY --from=openjdk /usr/lib/locale/C.utf8/LC_TIME /usr/lib/locale/C.utf8/LC_TIME
COPY --from=openjdk /usr/share/javazi-1.8 /usr/share/javazi-1.8

ENV LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/
COPY --chown=185 target/dependency/opentelemetry-javaagent.jar /deployments/

EXPOSE 8080
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

WORKDIR /deployments

ENTRYPOINT [ "/usr/lib/jvm/java-21-openjdk-21.0.9.0.10-1.el9.x86_64/bin/java", "-Dhttp.nonProxyHosts=agent", "-cp", ".", "-jar", "/deployments/quarkus-run.jar" ]
