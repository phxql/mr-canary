# Mr. Canary

## What is this?

A TCP reverse proxy with canaries built in.

## What is a canary?

It's the name of a release process. Instead of releasing the new version of your software to all of your users at once,
you deploy the new version next to the old one and let some users test the new version of the software. If the new
version is working, you'll route more users to it. If enough users are on the new version and it still works, you switch
all traffic to it. Then you can shut down the old version of the software.

This process limits the exposure of buggy new versions to a small percentage of your users.

## Why did you built that?

I looked for reverse proxies with canaries built in and found none. There are [solutions](https://docs.flagger.app/) for kubernetes, but not everyone
runs their software in kubernetes. I thought it would be cool to have a standalone application for that.

## What services are supported?

All that use TCP. I guess you'll want to use it with HTTP services, which works totally fine. Just beware some caveats:

Load balancing is done when the connection is opened. As long as this connection is open, it will always reach the same
backend. Depending on your use case you may want to disable HTTP keep-alive to make sure that every HTTP request is
load balanced. Otherwise your client may be routed to the old version of the software and will keep using this version -
even if the canary succeeds and you'll want to use the new version of the software for all users.

## How does this work exactly?

You create a config file like this:

```toml
id = "test-canary-1"

port = 8080
blue_address = "127.0.0.1:2020"
green_address = "127.0.0.1:3030"
max_failures = 3
analysis_interval = "PT10S"

[prometheus]
blue_uri = "http://127.0.0.1:9090"
green_uri = "http://127.0.0.1:9090"
# Both queries calculate the percentage of successful requests
blue_query = "rate(test_backend_success_total{name=\"blue\"}[10s]) / rate(test_backend_total{name=\"blue\"}[10s]) * 100"
green_query = "rate(test_backend_success_total{name=\"green\"}[10s]) / rate(test_backend_total{name=\"green\"}[10s]) * 100"
# Success rate must be >= 95
min = 95.0

[weight]
start = 10
end = 100
increase = 10
```

If you start Mr. Canary, it will listen on port 8080 for traffic. It will route all the traffic on that port to the blue backend by default
(`blue_address = 127.0.0.1:2020`) and return the result. 

As soon as you start the canary process by using the admin API (more on this later),
it will start shifting traffic. It will start with 10% (`weight.start`) traffic to the green backend (`green_address = 127.0.0.1:3030`), and 90% of
the traffic to the blue backend.
 
After 10 seconds (`analysis_interval`) it will check if the green version is working by
executing the prometheus query configured in `prometheus.green_query` against the configured prometheus (`prometheus.green_uri`).
If the result of that query is at least 95.0 (`prometheus.min`), the check succeeds. 

Mr. Canary will then route 20% of traffic (`10% + weight.increase`) to the green backend, and 80% to the blue backend. This processes is repeated until the canary weight reaches 100 (`weight.end`).
When that happens, one final check is done against the prometheus. If that succeeds, the new version of the software is considered good.
Mr. Canary will then route 100% of the traffic to the green backend.  

Should a prometheus check fail, the failure counter increases. If that counter reaches 3 (`max_failures`), the new version is considered
faulty and Mr. Canary falls back to the old version. It will then route 100% of the traffic to the blue backend. You can
now fix the new version, deploy it and start the process anew.

## What is the admin API?

You configure the port of the admin API in the main config file `config.toml`:

```toml
canaries_directory = "config"
admin_address = "127.0.0.1:8888"
```

You can use the admin API for starting new canaries, aborting running ones or get the status of your canaries.
For more details see [the documentation of the admin API](doc/admin-api.md). 

## How many backends are supported?

Two per canary, but an unlimited number of canaries (okay, not unlimited. It's limited by your available port range).

Every canary has exactly two backends, the blue and the green one. Mr. Canary alternates between them. If you
start the canary, traffic will shift from blue to green. After the canary succeeds, the primary backend for that canary
is the green one. If you now start the canary process again, traffic will shift from green to blue. After that canary
succeeds, the primary backend is again the blue one. And so on.

## Can i use something other than prometheus for canary checking?

No, not at the moment.

## Building

Run `./mvnw clean package` and check the `target` folder.

## License

Licensed under [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html).
