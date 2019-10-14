<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="org.ifeth.sehr.p1507291734.web.UAgentInfo"%>

<%
  String userAgent = request.getHeader("User-Agent");
  String httpAccept = request.getHeader("Accept");

  UAgentInfo detector = new UAgentInfo(userAgent, httpAccept);
  System.out.println(detector.toString());
  //detect whether the visitor is using a mobile device.
  String mode = request.getParameter("mode");
  if (detector.detectTierIphone()||"mobile".equalsIgnoreCase(mode)) {
   response.sendRedirect("./faces/mobile.xhtml");
   return;
  } 
  //else {
  // response.sendRedirect("./index.xhtml");
  // //response.sendRedirect("http://my.j-med.de/index.xhtml");
  // return;
  //}
%> 
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>SEHR Community Administration Service</title>
    </head>
    <body>
      <jsp:forward page="./faces/index.xhtml"></jsp:forward>
    </body>
</html>