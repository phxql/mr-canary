# Test backend

A small test backend.

Build it with

```
./mvnw clean package
```

Run it with

```
java -Dmicronaut.application.name=blue -Dmicronaut.server.port=2020 -Dservice.failure-percentage=50 -jar target/test-backend-*.jar
```

You can now execute requests against the service with

```
curl localhost:2020
```

In 50% of the time the request fails with a 500 (configured via `-Dservice.failure-percentage=50`).

The service also exposes Prometheus metrics on `localhost:2020/prometheus`. There are two metrics named
`test_backend_success` and `test_backend_failure`, both tagged with the application name (configured via 
`-Dmicronaut.application.name=blue`).

Example:

```
curl -s localhost:2020/prometheus | grep ^test_backend                                                                                                                                                     1 ↵
test_backend_success_total{name="blue",} 4.0
test_backend_failure_total{name="blue",} 5.0
```