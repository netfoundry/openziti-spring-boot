# Hosting an OpenZiti service using Spring Boot

## What you will need to get started
* About 30 minutes
* A favorite text editor or IDE
* JDK 11 or later
* Access to a Linux environment with Bash 
  * A VM or WSL2 works for Windows users 

## Get the code
The code can be downloaded from [here](https://github.com/netfoundry/openziti-spring-boot/releases) or clone it using Git:
```shell
 git clone https://github.com/netfoundry/openziti-spring-boot
```

Like most Spring guides, you can start from scratch and complete each step, or you can bypass basic 
setup steps that are already familiar to you. Either way, you end up with working code.

# Create the test OpenZiti network
This example will use a very simple OpenZiti network.

<p align="center">
<img id="exampleNetworkImage" src="images/DemoNetwork.png" alt="Example OpenZiti Network" width="300"/>
</p>

It isn't important right now to understand all of components of the OpenZiti network. The important things you need to know are:
1. The controller manages the network. It is responsible for configuration, authentication, and authorization of components that connect to the OpenZiti network.
2. The router delivers traffic from the client to the server and back again.

Want to know more about OpenZiti? Head over to https://openziti.github.io/ziti/overview.html#overview-of-a-ziti-network.

Let's get into it and create the test network!

## Get the OpenZiti quickstart shell extensions
OpenZiti provides a set of shell functions that bootstrap the OpenZiti client and testing network. As with any script, it is a good idea to download it first
and look it over before adding it to your shell.

Leave this terminal open, you'll need it to configure your network too.
 
```shell
# Pull the shell extensions
wget -q https://raw.githubusercontent.com/openziti/ziti/release-next/quickstart/docker/image/ziti-cli-functions.sh

# Source the shell extensions
. ziti-cli-functions.sh

# Pull the latest Ziti CLI and put it on your shell's classpath
getLatestZiti yes
```

## Start the OpenZiti network
The shell script you just downloaded includes a few functions to initialize a network.  

To start the OpenZiti network overlay, run this in the terminal you used to download the client:
```shell
 expressInstall
 startZitiController
 waitForController
 startExpressEdgeRouter
```
* **expressInstall**: Creates cryptographic material and configuration files required to run an OpenZiti network.
* **startZitiController**: Starts the network controller.
* **startExpressEdgerouter**: Starts the edge router.

## Log into the new network
The OpenZiti network is now up and running. The next step is to log into the controller and 
establish an administrative session that we will use to configure the example services and identities.
`ziti-cli-functions` has a function to do this:
```shell
zitiLogin
```

## Configure the OpenZiti test network
We'll use a script to configure the OpenZiti network.

The code for the example contains a network directory.  
To configure the network, run this in the same terminal you used to start the OpenZiti network:
```shell
./express-network-config.sh
```

If the script errors with a lot of `ziti: command not found` then you can re-run this first to put 
ziti back in your terminal path:
```shell
getLatestZiti yes
```

The script will write out the two identity files (client.json and private-service.json) that you will need for the Java code.

This repository includes a file called [NETWORK-README.md](network/NETWORK-README.md) if you want to learn more about what the script is doing and why.

# Resetting the Ziti demo network
You're done with this article or things have gone really off the rails, and you want to start over.  

There are a couple of commands that need to be run to stop the Ziti network and clean up.
```
stopAllEdgeRouters
stopZitiController
unsetZitiEnv
rm -rf ~/.ziti/quickstart
```

# Host service using Spring Boot
There are three things that need to be done to host an OpenZiti service in a Spring Boot application:

1. Add the OpenZiti spring boot dependency.
2. Add two properties to the service to configure the service identity and service name.
3. Add an OpenZiti Tomcat customizer to the main application component scan.

The example code contains an initial/server project. Pull that up in your favorite editor and follow along.

## Add the OpenZiti spring boot dependency
The openziti spring boot dependency is hosted on maven central.  

Add the following to build.gradle:
```kotlin
implementation 'org.openziti:ziti-springboot:0.23.12'
```

If you prefer maven, add the following to pom.xml:
```xml
<dependency>
         <groupId>org.openziti</groupId>
         <artifactId>ziti-springboot</artifactId>
         <version>0.23.12</version>
</dependency>
```
## Add application properties
Open the application properties file: `src/main/resources/application.properties`

The Tomcat customizer provided by OpenZiti needs an identity and the name of the service that the identity will bind. If you followed along with the network setup above, then the values will be:
```
ziti.id = ../network/private-service.json
ziti.serviceName = demo-service
```

## Configure the OpenZiti Tomcat customizer
The Tomcat customizer replaces the standard socket protocol with an OpenZiti protocol that knows 
how Bind a service to accept connections over the Ziti network. To enable this adapter, open up the
main application class `com.example.restservice.RestServiceApplication`.

Replace
```
@SpringBootApplication
```

With
```
@SpringBootApplication (scanBasePackageClasses = {ZitiTomcatCustomizer.class, GreetingController.class})
```

## Run the application
That’s all you need to do!  The OpenZiti Java SDK will connect to the test network, authenticate, 
and bind your service so that OpenZiti overlay network clients can connect to it. 

To run the application, enter the following in a terminal window (in your project directory)
```shell
./gradlew bootRun
```

If you use maven, run the following in a terminal window in your project directory:
```shell
./mvnw spring-boot:run
```

# Test your new Spring Boot service
The Spring Boot service you have just created is now totally dark. It has no listening ports at all. 
Go ahead - run `netcat` and find out for yourself!

```shell
netstat -anp | grep 8080
```
You should find nothing `LISTENING`. Now, the only way to access it is via the OpenZiti network!
Let’s write a simple client to connect to it and check that everything is working correctly.

This section will use the OpenZiti Java SDK to connect to the OpenZiti network. The example source 
includes a project and a class that takes care of the boilerplate stuff for you.

If you want to skip building the client then you can skip ahead to [Run The Client](#run-the-client).

## Connect to OpenZiti
The Java SDK needs to be initialized with an OpenZiti identity. It is polite to destroy the context 
once the code is done, so we’ll wrap it up in a `try/catch` with a `finally` block.

```java
ZitiContext zitiContext = null;
try {
  zitiContext = Ziti.newContext(identityFile, "".toCharArray());

  if (null == zitiContext.getService(serviceName,10000)) {
    throw new IllegalArgumentException(String.format("Service %s is not available on the OpenZiti network",serviceName));
  }

} catch (Throwable t) {
  log.error("OpenZiti network test failed", t);
}
finally {
  if( null != zitiContext ) zitiContext.destroy();
}
```
* **Ziti.newContext:** Loads the OpenZiti identity and starts the connection process.
* **zitiContext.getService** It can take a little while to establish the connection with the OpenZiti 
network fabric. For long-running applications this is typically not a problem, but for this little
client we need to give the network some time to get everything ready.
* **zitiContext.destroy():** Disposes of the context and cleans up resources locally and on the 
OpenZiti network.

## Send a request to the service
The client has a connection to the test OpenZiti network. Now the client can ask OpenZiti to dial the service and send some data.

This client is for demonstration purposes only. You would never write a raw HTTP request like this 
in a real app. OpenZiti has a couple of examples using OKHttp and Netty if you want to work this 
code up using a real HTTP client. The examples can be found at 
https://github.com/openziti/ziti-sdk-jvm/tree/main/samples. 

```java
log.info("Dialing service");
ZitiConnection conn = zitiContext.dial(serviceName);
String request = "GET /greeting?name=MyName HTTP/1.1\n" +
"Accept: */*\n" +
"Host: example.web\n" +
"\n";
log.info("Sending request");
conn.write(request.getBytes(StandardCharsets.UTF_8));
```
* **ZitiConnection:** A socket connection over the OpenZiti network fabric that can be used to exchange data with a Ziti service.
* **zitiContext.dial:** Dialing a service opens a connection through the OpenZiti to the service.
* **request:** The connection is essentially a plain socket. The contents of the request string is a plain HTTP GET to the greeting endpoint in the Spring Boot app.
* **con.write:** Sends the request over the OpenZiti network.

## Read the service response
The service will respond to the request with a json greeting. Read the greeting and write it to the log.
```java
byte[] buff = new byte[1024];
int i;
log.info("Reading response");
while (0 < (i = conn.read(buff,0, buff.length))) {
  log.info("=== " + new String(buff, 0, i) );
}
```
* **con.read:** Read data sent back from the Spring Boot service via the OpenZiti connection.

## Run The Client
To run the client, run the following in a terminal window (in the client project):
```shell
./gradlew build run
```

If you use maven, run the following in a terminal window (in the client project):
```shell
./mvnw package exec:java
```
# Dig Deeper
* **OpenZiti documentation:** https://openziti.github.io/ziti/overview.html
* **OpenZiti Github project:** https://github.com/openziti 
* **Spring Boot Rest Sample:** https://spring.io/guides/gs/rest-service/
* **NetFoundry hosted OpenZiti NaaS offering:** https://netfoundry.io

**Spring and Spring Boot are trademarks of Pivotal Software, Inc. in the U.S. and other countries.*
