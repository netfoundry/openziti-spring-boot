package com.example.restservice;

import org.openziti.Ziti;
import org.openziti.ZitiConnection;
import org.openziti.ZitiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SimpleClient {
  private static final Logger log = LoggerFactory.getLogger( SimpleClient.class );

  private static void usageAndExit() {
    System.out.println("Usage: SimpleClient <-i identityFile> <-s serviceName>");
    System.exit(1);
  }

  public static void main(String[] args) {
    String identityFile = "../../network/client.json";
    String serviceName = "demo-service";

    for(int i = 0; i < args.length; i++) {
      if("-i".equals(args[i])) {
        if( i < args.length-1 ) {
          identityFile = args[++i];
        } else {
          usageAndExit();
        }
      }
      if("-s".equals(args[i])) {
        if( i < args.length-1 ) {
          serviceName = args[++i];
        } else {
          usageAndExit();
        }
      }
    }

    hitZitiService(identityFile,serviceName);
  }

  private static void hitZitiService(String identityFile, String serviceName) {
	  // Add Code Here
  }
}
