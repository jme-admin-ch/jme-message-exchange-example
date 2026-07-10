# JME Message Exchange Service Example

This example shows how to use the jEAP Message Exchange Service locally. It contains the following modules:

* **jme-message-exchange-auth-scs**: An instance of the OAuth mock server to authenticate against the jEAP Message Exchange Service.
* **jme-message-exchange-client-service**: A mock for application using the Message Exchange service. This app will call the rest endpoints of the Message Exchange service to simulate a real application.
* **jme-message-exchange-client-service**: An instance of the Message Exchange service.

This example project show how to use the [jeap-message-exchange-service](https://github.com/jeap-admin-ch/jeap-message-exchange-service) library.
The library contains all the necessary components to set up a Message Exchange service instance.

## Changes

This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Prerequisites

To use this project, ensure you have the following installed:

1. **Java Development Kit (JDK)**: Version 25.
2. **Docker**: For running the required infrastructure.

**Note:** Use the provided maven wrapper to build and run the project.

## Getting started

### Infrastructure

Before the examples can be started the infrastructure has to be started using docker

```shell
docker-compose -f docker/docker-compose.yml up
```

### Build

The project itself can be built with a simple

```shell
./mvnw install
```

### Start

Then the individual subprojects can be started using

```shell
./mvnw --projects jme-message-exchange-auth-scs spring-boot:run -Dspring-boot.run.profiles=local
./mvnw --projects jme-message-exchange-service spring-boot:run -Dspring-boot.run.profiles=local
./mvnw --projects jme-message-exchange-client-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Testing the Message Exchange Service API

To test the API of the Message Exchange service, use the Swagger UI at

* Partner API http://localhost:8080/message-exchange/swagger-ui/index.html?urls.primaryName=MessageExchange-Service-Partner-API-V4
* Internal API http://localhost:8080/message-exchange/swagger-ui/index.html?urls.primaryName=MessageExchange-Service-Internal-API-V3

### Testing the Message Exchange Service Client API

To test the API via the client service, use the Swagger UI at
http://localhost:8082/message-exchange-client/swagger-ui/index.html?urls.primaryName=Message+Exchange+Service+Client+API

The token obtained from the OAuth mock server will contain a business partner ID (bpId) with the value '123'.

### Testing the API with the malware scan enabled

If the malware scan is enabled, the following steps can be used to test the API:

- Send a new message to the Message Exchange service using the Partner API. The message will be stored in the S3 bucket and a message will be sent to the SQS queue for malware scanning.
- Simulate a malware scan result by sending a message to the SQS queue for malware scan results. The message should contain the object key of the message in the S3 bucket, the bucket name, and the scan result (e.g., "NO_THREATS_FOUND" or "THREAT_FOUND").
- Use the following command to simulate a malware scan result:
```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  aws --endpoint-url=http://localhost:4566 --region eu-central-1 \
  sqs send-message \
  --queue-url http://localhost:4566/000000000000/malware-scan-results \
  --message-body '{
    "objectKey": "cc7d5097-4d3f-4fff-af91-fd2680191544",
    "bucketName": "bazg-jme-messageexchange-partner-obs-dev",
    "scanResult": "NO_THREATS_FOUND"
  }'
```
- Retrieve the message from the Message Exchange service using the Internal API. The message should now have the scan result updated and can be retrieved successfully.


## Note

This repository is part of the open source distribution of JME. See [github.com/jme-admin-ch/jme](https://github.com/jme-admin-ch/jme)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
