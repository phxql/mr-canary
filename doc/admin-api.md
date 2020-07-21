# Mr. Canary Admin API

## Health check

```
curl -s -i 127.0.0.1:8888/status

HTTP/1.1 200 OK
content-type: application/json
connection: keep-alive
content-length: 20

{
  "status": "ok"
}
```

## Get canary status

```
curl -s -i 127.0.0.1:8888/                                                                                                                                                                               130 ↵

HTTP/1.1 200 OK
content-type: application/json
connection: keep-alive
content-length: 248

{
  "canaries": [
    {
      "id": "test-canary-1",
      "blueBackend": "/127.0.0.1:2020",
      "greenBackend": "/127.0.0.1:3030",
      "status": "FAILED_BLUE",
      "currentBackend": "BLUE",
      "weight": 10,
      "failures": 0
    }
  ]
}
```

## Start a canary

```
curl -s -i --request POST "127.0.0.1:8888/start?canary=test-canary-1"

HTTP/1.1 200 OK
content-type: application/json
connection: keep-alive
content-length: 79

{
  "canaryId": "test-canary-1",
  "status": "SHIFT_TO_GREEN",
  "weight": 10
}
```

## Abort a running canary

```
curl -s -i --request POST "127.0.0.1:8888/abort?canary=test-canary-1"

HTTP/1.1 200 OK
content-type: application/json
connection: keep-alive
content-length: 88

{
  "canaryId": "test-canary-1",
  "status": "FAILED_BLUE",
  "currentBackend": "BLUE"
}
```