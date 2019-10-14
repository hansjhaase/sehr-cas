/*
 * (C) Th Horn
 */
package org.ifeth.sehr.p1507291734.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.ifeth.sehr.core.objects.UserSessionObject;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@WebServlet(name = "JNDIContext", urlPatterns = {"/JNDIContext"})
public class JNDIContext extends HttpServlet {

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    String outInfo = "<span style='color:red;'>Login required</span><br/>";
    ServletContext sctx = getServletContext();
    Properties p = (Properties) sctx.getAttribute("Properties");
    HttpSession sess = (HttpSession) request.getSession();
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
    if (!access) {
      outInfo += "<p>Use <a href='" + request.getContextPath() + "/index.jsp'>main site</a> or known moduleOwner settings with" + request.getContextPath() + "/ZoneListener?action=...&username=...&password=...'</p>";
    }
    PrintWriter out = response.getWriter();
    try {
      out.println("<!DOCTYPE html>");
      out.println("<html>");
      out.println("<head>");
      out.println("<title>Servlet JNDIContext</title>");
      out.println("</head>");
      out.println("<body>");
      out.println("<h1>Servlet JNDIContext</h1>");
      if (access) {
        Context ctx = new InitialContext();
        printJndiContextAsHtmlList(out, ctx, "");
        ctx.close();
      } else {
        out.println(outInfo);
      }
      out.println("</body>");
      out.println("</html>");
    } catch (NamingException ex) {
      Logger.getLogger(JNDIContext.class.getName()).log(Level.SEVERE, null, ex);
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

  private void printJndiContextAsHtmlList(PrintWriter writer, Context ctx, String name) {
    writer.println("<ul>");
    try {
      NamingEnumeration<Binding> en = ctx.listBindings("");
      while (en != null && en.hasMoreElements()) {
        Binding binding = en.next();
        String name2 = name + ((name.length() > 0) ? "/" : "") + binding.getName();
        writer.println("<li><u>" + name2 + "</u>: " + binding.getClassName() + "</li>");
        if (binding.getObject() instanceof Context) {
          printJndiContextAsHtmlList(writer, (Context) binding.getObject(), name2);
        }
      }
    } catch (NamingException ex) {
      // Normalerweise zu ignorieren
    }
    writer.println("</ul>");
  }
}
