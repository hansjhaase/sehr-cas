/*
 * (C)2013 IFETH
 * Copied from project 'sehr-saf'
 */
package org.ifeth.sehr.p1507291734.web.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

/**
 * Native JNDI query and dump of SEHR attributes at the JVM.
 *
 * @author hansjhaase
 */
public class MonitoringUtils {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static final String JEJBCTX = "ejb/sehr";
  private static final String JGLOBALCTX = "java:global/sehr";

  public static void dumpJNDI(String dir) {
    System.out.println("--- DEBUGGING:JNDI context ---");
    if (dir == null || dir.isEmpty()) {
      dir = JGLOBALCTX;
    }
    InitialContext ic;
    try {
      ic = new InitialContext();
      System.out.println("--- context list " + dir + " ---");
      NamingEnumeration list = ic.list(dir);
      while (list.hasMore()) {
        NameClassPair ncp = (NameClassPair) list.next();
        System.out.println(ncp.getName());
      }
      System.out.println("--- end list ---");
    } catch (NamingException ne) {
      Log.log(Level.INFO, "MonitoringUtils::dumpJNDI():{0}", ne.toString());
    }
  }

  public static void dumpModuleJNDI(String modulepik) {
    System.out.println("--- DEBUGGING:JNDI context of SEHR module " + modulepik + " ---");
    InitialContext ic;
    try {
      ic = new InitialContext();
      //System.out.println("--- context list " + dir + " ---");
      NamingEnumeration list = ic.list("sehr/" + modulepik);
      while (list.hasMore()) {
        NameClassPair ncp = (NameClassPair) list.next();
        System.out.println(ncp.getName());
      }
      System.out.println("--- end list ---");
    } catch (NamingException ne) {
      Log.log(Level.INFO, "MonitoringUtils::dumpJNDI():{0}", ne.toString());
    }
  }

  public static void dumpEJBJNDI() {
    InitialContext ic;
    try {
      ic = new InitialContext();
      System.out.println("--- EJB context list '" + JEJBCTX + "' ---");
      NamingEnumeration listejb = ic.list(JEJBCTX);
      while (listejb.hasMore()) {
        NameClassPair ncp = (NameClassPair) listejb.next();
        System.out.println(ncp);
      }
      System.out.println("--- end list ---");

    } catch (NamingException ne) {
      Log.log(Level.INFO, "MonitoringUtils::dumpJNDI():{0}", ne.toString());
    }
  }

  public static Map<String, String> getRequestHeadersInMap(HttpServletRequest request, boolean dump) {

    Map<String, String> result = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    if (dump) {
      sb.append("------ http hader ------\n");
    }
    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String key = (String) headerNames.nextElement();
      String value = request.getHeader(key);
      result.put(key, value);
      if (dump) {
        sb.append(key).append(":").append(value).append("\n");
      }
    }
    if (dump) {
      sb.append("------ end of http header ------");
      System.out.print(sb.toString());
    }
    return result;
  }
}
