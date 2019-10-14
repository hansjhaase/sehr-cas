/*
 * (C)2016 MDI for the SEHR community
 */
package org.ifeth.sehr.p1507291734.web.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.p1507291734.web.model.LifeCARDDAO;

/**
 * LifeCARD(R) Item RESTful WEB Service (lcis).
 *
 * <p>
 * The LifeCARD(R) item is the public, printed part of the card. By this service
 * there are no medical data available by convention - only the data printed on
 * the card to verify the holder, status, ICE and demographic data.
 * </p>
 * <p>
 * The public key of the patient is available by '/rest/lifecard/id/[id]'.
 * </p>
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@RequestScoped
@Path("lcis")
public class LifeCARDItemsResource {

  @Context
  private UriInfo uriInfo;
  @Context
  private Request request;
  @Context
  private SecurityContext securityContext;

  // Return the list of LifeCardItems in the browser
  @GET
  @Produces(MediaType.TEXT_XML)
  public List<LifeCardItem> getLifeCARDItemsBrowser() {
    List<LifeCardItem> items = new ArrayList<>();
    items.addAll(LifeCARDDAO.getInstance().getList().values());
    return items;
  }

  /**
   * Return the list of LifeCardItems registered on this host.
   *
   * @return
   */
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<LifeCardItem> getLifeCardItems() {

    List<LifeCardItem> items = new ArrayList<>();
    items.addAll(LifeCARDDAO.getInstance().getList().values());
    return items;
  }

  /**
   * The number of LifeCardItems registered on this host.
   *
   * Usage: http://localhost:8080/sehr-cas-web/rest/lcis/count
   *
   * @return 
   */
  @GET
  @Path("count")
  @Produces(MediaType.TEXT_PLAIN)
  public String getCount() {
    Map<String, LifeCardItem> list = LifeCARDDAO.getInstance().getList();
    int count = -1; //indicates an error
    if (list != null) {
      count = list.size(); //no error: >=0
    }
    return String.valueOf(count);
  }

  @POST
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void newLifeCardItem(@FormParam("country") String country,
          @FormParam("firstname") String firstname,
          @FormParam("surname") String surname,
          @Context HttpServletResponse servletResponse) throws IOException {
    final String username = securityContext.getUserPrincipal().getName();
    //debugging
    System.out.println("UserName by securityContext.getUserPrincipal():" + username);
    for (String s : servletResponse.getHeaderNames()) {
      System.out.println(s);
    }
    LifeCardItem item = new LifeCardItem();

    if (firstname != null) {
      item.setFirstname(firstname);
    }
    if (surname != null) {
      item.setSurname(surname);
    }
    //TODO save LcMain and get record id of LcMain ....
    int count = LifeCARDDAO.getInstance().getList().size();
    String idLcMain = Integer.toString(count++);
    Map<String, LifeCardItem> l = LifeCARDDAO.getInstance().getList();
    if (l == null) {
      l = new HashMap<>();
    }
    l.put(idLcMain, item);
    servletResponse.sendRedirect("../test/LifeCARD.html");
  }

  // Defines that the next path parameter after 'lcis' is
  // treated as a parameter and passed to the LifeCARDItemResource
  // Use case: http://localhost:8080/sehr-cas-web/rest/lcis/id/1
  @Path("id/{cardid}")
  public LifeCARDItemResource getLifeCARDItem(@PathParam("cardid") String cardid) {
    return new LifeCARDItemResource(uriInfo, request, cardid);
  }

}
