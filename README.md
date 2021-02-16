# Assignment-2-CI

## Server Functionality

* Create commit statuses on Github (success, failure, or error. Includes a description). The status is determined by success of compilation and tests.
* Email notification of build status of pushed commits.
* Build history is saved on the server. A top-level html document links to each saved build where the build log can be found and a Github link is provided 
* Configurable server address, port, path for webhooks, and destination to save build logs (default app/\<dir\>).

## Info
The CI server is built using **Java 11** and [**Gradle 6.8.1**](https://gradle.org/releases/) for building and testing the application. [**ngrok**](https://ngrok.com/) is used to host the server.
Furthermore, some java libraries are included to implement the specific functions: the java library: [json-simple-1.1.1](https://code.google.com/archive/p/json-simple/downloads) is used for processing JSON data; the java libraries: [javax-mail-1.6.2](https://mvnrepository.com/artifact/com.sun.mail/javax.mail/1.6.2) as well as [javax-mail-api-1.6.2](https://mvnrepository.com/artifact/javax.mail/javax.mail-api/1.6.2) are used to send email to notify the branch owner.

## Setting up the server

For this project we use [**ngrok**](https://ngrok.com/) with our own machines to host the CI server. The URL provided by ngrok is publically accessible on the internet.
Run:
```
ngrok http 127.0.0.1:8080
```
To make localhost accessible on the internet where the web server runs on port 8080.
![ngrok](./res/images/ngrok.png)

## Configuring webhooks

Github webhooks is what allows us to be notified when a github event, such as a push, occurs. For this server we only require the push event. Github webhooks can be configured in the settings of a repository. The POST requests generated by these hooks are expected to be sent to the path: **\"/github-webhooks\"** of the main ngrok URL.
![webhooks](./res/images/webhooks.png)

## Github Authentication

In order for Github to accept GET and POST requests sent by us, we need to verify ourselves. This is done by generating a [personal access token](https://github.com/settings/tokens) with minimum repo and user access.
The token should be saved in an environment variable called "GITHUB\_TOKEN" which is retrieved by the server through Java's
```
System.getenv("GITHUB_TOKEN")
```

## Building and Running the server

Example: to run on Socket Address 127.0.0.1:8080, serving Github webhooks to path "/github-webhooks" and logs are saved in "Assignment-2-CI/app/buildLogs". Also, the absolute path of the project directory needs to be supplied.
```
./gradlew run --args="--address 127.0.0.1 --port 8080 --target_dir buildLogs --webhooks /github-webhooks --project_dir /home/dh/masters/swe/fork/Assignment-2-CI" --no-daemon
```
This starts the server. Any triggered events, which are included in our webhook configuration, should now successfully send POST requests to our server on the configured path.

To test:
```
gradle test
```
in project directory, or:
```
./gradlew test
```
to run tests with wrapper. This is used by the server to verify that tests run successfully.

To build:
```
gradle build
```
in project directory, or:
```
./gradlew build
```
to build with wrapper. This is used by the server to verify that project version compiles and builds.

## Hosting build logs

The build history can be found in the root directory of the generated ngrok url.


## Test commits
1
2
3
4
5
6
7
