/*
 * (C)2016 MDI; developed for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.rest;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.enterprise.context.RequestScoped;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import org.ifeth.sehr.core.spec.SEHRConstants;
import org.ifeth.sehr.intrasec.entities.LcMain;
import org.ifeth.sehr.p1507291734.ejb.LifeCARDAdmin;
import org.ifeth.sehr.p1507291734.web.model.LifeCARDDAO;

/**
 * Simple REST Web Service to handle LifeCARD(R) administrative requests using
 * the LcMain entity.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Path("lifecard")
@RequestScoped
public class LcMainResource {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @Context
  private UriInfo context;
  @Context
  private ServletContext sctx;
  @EJB
  private LifeCARDAdmin ejbLCAdmin;

  /**
   * Creates a new instance of LifecardResource.
   */
  public LcMainResource() {
  }

  @GET
  @Path("list/{cc}/{zoneid}")
  @Produces(MediaType.TEXT_XML)
  public List<LcMain> getLcListByCountry(
          @PathParam("cc") String cc, @PathParam("zoneid") Integer zid) {
    List<LcMain> list = ejbLCAdmin.listRegistrations(cc, zid, null, SEHRConstants.LifeCARD_STS_CRDRCV);
    return list;
  }

  /**
   * List of administrative entries (LcMain) of activated cards for a given
   * country.
   *
   * @param cc
   * @return
   */
  @GET
  @Path("list/{cc}")
  @Produces(MediaType.TEXT_XML)
  public List<LcMain> getLcListByCountryZone(
          @PathParam("cc") String cc) {
    List<LcMain> list = ejbLCAdmin.listRegistrations(cc, null, null, SEHRConstants.LifeCARD_STS_CRDRCV);
    return list;
  }

  /**
   * List of administrative entries (LcMain) of activated cards for a given
   * country, zone and center.
   *
   * @param cc
   * @param zid
   * @param cid
   * @return
   */
  @GET
  @Path("list/{cc}/{zoneid}/{centerid}")
  @Produces(MediaType.TEXT_XML)
  public List<LcMain> getLcListByParams(
          @PathParam("cc") String cc,
          @PathParam("zoneid") Integer zid,
          @PathParam("centerid") Integer cid) {
    List<LcMain> list = ejbLCAdmin.listRegistrations(cc, zid, cid, SEHRConstants.LifeCARD_STS_CRDRCV);
    return list;
  }

  /**
   * Retrieve LcMain object by an app (client).
   *
   * @param id
   * @return LcMain - The administrative record as xml...
   */
  @GET
  @Path("id/{id}")
  @Produces("application/xml")
  public LcMain getLcMainXml(@PathParam("id") Integer id) {
    LifeCARDDAO dao = LifeCARDDAO.getInstance();
    LcMain lcMain = dao.getLcMain(id);
    return lcMain;
  }

  /**
   * Test the integration with a browser.
   *
   * @param id
   * @return LcMain - The administrative record as xml...
   */
  @GET
  @Path("id/{id}")
  @Produces({MediaType.TEXT_XML})
  public LcMain getLcMainHTML(@PathParam("id") Integer id) {
    LifeCARDDAO dao = LifeCARDDAO.getInstance();
    LcMain lcMain = dao.getLcMain(id);
    return lcMain;
  }

  /**
   * Retrieve LcMain object by an app (client) using the printed number.
   *
   * @param number
   * @return LcMain - The administrative record as xml...
   */
  @GET
  @Path("number/{number}")
  //@Produces("application/xml")
  @Produces({MediaType.TEXT_XML})
  public LcMain getLcMainByNumberXml(@PathParam("number") String number) {
    Log.info("getLcMainByNumberXml():param number=" + number);
    LcMain lcMain = ejbLCAdmin.getLcMainByNumber(number);
    return lcMain;
  }

  /**
   * PUT method for future use cases.
   *
   * @param lcMainXml representation for the resource
   */
  @PUT
  @Consumes("application/xml")
  public void putLcMainXml(String lcMainXml) {
    Log.info("@PUT not yet implemented.");
  }
}
