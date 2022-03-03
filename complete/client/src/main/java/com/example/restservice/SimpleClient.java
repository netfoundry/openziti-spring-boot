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

    for (int i = 0; i < args.length; i++) {
      if ("-i".equals(args[i])) {
        if (i < args.length-1) {
          identityFile = args[++i];
        } else {
          usageAndExit();
        }
      }
      if ("-s".equals(args[i])) {
        if (i < args.length-1) {
          serviceName = args[++i];
        } else {
          usageAndExit();
        }
      }
    }

    hitZitiService(identityFile, serviceName);
  }
  private static void hitZitiService(String identityFile, String serviceName) {

    ZitiContext zitiContext = null;
    try {
      zitiContext = Ziti.newContext(identityFile, "".toCharArray());

      if (null == zitiContext.getService(serviceName,10000)) {
        throw new IllegalArgumentException(String.format("Service %s is not available on the OpenZiti network",serviceName));
      }

      log.info("Dialing service");
      ZitiConnection conn = zitiContext.dial(serviceName);

      String request = "GET /greeting?name=MyName HTTP/1.1\n" +
        "Accept: */*\n" +
        "Host: example.web\n" +
        "\n";

      log.info("Sending request");
      conn.write(request.getBytes(StandardCharsets.UTF_8));

      byte[] buff = new byte[1024];
      int i;

      log.info("Reading response");
      while (0 < (i = conn.read(buff,0, buff.length))) {
        log.info("=== " + new String(buff, 0, i) );
      }
    } catch (Throwable t) {
      log.error("OpenZiti network test failed", t);
    }
    finally {
      if( null != zitiContext ) zitiContext.destroy();
    }
  }
}
