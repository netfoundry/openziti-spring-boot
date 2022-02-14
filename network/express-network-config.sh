if !ziti edge list edge-routers > /dev/null 2>&1; then
	echo "Error: Log into OpenZiti before running this script" 
	exit 1
fi

rm private-service.jwt client.jwt private-service.json client.json 2> /dev/null

echo Creating identities
ziti edge create identity device private-service -o private-service.jwt -a "services"
ziti edge create identity device client -o client.jwt -a "clients"

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
