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
 * Since 08.2015
 */
package org.ifeth.sehr.p1507291734.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.core.objects.UserSessionObject;
import org.ifeth.sehr.p1507291734.web.MessagingManager;
import org.ifeth.sehr.p1507291734.web.inc.HTMLFrameUtils;
import org.ifeth.sehr.p1507291734.web.listener.XNetMessaging;

/**
 * Start/Stop and listen to the SEHR message bus (zone-2-zone transfers).
 *
 * <p>
 * The prefix of all SEHR based messages is by convention
 * <b>'sehr.[ZoneID]</b> or <b>'sehr.xnet.[domain]'</b>.<br/>
 * The next part on a zone based messaging defines the <b>handler</b> or a
 * <b>center (ID, 7 digits)</b>. A handler must start with a character and
 * should be a string, e.g. <b>'app', 'lc' 'chat', 'service'</b> etc.</p>
 * <p>
 * <u>Important:</u> There can be only one processing component (handler) of the
 * same type like <b>'app.p1234567'</b> on the same messaging host (broker).
 * Otherwise a message may not be processed as expected! The handlers have an
 * own endpoint specification like<br/>
 * <b>app.[PIK(=AppToken)].queue</b> or<br/>
 * <b>lc.[country]-[zid]-[cid]-[pid].queue</b>
 * </p>
 * <p>
 * The postfix (closing) string is always <b>'topic'</b> or <b>'queue'</b>.<br/>
 * All messages are handled by a message broker. The broker can handle multiple
 * queues of different zones and their centers also. Messages transferred
 * between zones are handled by <b>'sehr.xnet.[domain].queue'</b> or
 * <b>'-.topic'</b> using the <b>domain broker</b> and <b>Apache Camel routes</b>.
 * </p>
 * <p>
 * The <b>XNET</b> is a SEHR messaging bus allowing to handle EHR messages
 * between zones. The messages can be routed and processed (using Apache Camel).
 * Every zone can be registered to the bus for sending and receiving messages
 * with other zones (SEHR messaging services) like public hosts as a gateway to
 * mobile devices. By convention the XNET bus is domain based. So the zone where
 * this SEHR-CAS service is running and activated on become XNET sender/receiver
 * for itself (and its centers and subzones) and may (should) connect then to
 * the next upper level zone (domain broker). For example:<br/>
 * [xnet.z9999997.de.e-hn.org]&lt;-&gt;[xnet.de.e-hn.org]&lt;-&gt;[xnet.e-hn.org]
 * (root level)<br/>
 * </p>
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@WebServlet(name = "XNetProcessor", urlPatterns = {"/XNet"})
public class XNetService extends HttpServlet {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  /**
   * Handle actions to start/stop or check the XNET messaging bus.
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

    //--- check for login
    UserSessionObject usrSessObj = (UserSessionObject) sess.getAttribute("UserSessionObject");
    //System.out.println(usrSessObj == null ? "UserSessionObject is null" : usrSessObj.toString());
    boolean access = false;
    String content = "<span style='color:red;'>Login required</span><br/>";
    content += "Use <a href='" + request.getContextPath() + "/index.jsp'>main site</a> or known moduleOwner settings with" + request.getContextPath() + "/XNet?action=...&username=...&password...'";
    if (usrSessObj == null || !usrSessObj.getSessionid().equals(sess.getId())) {
      //login param required if not logged in by main site
      String username = request.getParameter("username");
      String pw = request.getParameter("password");
      if (username != null && username.equals(p.getProperty("moduleOwner"))
              && pw != null && pw.equals(p.getProperty("modulePw"))) {
        access = true;
      } else {
        content += "Oups - name and/or password don't match with settings. Try again.";
      }
    } else {
      access = true;
    }

    if (access) {
      content = "Action not defined. Try '" + request.getContextPath() + "/XNet?action=start' or '...stop'";
      String action = request.getParameter("action");

      XNetMessaging xnetMessenger = (XNetMessaging) sctx.getAttribute("XNetMessaging");

      //queue for domain name based cross networking processing, 
      //the purpose is to get messages and deliver or process them
      if (action != null) {
        if (action.equalsIgnoreCase("start")) {
          if (xnetMessenger != null && xnetMessenger.isStarted()) {
            content = "SEHR XNet processor already running<br/>";
            content += xnetMessenger.getInfo(true);
          } else {
            if (!p.containsKey("sehrxnetroot")) {
              content = "SEHR XNet root domain not defined (Property 'sehrxnetroot')<br/>";
            } else if (!p.containsKey("domain")) {
              content = "Property 'domain' not defined<br/>";
            } else {
              //e.g. we're listenening on level 'de.e-hn.org' for messages
              String queueSehrXNet = "sehr.xnet." + p.getProperty("domain") + ".queue";
              String brokerURLSehrXNet = request.getParameter("amqurl");
              //TODO add user, pw for XNET broker
              if (brokerURLSehrXNet != null) {
                p.setProperty("sehrxnetdomainurl", brokerURLSehrXNet);
              } else {
                Log.fine("Using XNET settings:" + p.getProperty("sehrxnetdomainurl"));
              }
              //The subdomain XNet broker is responsible for and its children 
              //(other subnetted zones) or leafs (centers/endpoints))
              String subdomain = p.getProperty("subdomain", "");
              //parent level domain to exchange data through (by next xnet bus)
              String domain = p.getProperty("domain", "");
              String localDomain = subdomain + "." + domain;
              if (StringUtils.isBlank(subdomain)) {
                content += "Configuration Error: 'subdomain' must be a country code like 'de' or a valid zone name like 'z[ZID]'<br/>";
              } else if (StringUtils.isBlank(domain)) {
                content += "Configuration Error: 'domain' must be a valid next level domain name like 'de.e-hn.org'<br/>";
              } else if (localDomain.equalsIgnoreCase(p.getProperty("sehrxnetdomain", ""))) {
                content += "XNET domain is same as receiving domain. Messages will run in a loop!<br/>";
              } else {
                String queueLocalDomain = "sehr.xnet." + localDomain + ".queue";
                //We're using Apache Camel for message routing and processing!
                //by convention the basic attribute is the local broker
                ActiveMQConnection amqCon = (ActiveMQConnection) sctx.getAttribute("ActiveMQConnection");
                if (amqCon == null || amqCon.isClosed()) {
                  MessagingManager jmsMan = MessagingManager.getInstance(sctx);
                  if (!jmsMan.configure(p)) {
                    Log.warning(XNetService.class.getName() + ":processRequest():start:JMS Service not available. See logs for details.");
                    content = "No local broker connected for messaging.<br/>";
                    sctx.setAttribute("OutOfService", true);
                  } else {
                    //+++ HansJHaase: done by MessagingManager:initJMS
                    //sctx.setAttribute("ActiveMQConnection", jmsMan.getAMQConnection());
                    sctx.setAttribute("isJMSConnected", jmsMan.isConnected());
                    amqCon = jmsMan.getAMQConnection();
                  }
                  //content = "No local broker connected for messaging.<br/>";
                }
                if (amqCon != null && !amqCon.isClosed()) {
                  xnetMessenger = new XNetMessaging(p);
                  xnetMessenger.setCountry(p.getProperty("country", "de"));
                  if (xnetMessenger.connect(queueSehrXNet, queueLocalDomain)) {
                    //store XNET processor in the application/container context
                    sctx.setAttribute("XNetMessaging", xnetMessenger);
                    content = xnetMessenger.getInfo(true);
                    content += xnetMessenger.getRoutesInfo(true);
                  }
                }
              }
            }
          }
        } else if (action.equalsIgnoreCase("stop")) {
          if (xnetMessenger != null) {
            content = "Disconnecting '" + xnetMessenger.getXNetLocalDomain() + "' from SEHR XNet";
            //xnetMessenger.stop(); //session
            xnetMessenger.close(); //connection (and stops session of course)
            sctx.removeAttribute("XNetMessaging");
          } else {
            content = "No XNET messaging listener found.";
          }
        } else if (action.equalsIgnoreCase("test")) {
          if (xnetMessenger != null) {
            String msg = request.getParameter("message");
            if (msg != null) {
              if (xnetMessenger.test(msg)) {
                content = "Test message sent to XNet bus '" + xnetMessenger.getXNetDomainBus() + "' for '" + xnetMessenger.getXNetLocalDomain() + "'.";
              } else {
                content = "<span style='color:red;'>'message' not sent. See Log!</span>.";
              }
            } else {
              content = "<span style='color:red;'>Missing 'message' parameter</span>. Try " + request.getContextPath() + "/XNet?action=test&message=Lorum%20ipsum[|stress]";
            }
          } else {
            content = "No XNET service to send test message!";
          }
        }
      }
    }
    PrintWriter out = response.getWriter();

    try {
      out.println(HTMLFrameUtils.htmlHeaderJMedCss("SEHR - Cloud Administration Service"));

      out.println("<body>");
      out.println("<h1>Zone-2-Zone Transfers (SEHR XNET messaging bus)</h1>");
      out.println("<p>" + content + "</p>");
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
