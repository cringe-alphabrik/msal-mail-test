# msal-mail-test

Simple example using msal4j to access MS365 mailboxes

[![Java CI with Maven](https://github.com/cringe-alphabrik/msal-mail-test/actions/workflows/maven.yml/badge.svg)](https://github.com/cringe-alphabrik/msal-mail-test/actions/workflows/maven.yml)

## How to run

Build a runnable JAR file

````bash
./mvnw package
````

Setup your environment with the following variables:

* `account`=your-email@example.com
* `clientId`=...
* `clientSecret`=...
* `tenant`=...
* `authority`=https://login.microsoftonline.com
* `baseUrl`=https://graph.microsoft.com/v1.0

Run the application

````bash
java -jar target/msal-1.0.0-SNAPSHOT-jar-with-dependencies.jar
````
