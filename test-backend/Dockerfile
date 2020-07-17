FROM openjdk:14-alpine
COPY target/test-backend-*.jar test-backend.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "test-backend.jar"]