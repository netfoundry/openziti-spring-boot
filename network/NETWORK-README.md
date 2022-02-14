# Overview
This document describes the steps necessary to configure an OpenZiti (https://github.com/openziti) network for the code samples contained in this project.

# Start the OpenZiti network
## Get the OpenZiti quickstart shell extensions
OpenZiti provides a set of shell functions that bootstrap the OpenZiti client and network. As with any script, it is a good idea to download it first
and look it over before adding it to your shell.

Leave this terminal open, you'll need it to configure your network too.
 
```shell
wget -q https://raw.githubusercontent.com/openziti/ziti/release-next/quickstart/docker/image/ziti-cli-functions.sh
. ziti-cli-functions.sh
```

## Start the OpenZiti network
The shell script you just downloaded includes a few functions to initialize a network.  

To start the OpenZiti network overlay, run this in the same terminal:
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

# Configure the OpenZiti network
There is a file next to this readme called express-network-config.sh. 

If you just want to get going you can run that script. Continue through this file to learn more about the components
of an OpenZiti network.

## Create and enroll identities
An identity is anything that needs to connect to the OpenZiti network. Users, services, workflows, databases - they are all individual identities in the network.  
This example will use two identities - one for the service and one for the test. Let’s create these now:
```shell
ziti edge create identity device private-service -o private-service.jwt -a "services"
ziti edge create identity device client -o client.jwt -a "clients"
```
* **-o private-service.jwt:** The enrollment token generated when the identity is created will be written to private-service.jwt
* **-a “clients”/“services”:** OpenZiti supports grouping identities by attributes. This sample sets clients and services attributes to support load balancing and policies based on attributes.

Now that the identities are created, they must be enrolled before they can access the network.  The enrollment process exchanges the identity token created for the identity for a set of cryptographic keys used by OpenZiti to authenticate with the network.
```shell
ziti edge enroll -j private-service.jwt
ziti edge enroll -j client.jwt
```

## Create the service
A OpenZiti service definition allows an identity to bind to the network as a service host.  
The commands to create our sample service are:

```shell
ziti edge create config demo-service-config ziti-tunneler-client.v1 '{"hostname": "example.web","port": 8080}'
ziti edge create service demo-service --configs demo-service-config -a "demo-service"
```
* **demo-service-config:** Services can have configuration data that can be retrieved at runtime.  This `ziti-tunneler-client.v1` configuration tells clients of the OpenZiti network that the service is available at example.web:8080.
* **demo-service:** This is the name of the service in the OpenZiti network.

## Create identity access policies
Almost there! OpenZiti is a secure by default network. The example identities are all created in the network, but cannot do anything until permission is granted.  
OpenZiti handles permissions via a few different types of policies:
* **Network access policies:** Grant an identity access to one or more edge routers in the OpenZiti network. An identity cannot connect until it is granted access to one or more edge routers.
* **Service dial policies:** Grant an identity permission to open a connection to a service.
* **Service bind policies:** grant an identity permission to host a service.

First the network access:
```shell
ziti edge create edge-router-policy public-router-client-access --identity-roles "#clients" --edge-router-roles "#public"
ziti edge create edge-router-policy public-router-service-access --identity-roles "#services" --edge-router-roles "#public"
ziti edge create service-edge-router-policy public-router-access --service-roles "#demo-service" --edge-router-roles "#public"
```
* ***-roles:** These commands assign permissions based on attributes. These attributed were added when the network components were created.

The test identities can now connect and authenticate with the OpenZiti network, but they still cannot host or connect to any services.  
To grant service access:
```shell
ziti edge create service-policy service-bind-policy Bind --identity-roles "#services" --service-roles "#demo-service"
ziti edge create service-policy service-dial-policy Dial --identity-roles "#clients" --service-roles "#demo-service"
```

## Verify identity access
Zero Trust access policies are complex by necessity. OpenZiti provides a policy-advisor tool that we’ll use here to verify that the client and service are configured correctly.
```shell
ziti edge policy-advisor services demo-service
```

If everything worked right, then the last two lines of the command output look like this:
> OKAY : private-service (1) -> demo-service (1) Common Routers: (1/1) Dial: N Bind: Y  
> OKAY : client (1) -> demo-service (1) Common Routers: (1/1) Dial: Y Bind: N

# The End
At this point your network should be running and correctly configured for use with the Java examples.
