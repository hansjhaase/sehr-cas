/*
 * (C)2016 MDI for the SEHR community
 */
package org.ifeth.sehr.p1507291734.web.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import org.ifeth.sehr.core.objects.LifeCardItem;
import org.ifeth.sehr.p1507291734.web.model.LifeCARDDAO;

/**
 * REST Web Service.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@RequestScoped
public class LifeCARDItemResource {

  @Context
  UriInfo uriInfo;
  @Context
  Request request;

  String cardid;

  /**
   * Creates a new instance of LifeCARDItemsResource.
   *
   * @param uriInfo
   * @param request
   * @param cardid
   */
  public LifeCARDItemResource(UriInfo uriInfo, Request request, String cardid) {
    this.uriInfo = uriInfo;
    this.request = request;
    this.cardid = cardid;
  }

  //Application integration     
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public LifeCardItem getLifeCardItem() {
    LifeCardItem item = LifeCARDDAO.getInstance().getList().get(cardid);
    if (item == null) {
      throw new RuntimeException("@GET:getLifeCardItem():LifeCardItem with " + cardid + " not found");
    }
    return item;
  }

  // for the browser

  @GET
  @Produces(MediaType.TEXT_XML)
  public LifeCardItem getLifeCardItemHTML() {
    LifeCardItem item = LifeCARDDAO.getInstance().getList().get(cardid);
    if (item == null) {
      throw new RuntimeException("@GET:getLifeCardItemHTML():LifeCardItem with " + cardid + " not found");
    }
    return item;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_XML)
  public Response putLifeCardItem(JAXBElement<LifeCardItem> item) {
    LifeCardItem c = item.getValue();
    return putAndGetResponse(c);
  }

  /**
   * Fast deactivation of a card.
   * <p>
   * Use case: A REST based service to deactivate a card if it has been stolen.
   * </p>
   */
  @DELETE
  public void deactivateLifeCardItem() {
    LifeCardItem item = LifeCARDDAO.getInstance().getList().remove(cardid);
    if (item == null) {
      throw new RuntimeException("Delete: LifeCardItem with " + cardid + " not found");
    }
  }

  private Response putAndGetResponse(LifeCardItem item) {
    Response res;
    String id = Integer.toString(item.getLcid());
    if (LifeCARDDAO.getInstance().getList().containsKey(id)) {
      res = Response.noContent().build();
    } else {
      res = Response.created(uriInfo.getAbsolutePath()).build();
    }
    LifeCARDDAO.getInstance().getList().put(id, item);
    return res;
  }

}
