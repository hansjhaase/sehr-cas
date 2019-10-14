package org.ifeth.sehr.p1507291734.web.listener;

import org.ifeth.sehr.p1507291734.lib.Constants;
import org.ifeth.sehr.p1507291734.lib.LoggerUtility;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.ifeth.sehr.core.lib.SEHRConfiguration;
import org.ifeth.sehr.core.objects.SEHRConfigurationObject;
import org.ifeth.sehr.core.spec.SEHRConstants;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
//import org.ifeth.sehr.p1507291734.web.rest.SEHRConfiguration;

/**
 * Application Lifecycle Listener.
 *
 * Starts after deployment.
 *
 */
@WebListener
public class ContextListener implements ServletContextListener,
        ServletContextAttributeListener {

  private static final Logger logger = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
  private static final String MODULECFG = "/WEB-INF/" + Constants.MODULE_NAME + ".properties";
  private static final String MONITORCFG = "/WEB-INF/ZoneAdv.map";

  @PersistenceUnit(unitName = "HibJTAIntraSEC", name = "jpa/p1507291734/HibJTAIntraSEC")
  private EntityManagerFactory emf;

  //private ServletContext ctx;
  //private InitialContext ic;
  private Properties p;
  private int debug = 0;
  //used for receiving center p2p (queue) messages and topic related stuff
  private ActiveMQConnectionFactory connectionFactory;
  private ActiveMQConnection amqConnection;
  private MessagingManager jmsMan;

  //@EJB(beanName = "ScheduledTimerBean")
  //private ScheduledTimerBean cronEjb;
  //@EJB(beanName = "MessageHandler")
  //private MessageHandler ejbMessageHandler;
  private final Map<String, HttpSession> currentSessions = new HashMap<>();
  private final Map<String, SAFQueueListener> safListeners = new HashMap<>();
  private org.primefaces.push.EventBus pfEventBus;

  /**
   * Default constructor.
   */
  public ContextListener() {
    //System.out.println("ContextListener:constructor()");
  }

  /**
   * App ready for service.
   *
   * @param evt0
   * @see ServletContextListener#contextInitialized(ServletContextEvent)
   */
  @Override
  public void contextInitialized(ServletContextEvent evt0) {
    logger.log(Level.FINE, "{0}:contextInitialized():Logger:{1} {2}; evt0 object:{3}", new Object[]{ContextListener.class.getName(), logger.getName(), logger.getLevel(), evt0.getSource()});

    ServletContext ctx = evt0.getServletContext();
    ctx.setAttribute("OutOfService", false);
    SEHRConfigurationObject sco = null;
    try {
      InputStream is = ctx.getResourceAsStream(MODULECFG);
      p = new Properties();
      p.load(is); //properties by this module, not yet parsed...      
      is.close();
    } catch (IOException e2) {
      logger.log(Level.SEVERE, ContextListener.class.getName() + ":contextInitialized():Error reading settings from {0}:{1}",
              new Object[]{MODULECFG, e2.toString()});
      ctx.setAttribute("OutOfService", true);
      return;
    }
    SEHRConfiguration sehrcfg = new SEHRConfiguration();
    //TODO use configure to clean up and interprete %{...} fields
    //if (sehrcfg.configure(p).loadSEHRCfg(p)) {
    if (!sehrcfg.loadSEHRCfg(ctx.getResourceAsStream(MODULECFG))) {
      logger.log(Level.SEVERE, ContextListener.class.getName() + ":contextInitialized():Error getting SEHRConfigurationObject from {0}",
              new Object[]{MODULECFG});
      ctx.setAttribute("OutOfService", true);
      return;
    }
    sco = sehrcfg.getSEHRConfigurationObject();
    //since 9/2015 'activemq...' is deprecated by this module
    sco.setAMQBrokerURL(p.getProperty("sehrxnetzoneurl"));
    sco.setAMQUser(p.getProperty("sehrxnetzoneuser", "defaultUser"));
    sco.setAMQUserPw(p.getProperty("sehrxnetzonepw", "defaultPassword"));
    String zid = (String) p.getProperty("zoneid", "0000000");
    // --- clean up values in cfg file from comments!!
    Enumeration e = p.propertyNames();
    String key;
    String value;
    Pattern pattern = Pattern.compile("([^\\s]*)");

    while (e.hasMoreElements()) {
      key = (String) e.nextElement();
      value = p.getProperty(key);
      // cut of comments... in values in lines like 'key=value #blah blah'
      Matcher matcher = pattern.matcher(value);
      if (matcher.find()) {
        value = matcher.group(1);
        // this.setSEHRCfgValue(key, value);
        // System.out.println("key: " + key + "/value: " + value);

      }
      //interprete %{} values
      if (value.contains("%{zoneid}")) {
        //String subdomain = p.getProperty("subdomain", "");
        Map<String, String> values = new HashMap<>();
        values.put("zoneid", zid);
        StrSubstitutor sub = new StrSubstitutor(values, "%{", "}");
        value = sub.replace(value);

      }
      p.setProperty(key, value);
    }
    sco.setSubdomain(p.getProperty("subdomain", ""));
    //store persistence factory context
    ctx.setAttribute("EntityManagerFactory", emf);

    InitialContext ic;
    try {
      ic = new InitialContext();
      //%{...} values have been substituted at this point!
      ic.rebind(Constants.ICPropName, p);
      ic.rebind(SEHRConstants.icSCO, sco);
    } catch (NamingException ne) {
    }
    debug = Integer.parseInt(p.getProperty("debug"));
    //assigns level only to this named logger
    LoggerUtility.assignLevelByDebug(debug, logger);
    logger.log(Level.FINE, "{0}:contextInitialized():Logger assigned by ''debug={1}'':{2} {3}; evt0 object:{4}", new Object[]{ContextListener.class.getName(), debug, logger.getName(), logger.getLevel(), evt0.getSource()});

    //App Server ignores our logging.properties; in future we'll use Log4J...
    //System.out.println("ContextListener:contextInitialized():before... "+logger.getLevel());
    //System.out.println("ContextListener:contextInitialized():debug="+debug);
    if (debug >= 8) {
      //workaround to use our logging.properties with logging levels for 
      //all our classes and/or working packages but all messages will be logged
      //to our handles... not useful while developping (:
//      try {
//        FileInputStream fis = new FileInputStream(webInfFile(JMS4S_LOGCFG));
//        LoggerUtility.assignConfig(fis);
//      } catch (MalformedURLException ex) {
//        Logger.getLogger(ContextListener.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (FileNotFoundException ex) {
//        Logger.getLogger(ContextListener.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (IOException ex) {
//        Logger.getLogger(ContextListener.class.getName()).log(Level.SEVERE, null, ex);
//      }
      //so we're setting levels to other loggers step by step...
      Logger logEJB = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");
      LoggerUtility.assignLevelByDebug(debug, logEJB);
      Logger logWEB = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
      LoggerUtility.assignLevelByDebug(debug, logWEB);

      Enumeration<String> eLoggers = LogManager.getLogManager().getLoggerNames();
      System.out.println(ContextListener.class.getName() + ":contextInitialized():Current Loggers:");
      while (eLoggers.hasMoreElements()) {
        String s = (String) eLoggers.nextElement();
        if (s.startsWith("org.ifeth")) {
          System.out.println("- " + s);
        }
      }
    }
    //System.out.println("ContextListener:contextInitialized():after... " + logger.getLevel());

    /*
     * LogManager manager = LogManager.getLogManager(); try{
     * manager.readConfiguration(ctx.getResourceAsStream("/WEB-INF/logging.properties")); logger =
     * Logger.getLogger("de.mdigmbh.jmed.mobile.web"); } catch
     * (SecurityException e1) { System.out.println("Logger init error: " +
     * e1.toString()); } catch (FileNotFoundException e1) {
     * System.out.println("Logger init error: " + e1.toString()); } catch
     * (IOException e1) { System.out.println("Logger init error: " +
     * e1.toString()); } logger.setLevel(Level.FINEST);
     * System.out.println("Logger set to: " + logger.getName() + " " +
     * logger.getLevel());
     *
     */
    ctx.setAttribute("Properties", p);

    ctx.setAttribute("CurrentSessions", currentSessions);
    ctx.setAttribute("SAFQueueListener", safListeners);
//    if (cronEjb == null) {
//      logger.warning("ContextListener:contextInitialized():EJB module not initialized.");
//      ctx.setAttribute("OutOfService", true);
//      //do not proceed!
//      return;
//    }

//    if (!initDataSource(ctx)) {
//      logger.warning(ContextListener.class.getName()+":contextInitialized():Service not available. See logs for details.");
//      ctx.setAttribute("OutOfService", true);
//      return;
//    }
    //get pre-configured service queue activation
    HashMap<String, String> zoneAdvMap = new HashMap<>();
    ctx.setAttribute("ZoneAdv", zoneAdvMap);

    Properties pMap = new Properties();
    try {
      InputStream is = ctx.getResourceAsStream(MONITORCFG);
      if (is != null) {
        pMap.load(is);
        logger.finest(pMap.toString());
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, ContextListener.class.getName() + ":contextInitialized():Error reading presets.:{0}", ex.getMessage());
    }

    //Configure JMS (Apache MQ)
    jmsMan = MessagingManager.getInstance(ctx);
    if (!jmsMan.configure(p)) {
      logger.warning(ContextListener.class.getName() + ":contextInitialized():JMS Service not available. See logs for details.");
      ctx.setAttribute("OutOfService", true);
      return;
    } else {
      ctx.setAttribute("isJMSConnected", jmsMan.isConnected());
      //+++ do not start connection advisory on deployment 
      //jmsMan.startAdvMonitor("ConnectionAdvisory");
      //but SAF zone service listener...
      //...by default the zone by property 'zoneid'
      if (jmsMan.isConnected()) {
        logger.log(Level.FINER, "{0}:contextInitialized():Served zone {1}", new Object[]{ContextListener.class.getName(), zid});
        jmsMan.addServiceListener(zid);
        //...optional this EAR may serve other zones too
        for (final Map.Entry<Object, Object> entry : pMap.entrySet()) {
          zoneAdvMap.put((String) entry.getKey(), (String) entry.getValue());
          jmsMan.addServiceListener((String) entry.getKey());
          if (logger.getLevel().equals(Level.FINER)) {
            System.out.println("additionally served: " + entry.getKey() + " " + entry.getValue());
          }
        }
      } else {
        logger.log(Level.WARNING, "{0}:contextInitialized():AMQ Messaging not yet connected.", new Object[]{ContextListener.class.getName()});
      }
    }

    logger.log(Level.FINER, "{0}:context initialized():Done.", ContextListener.class.getName());
  }

  /**
   * @param evt0
   * @see
   * ServletContextAttributeListener#attributeAdded(ServletContextAttributeEvent)
   */
  @Override
  public void attributeAdded(ServletContextAttributeEvent evt0) {
    logger.log(Level.FINE, "ContextListener:attributeAdded():{0}", evt0.getName());
  }

  /**
   * @param evt0
   * @see
   * ServletContextAttributeListener#attributeReplaced(ServletContextAttributeEvent)
   */
  @Override
  public void attributeReplaced(ServletContextAttributeEvent evt0) {
    logger.log(Level.FINE, "ContextListener:attributeReplaced():{0}", evt0.getName());

    ServletContext ctx = evt0.getServletContext();
    if (evt0.getName().equalsIgnoreCase("Properties")) {
      p = (Properties) ctx.getAttribute("Properties");
      //stop existing listeners
      Map<String, SAFQueueListener> mAppList = (HashMap) ctx.getAttribute("SAFQueueListener");
      if (!mAppList.isEmpty()) {
        for (SAFQueueListener ql : mAppList.values()) {
          if (ql.isSession()) {
            ql.stop();
          }
        }
      }
      AdvConnectionListener advJMSListener = (AdvConnectionListener) ctx.getAttribute(AdvConnectionListener.class.getName());
      if (advJMSListener != null) {
        advJMSListener.stop();
      }
      Map<String, HttpSession> websessions = (Map) ctx.getAttribute("CurrentSessions");
      for (Map.Entry<String, HttpSession> entry : websessions.entrySet()) {
        HttpSession sess = entry.getValue();
//        ChatHandler chatHandler = (ChatHandler) sess.getAttribute("ChatHandler");
//        if (chatHandler != null) {
//          //TODO I18N
//          //chatHandler.sendMsg("of center " + p.getProperty("centerID") + " leaves the chat.");
//          chatHandler.sendMsg("hat den Chatraum verlassen.");
//          chatHandler.closeSession();
//        }
      }

      //reconfigure environment
      //initDataSource(ctx);
      jmsMan = MessagingManager.getInstance(ctx);
      if (!jmsMan.configure(p)) {
        logger.log(Level.WARNING, "{0}:attributeReplaced():JMS Service not available. See logs for details.", ContextListener.class.getName());
        ctx.setAttribute("OutOfService", true);
      } else {
//        try {
//          //ctx.setAttribute("MessagingManager", jmsMan);
//          jmsMan.startAdvMonitor("ConnectionAdvisory");
//        } catch (GenericSEHRException ex) {
//          Logger.getLogger(ContextListener.class.getName()).log(Level.INFO, null, ex.getMessage());
//        }
      }
    }

  }

  /**
   * @param evt0
   * @see
   * ServletContextAttributeListener#attributeRemoved(ServletContextAttributeEvent)
   */
  @Override
  public void attributeRemoved(ServletContextAttributeEvent evt0) {
    logger.log(Level.FINE, "ContextListener:attributeRemoved():{0}", evt0.getName());

  }

  /**
   * @param evt0
   * @see ServletContextListener#contextDestroyed(ServletContextEvent)
   */
  @Override
  public void contextDestroyed(ServletContextEvent evt0) {
    logger.log(Level.FINE, "ContextListener:contextDestroyed():{0}", evt0.getServletContext().getServletContextName());
//    if (cronJob != null) {
//      logger.finer("ContextListener:contextDestroyed():Clearing timers.");
//      cronJob.clearTimers();
//    }
    ServletContext ctx = evt0.getServletContext();
    Map<String, SAFQueueListener> mAppList = (HashMap) ctx.getAttribute("SAFQueueListener");
    if (!mAppList.isEmpty()) {
      for (SAFQueueListener ql : mAppList.values()) {
        if (ql.isSession()) {
          ql.stop();
        }
      }
    }
    AdvConnectionListener advJMSListener = (AdvConnectionListener) ctx.getAttribute(AdvConnectionListener.class.getName());
    if (advJMSListener != null) {
      advJMSListener.stop();
    }
    jmsMan = MessagingManager.getInstance(ctx);
    jmsMan.close();

    logger.fine("ContextListener:contextDestroyed():Done.");
  }

//  private File getSubdirWEBINF(String sub) {
//    String className = getClass().getResource("ContextListener.class").getFile();
//    String path = className.substring(0, className.indexOf("WEB-INF") + "WEB-INF".length());
//    return new File(path, sub);
//  }
}
