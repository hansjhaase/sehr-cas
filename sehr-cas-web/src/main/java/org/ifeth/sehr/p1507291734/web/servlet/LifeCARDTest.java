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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang.StringUtils;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetServices;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;

/**
 * Do some LifeCARD tests.
 *
 * <p>
 * This servlet is just for testing some LifeCARD(R) basic operations.
 * </p>
 *
 * @author Hans J Haase hansjhaase@mdigmbh.de
 */
@WebServlet(name = "LifeCARDTest", urlPatterns = {"/LifeCARDTest"})
public class LifeCARDTest extends HttpServlet {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @EJB
  private LifeCARDAdmin ejbLCAdmin;

  /**
   * Check the LifeCARD(R) service (EJB) and XNET data exchange.
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
    int zid = Integer.parseInt(p.getProperty("zoneid", "0"));

    String outInfo = "<p>Unknown command.</p>";

    String action = request.getParameter("action");
    if (action == null) {
      action = "listservices";
    }

    if (action.equalsIgnoreCase("listservices")) {
      List<NetServices> list = ejbLCAdmin.listServices(zid);
      if (list != null && !list.isEmpty()) {
        outInfo = "<p>Services at this zone for processing LifeCard(R):</p>";
        outInfo += "<ul>";
        for (NetServices s : list) {
          outInfo += "<li>" + s.toString() + "</li>";
        }
        outInfo += "</ul>";
      } else {
        outInfo = "<p>No services found. Register at least one service.</p>";
      }
    } else if (action.equalsIgnoreCase("listcenters")) {
      List<NetCenter> list = ejbLCAdmin.listCenters(zid, -1);
      if (list != null && !list.isEmpty()) {
        outInfo = "<p>Centers of this zone that are using a LifeCard(R) service:</p>";
        outInfo += "<ul>";
        for (NetCenter c : list) {
          outInfo += "<li>" + c.toString() + "</li>";
        }
        outInfo += "</ul>";
      } else {
        outInfo = "<p>No Centers found that have registered a LifeCard(R) service.</p>";
      }
    } else if (action.equalsIgnoreCase("checkxnet")) {
      outInfo = "<p>Not yet fully implemented.</p>";
      //TODO send test message to global XNET root - get time of response
      Map<String, Object> status = ejbLCAdmin.getXNetComponent();
      if (status != null) {
        if ((boolean) status.get("XNETStarted") == false) {
          outInfo = "<span style='color:red;'>No XNet connection.</span>";
        } else {
          outInfo += "<p>XNET Connection:</p><ul>";
          for (Entry<String, Object> entry : status.entrySet()) {
            Object o = entry.getValue();
            outInfo += "<li>" + entry.getKey() + ": " + o + "</li>";
          }
          outInfo += "</ul>";
        }
      } else {
        outInfo = "<span style='color:red;'>No XNet service (component).</span>";
      }

    } else if (action.equalsIgnoreCase("checkregistration")) {
      String number = request.getParameter("number");
      int lczid;
      if (StringUtils.isBlank(number)) {
        //Jane Doe (Patient 0 at center 0) by convention for testing...
        String ctry = p.getProperty("country", "de");
        number = ctry.toUpperCase() + "-" + String.format("%07d", zid) + "-0000000-00000000";
      }
      if (number.matches("([A-Z]{2}-)(\\d{7}-)(\\d{7}-)(\\d{8})")) {
        CamelContext cc = ejbLCAdmin.getXNetContext();
        if (cc == null) {
          outInfo = "<span style='color:red;'>No XNet connection.</span>";
        } else {
          outInfo = "<p style='color:black;'>Checking registration for '" + number + "'</p>";
          //--- check global SEHR service for accessing card registrations
          //By convention a message to 'sehr.lc.<LCNR>.queue' has to be routed to a 
          //global messaging bus.

          //outInfo += "<h3>Testing LDAP</h3>";//<p>Not yet fully implemented....</p>";
          //LDAP does contain only registration (administrative) and ICE data
          //as well as the public key (if any) of the patient
          //LDAP may contain the connected devices (mobile, laptop) for push 
          //notifications
          //LDAP does not contain private data like name, address, date of birth
          //TODO access global e-hn.org LDAP context
          outInfo += "<h3>Testing XNET Messaging</h3>";
          //outInfo +="<p>Not yet fully implemented....</p>";
          outInfo += "<p>";
          outInfo += "Context Endpoints: " + cc.getName() + "<br/>";
          Map<String, Endpoint> endpoints = cc.getEndpointMap();
          for (Entry<String, Endpoint> entry : endpoints.entrySet()) {
            //System.out.println(entry.getKey());
            outInfo += "- " + entry.getKey() + ", <br/>";
          }
          outInfo += "</p>";

          outInfo += "<p>Processing test message to the card (the queue the holder can read) " + number + " using internal endpoint \"direct:LifeCARDOut\"...<br/>";
          try {
            ProducerTemplate template = cc.createProducerTemplate();

            //TODO define LC related messaging attributes
            Map<String, Object> headers = new HashMap<>();
            headers.put("lcNumber", number);

            Map<String, Object> body = new HashMap<>();
            body.put("boolean value", true);
            body.put("sent DT (long)", System.currentTimeMillis());
            //template.sendBody("xdom:queue:sehr.lc." + lcid, body);
            template.sendBodyAndHeaders("direct:LifeCARDOut", body, headers);
            outInfo += "Message sent.";
          } catch (CamelExecutionException ex) {
            //ex.printStackTrace();
            Log.log(Level.SEVERE, LifeCARDTest.class.getName() + ":Test failed:" + ex.getMessage());
            outInfo += "<span style='color:red;'>Message not sent." + ex.getMessage();
          }
          outInfo += "</p>";
        }
        String parts[] = number.split("\\-");
        //The DB IntraSEC of a zone host contains the data record 
        //(LifeCardItem Object). But the host of 
        //the zone may be offline (or disconnected sometimes). 
        //Requesting other zones must be done asynchronously! 
        lczid = Integer.parseInt(parts[1]);
        String lcctry = parts[0];
        int lccid = Integer.parseInt(parts[2]);
        int lcpid = Integer.parseInt(parts[3]);
        if (zid == lczid) {
          //If we are at the same zone (host) we can use a DB query. 
          outInfo += "<h3>Testing for LC entry at local zone host (ZID " + zid + ")</h3>";
          outInfo += "<p style='color:black;'>Getting LC objects by DB queries from DB of this host with zone ID "+zid+" for card number '"+number+"'</p>";

          LifeCardItem lcItem = ejbLCAdmin.getLifeCardItem(lcctry, lczid, lccid, lcpid);
          if (lcItem == null) {
            outInfo += "No item (record) found for " + number;
          } else {
            outInfo += "<p>LifeCARD Item Object (the printed card reference)<br/>" + lcItem.toString() + "</p>";
            LcMain lcMain = ejbLCAdmin.getLcMainByNumber(number);
            if (lcMain == null) {
              outInfo += "Upps. No administrative record found for card " + number;
            } else {
              outInfo += "<p>LifeCARD Admin Object (the patient/user/holder record - LcMain)<br/>" + lcMain.toString() + "</p>";
            }
          }
        } else {
          //Requesting other zones must be done by messaging!
          outInfo += "<h3>Testing for LC entry at another zone host (ZID " + zid + ")</h3>";
          if (cc == null) {
            outInfo += "<span style='color:red;'>No XNet connection to check by messaging.</span>";
          } else {
            outInfo += "<p style='color:black;'>Sending message to zone " + parts[1] + " to get LC object...</p>";
            //TODO create pending requests spooler
            outInfo += "<p>Not yet implemented.</p>";
          }
        }
      } else {
        outInfo += "<p>Syntax error of LCID. The valid syntax is 'XX-0000000-0000000-00000000'</p>";
      }
    }

    PrintWriter out = response.getWriter();
    try {
      out.println("<!DOCTYPE html>");
      out.println("<html>");
      out.println("<head>");
      out.println("<title>LifeCARD Service Test</title>");
      out.println("<style>");
      out.println(".h1 {font-size:12px;}");
      out.println(".h2 {font-size:10px;}");
      out.println(".h3 {font-size:10px;}");
      out.println(".p {font-size:10px;}");
      out.println("</style>");
      out.println("</head>");
      out.println("<body>");
      out.println("<h1>LifeCARD Service Test</h1>");
      out.println("<p>" + outInfo + "</p>");
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
