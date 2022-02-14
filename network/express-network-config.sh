hostsFileError() {
	echo Error: The ziti edge controller or edge router hostname could not be resolved.  
	echo These commands can be used to configure the hosts if you are using the docker-compose environment:
	echo 'echo "127.0.0.1       ziti-edge-controller" | sudo tee -a /etc/hosts'
	echo 'echo "127.0.0.1       ziti-edge-router" | sudo tee -a /etc/hosts'
	exit 1;
}

#echo Checking hosts file setup
#getent hosts ziti-edge-controller > /dev/null || { hostsFileError; }
#getent hosts ziti-edge-router > /dev/null || { hostsFileError; }

if !ziti edge list edge-routers > /dev/null 2>&1; then
	echo "Error: Log into OpenZiti before running this script" 
	exit 1
fi

echo Creating identities
ziti edge create identity device private-service -o private-service.jwt -a "services"
ziti edge create identity device client -o client.jwt -a "clients"
ziti edge create identity device zdew-client -o zdew-client.jwt -a "clients"

echo Enrolling identities
ziti edge enroll -j private-service.jwt
ziti edge enroll -j client.jwt

echo Creating demo-service
ziti edge create config demo-service-config ziti-tunneler-client.v1 '{"hostname": "example.web","port": 8080}'
ziti edge create service demo-service --configs demo-service-config -a "demo-service"

echo Creating identity network access policies
ziti edge create edge-router-policy public-router-client-access --identity-roles "#clients" --edge-router-roles "#public"
ziti edge create edge-router-policy public-router-service-access --identity-roles "#services" --edge-router-roles "#public"
ziti edge create service-edge-router-policy public-router-access --service-roles "#demo-service" --edge-router-roles "#public"

echo Creating identity service policies
ziti edge create service-policy service-bind-policy Bind --identity-roles "#services" --service-roles "#demo-service"
ziti edge create service-policy service-dial-policy Dial --identity-roles "#clients" --service-roles "#demo-service"

echo Network configuration complete
