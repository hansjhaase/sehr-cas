package org.ifeth.sehr.p1507291734.web.listener;

import com.google.common.eventbus.EventBus;
import org.ifeth.sehr.p1507291734.lib.Constants;
import java.util.logging.Level;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

//import org.firebirdsql.ds.FBConnectionPoolDataSource;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/**
 * WEB, Mobile Session Lifecycle Listener.
 *
 */
@WebListener
public class SessionListener implements HttpSessionListener {

  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web");
  //@EJB(beanName = "JMedDatasource")
  //private JMedDatasourceBean jmedDatasource;
  //private FBConnectionPoolDataSource fbds;
  private InitialContext ic;
  private int debug = 2; //errors and warnings by default

  @PersistenceUnit(unitName = "HibJTAIntraSEC", name = "jpa/p1507291734/HibJTAIntraSEC")
  private EntityManagerFactory emf;

  /**
   * Default constructor.
   */
  public SessionListener() {
    Log.fine(SessionListener.class.getName() + "::Constructor");
  }

  /**
   * @param arg0
   * @see HttpSessionListener#sessionCreated(HttpSessionEvent)
   */
  @Override
  public void sessionCreated(HttpSessionEvent arg0) {
    Log.fine(SessionListener.class.getName() + ":sessionCreated()");

    Properties p;
    HttpSession sess = arg0.getSession();
    try {
      ic = new InitialContext();
      p = (Properties) ic.lookup(Constants.ICPropName);
      debug = Integer.parseInt(p.getProperty("debug"));
      String zoneID = p.getProperty("zoneid", "0000000");
      if (!zoneID.equals("0000000")) {
        //we do not use native FBDS in this project!
        /*
         String icDSName = "sehr/" + p.getProperty("zoneid")
         + "/DataSources/IntraSEC";
         fbds = (FBConnectionPoolDataSource) ic.lookup(icDSName);
         if (debug >= 8) {
         //System.out.println(jmedDatasource.dumpMetadata());

         System.out.println("Session..........: " + sess.getId());
         System.out.println("Database.........: " + fbds == null ? "n/a" : fbds.getDatabaseName());
         //System.out.println("Ping Statement...: " + fbds.getPingStatement());
         System.out.println("SQL Dialect......: " + fbds == null ? "n/a" : fbds.getSqlDialect());
         }
         */
      }
    } catch (NamingException e) {
      //a faulty initial context is severe
      Log.log(Level.SEVERE, SessionListener.class.getName() + ":sessionCreated():{0}", e.toString());
      //log more errors from now
      debug = 8;
    }

    ServletContext ctx = sess.getServletContext();
    Map currentSessions = (Map) ctx.getAttribute("CurrentSessions");
    currentSessions.put(sess.getId(), sess);
    //(re)store persistence factory context
    ctx.setAttribute("EntityManagerFactory", emf);
    //
    if (ctx.getAttribute("EventBus") == null) {
      EventBus eventBus = new EventBus();
      ctx.setAttribute("EventBus", eventBus);
    }
  }

  /**
   * @param arg0
   * @see HttpSessionListener#sessionDestroyed(HttpSessionEvent)
   */
  @Override
  public void sessionDestroyed(HttpSessionEvent arg0) {
    Log.fine(SessionListener.class.getName() + ":sessionDestroyed()");

    HttpSession sess = arg0.getSession();
    ServletContext ctx = sess.getServletContext();
    Map currentSessions = (Map) ctx.getAttribute("CurrentSessions");
    Properties p = (Properties) ctx.getAttribute("Properties");

    //close/finish session related stuff
    SEHRMessagingListener chatHandler = (SEHRMessagingListener) sess.getAttribute("ChatHandler");
    if (chatHandler != null) {
      //TODO I18N
      chatHandler.sendMsg("Session control of " + p.getProperty("zoneid") + " closed the messaging.");
      chatHandler.closeSession();
    }
    //clean up
    if (currentSessions.containsKey(sess.getId())) {
      currentSessions.remove(sess.getId());
      Log.log(Level.INFO, "Session #{0} removed from stack.", sess.getId());
    }

  }

  /**
   * Verify session.
   *
   * <p>
   * Return 'true' only if there is a valid SessionID as well as a 'pid'
   * (String). By <bold>convention of SEHR</bold> there should be always a
   * second code/deviceid etc. named 'pid'.
   * </p>
   *
   * @param ctx
   * @param sessionID
   * @return
   */
  public static boolean isSession(ServletContext ctx, String sessionID) {
    Map currentSessions = (Map) ctx.getAttribute("CurrentSessions");
    if (currentSessions != null && sessionID != null) {
      if (currentSessions.containsKey(sessionID)) {
        Log.log(Level.FINE, SessionListener.class.getName() + ":isSession():Session #{0} found in map.", sessionID);
        HttpSession sess = (HttpSession) currentSessions.get(sessionID);
        if (sess.getAttribute("pid") != null) {
          //TODO test 'pid' against the SEHR database 
          //(registered device, public key of a person etc)
          return true;
        } else {
          Log.log(Level.WARNING, "Session found but no valid 'pid' (is null)");
        }
      } else {
        Log.log(Level.WARNING, "Session #{0} not found in map.", sessionID);
      }
    }
    return false;
  }
}
