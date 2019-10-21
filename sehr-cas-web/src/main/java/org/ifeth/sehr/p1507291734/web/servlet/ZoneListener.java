/*
 * (C) 2015 MDI GmbH for the SEHR Community
 * Licensed under the European Union Public Licence - EUPL v.1.1 ("License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://ec.europa.eu/idabc/servlets/Doc?id=31979
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Refactored from 'sehr-saf' on 3.08.2015
 */
package org.ifeth.sehr.p1507291734.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.exception.GenericSEHRException;
import org.ifeth.sehr.core.objects.UserSessionObject;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
import org.ifeth.sehr.p1507291734.web.beans.MQClients;
import org.ifeth.sehr.p1507291734.web.inc.HTMLFrameUtils;
import org.ifeth.sehr.p1507291734.web.listener.AdvConnectionListener;
import org.ifeth.sehr.p1507291734.web.listener.SAFQueueListener;
import org.ifeth.sehr.p1507291734.web.listener.XNetMessaging;

/**
 * Start/stop SEHR service queue of the zone this SEHR CAS is running on or a
 * zone managed by this host.
 *
 * <p>
 * The prefix of all SEHR based messages of a zone by convention is
 * <b>'sehr.[ZoneID].</b>. The next part defines the handler or a center (ID, 7
 * digits). A handler always (must) start with a character and should be a
 * string, e.g. 'chat' or 'service' etc.<br/>
 * Important: There can be only one handler per service - otherwise a message
 * may not be processed as expected!<br/>
 * The postfix (closing) string is always 'topic' or 'queue'.
 * </p>
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@WebServlet(name = "ZoneListener", urlPatterns = {"/ZoneListener"})
public class ZoneListener extends HttpServlet {

  private static final Logger logger = Logger.getLogger("org.ifeth.sehr.p1507291734.web.servlet");
  private static final String MONITORCFG = "/WEB-INF/ZoneAdv.map";

  //private String sZoneId;
  /**
   * Process actions (requests) to start or stop a service listener of a zone.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    ServletContext sctx = getServletContext();
    Properties p = (Properties) sctx.getAttribute("Properties");
    HttpSession sess = (HttpSession) request.getSession();
    String outInfo = "<span style='color:red;'>Login required</span><br/>";

    //--- check for a valid login
    UserSessionObject usrSessObj = (UserSessionObject) sess.getAttribute("UserSessionObject");
    //System.out.println(usrSessObj == null ? "UserSessionObject is null" : usrSessObj.toString());
    boolean access = false;
    if (usrSessObj == null || !usrSessObj.getSessionid().equals(sess.getId())) {
      //login param required if not logged in by main site
      String username = request.getParameter("username");
      String pw = request.getParameter("password");
      if (username != null && username.equals(p.getProperty("moduleOwner"))
              && pw != null && pw.equals(p.getProperty("modulePw"))) {
        access = true;
      } else {
        outInfo += "<p>Oups - name and/or password don't match with settings. Try again.</p>";
      }
    } else {
      access = true;
    }
    String queue = null;
    ActiveMQConnection amqConnection = null;
    if (!access) {
      outInfo += "<p>Use <a href='" + request.getContextPath() + "/index.jsp'>main site</a> or known moduleOwner settings with" + request.getContextPath() + "/ZoneListener?action=...&username=...&password=...'</p>";
    } else {
      String action = request.getParameter("action");
      //commands: start, stop, restart, info
      outInfo = "<p>ZoneListener: Unknown command.</p>";
      if (action != null) {
        MessagingManager jmsMan = MessagingManager.getInstance(sctx);
        if (action.equalsIgnoreCase("restart")) {
          //restart messaging and configured zone services
          if (!jmsMan.configure(p)) {
            logger.warning(ZoneListener.class.getName() + ":processRequest():JMS Service not available. See logs for details.");
            sctx.setAttribute("OutOfService", true);
            outInfo = "<p>ZoneListener: Messaging could not be configured for ".concat(p.getProperty("zoneid", "0000000")).concat("</p>");
          } else {
            sctx.setAttribute("isJMSConnected", jmsMan.isConnected());
            //SAF zone service listener
            //...this SEHR CAS ist serving by property settings
            jmsMan.addServiceListener(p.getProperty("zoneid", "0000000"));
            outInfo = "ZoneListener listener restarted for<br/>".concat(p.getProperty("zoneid", "0000000"));

            //jmsMan.configureAdvMonitor("ConnectionAdvisory");
            HashMap<String, String> zoneAdvMap = (HashMap) sctx.getAttribute("ZoneAdv");
            Properties pMap = new Properties();
            StringBuilder outStatus = new StringBuilder();
            try {
              //pMap.load(Thread.currentThread().getContextClassLoader().getResource("/WEB-INF/center2zone.map").openStream());
              InputStream is = sctx.getResourceAsStream(MONITORCFG);
              if (is != null) {
                pMap.load(is);
                logger.finest(pMap.toString());

                for (final Map.Entry<Object, Object> entry : pMap.entrySet()) {
                  zoneAdvMap.put((String) entry.getKey(), (String) entry.getValue());
                  if (jmsMan.isConnected()) {
                    String z = (String) entry.getKey();
                    jmsMan.addServiceListener(z);
                    outStatus.append(z).append(", ");
                  }
                }
                outInfo.concat(outStatus.toString());
              }
            } catch (IOException ex) {
              logger.log(Level.WARNING, ZoneListener.class.getName() + ":initMessaging():Error reading presets.:{0}", ex.getMessage());
              outInfo = "Restarting service listener failed.<br/>";
            }
          }
        } else {
          amqConnection = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");
          if (amqConnection == null || amqConnection.isClosed()) {
            outInfo = "<p style='color:red;font-family:bold;'>ZoneListener: Error - no local messaging service!</p>";
            outInfo += "<p style='color:blue;'>Check settings and/or start Apache MQ...</p>";
          } else {
            Map<String, SAFQueueListener> safListeners = (HashMap) sctx.getAttribute("SAFQueueListener");
            String zid = request.getParameter("zoneid");
            //by default the service queue of the zone of this SEHR-CAS host
            int zoneid = Integer.parseInt(p.getProperty("zoneid", "0000000"));
            if (zid != null && zid.matches("\\d+")) {
              zoneid = Integer.parseInt(zid);
            }
            SAFQueueListener serviceListener;
            //'0000000' is not a valid zone id!
            if (zoneid > 0) {
              queue = "sehr." + String.format("%07d", zoneid) + ".service.queue";
            } else {
              outInfo = "<p style='color:red;font-family:bold;'>ZoneListener: Error - " + String.format("%07d", zoneid) + " is not a valid zone!</p>";
            }
            if (action.equalsIgnoreCase("start") && StringUtils.isNotBlank(queue)) {
              if (queue != null && safListeners.containsKey(queue)) {
                serviceListener = (SAFQueueListener) safListeners.get(queue);
                serviceListener.stop();
                safListeners.remove(queue);
                outInfo = "<p>Message processing on '" + queue + "' has been stopped for restart. / Broker URL=" + amqConnection.getBrokerInfo().getBrokerURL() + "</p>";
              }
              if (!jmsMan.isConnected()) {
                outInfo += "<p>No connection to '" + jmsMan.toString() + "' has been stopped for restart. / Broker URL=" + amqConnection.getBrokerInfo().getBrokerURL() + "</p>";

              }
              serviceListener = new SAFQueueListener();
              if (serviceListener.configure(sctx, queue)) {
                safListeners.put(queue, serviceListener);
                logger.log(Level.INFO, ":processRequest():Message processing on '" + queue + "' started / URL=" + amqConnection.getBrokerInfo().getBrokerURL());
                outInfo = "<p>Message processing on '" + queue + "' has been started. / Broker URL=" + amqConnection.getBrokerInfo().getBrokerURL() + "</p>";
              } else {
                outInfo = "<p>Error: Listening on '" + queue + "' failed. / Broker URL=" + amqConnection.getBrokerInfo().getBrokerURL() + "</p>";
              }
            } else if (action.equalsIgnoreCase("stop") && StringUtils.isNotBlank(queue)) {
              if (safListeners.containsKey(queue)) {
                serviceListener = (SAFQueueListener) safListeners.get(queue);
                serviceListener.stop();
                safListeners.remove(queue);
                outInfo = "<p>Message processing on '" + queue + "' has been stopped. / Broker URL=" + amqConnection.getBrokerInfo().getBrokerURL() + "</p>";
              } else {
                outInfo = "<p>'" + queue + "' not in list / Broker URL=" + amqConnection.getBrokerInfo().getBrokerURL() + "</p>";
              }
            } else if (action.equalsIgnoreCase("monitor")) {
              try {
                jmsMan.startAdvMonitor("ConnectionAdvisory");
                outInfo = "Monitoring of connections started.<br/>";
              } catch (GenericSEHRException ex) {
                Logger.getLogger(ZoneListener.class.getName()).log(Level.INFO, null, ex.getMessage());
                outInfo = "Monitoring already started.<br/>";
              }
            } else if (action.equalsIgnoreCase("info")) {
              SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm z");
              outInfo = "Local domain: " + p.getProperty("subdomain", "?sub?") + "." + p.getProperty("domain", "n/a") + "<br/>";
              outInfo += "Local AMQ Messaging since: " + sdf.format(amqConnection.getTimeCreated()) + "<br/>";
              outInfo += "SEHR XNET root (global bus) is: " + p.getProperty("sehrxnetroot", "n/a") + "<br/>";
              XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");
              if (xnetMessenger != null) {
                outInfo += xnetMessenger.getInfo(true);
              } else {
                outInfo += "<span style='color:red;'>No SEHR XNET messaging running!</span> ";
                outInfo += "<a href='./XNet?action=start'>Klick</a> to start XNET now.<br/>";
              }
              //list is generated for all action use cases below 
            } else {
              outInfo = "Use '" + request.getContextPath() + "?action=start&zoneid=[ZONEID]', '...stop&zoneid=[ZONEID]' or 'action=monitor|info'";
            }
          }
        }
      } else {
        outInfo = "Use '" + request.getContextPath() + "?action=start&zoneid=[ZONEID]', '...stop&zoneid=[ZONEID]' or 'action=monitor|info'";
      }
    }
    PrintWriter out = response.getWriter();
    try {
      out.println(HTMLFrameUtils.htmlHeaderJMedCss("SEHR - Cloud Administration Service"));

      out.println("<body>");
      out.println("<h1>SEHR Service Monitor</h1>");
      out.println("<p>" + outInfo + "</p>");

      if (access && amqConnection != null && !amqConnection.isClosed()) {
        out.println("<h2 style='font-size:12px;'>List of Zones this SEHR host is serving/monitoring</h2>");
        Map<String, SAFQueueListener> mAppList = (HashMap) sctx.getAttribute("SAFQueueListener");
        if (!mAppList.isEmpty()) {
          for (String entry : mAppList.keySet()) {
            out.println("* " + entry + "<br/>");
          }
        }
        out.println("<h2 style='font-size:12px;'>List of Connections</h2>");

        AdvConnectionListener adv = (AdvConnectionListener) sctx.getAttribute(AdvConnectionListener.class.getName());
        if (adv == null) {
          out.println("- no monitoring of connections (AdvConnectionListener is null) -");
        } else if (adv.getMQClients().isEmpty()) {
          out.println("- no monitored connections in list -");
        } else {
          out.println("<ol>");
          for (MQClients c : adv.getMQClients().values()) {
            String fromIP = c.getClientIP();
            String clientId = c.getClientId();
            out.println("<li>" + fromIP + ": " + clientId + "</li>");
          }
          out.println("</ol>");
        }
      }
      out.println("</body>");
      out.println("</html>");
    } finally {
      out.close();
    }
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>

}
