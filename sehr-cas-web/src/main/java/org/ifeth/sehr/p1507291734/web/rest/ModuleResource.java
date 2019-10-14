/*
 * (C) MDI GmbH for the SEHR Community
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
import javax.ws.rs.QueryParam;
import org.ifeth.sehr.intrasec.entities.DefOptions;
import org.ifeth.sehr.p1507291734.ejb.ModuleAdmin;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/**
 * REST Web Service
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Path("module")
@RequestScoped
public class ModuleResource {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @Context
  private UriInfo context;
  @Context
  private ServletContext sctx;
  @EJB
  private ModuleAdmin ejbModuleAdmin;

  /**
   * Creates a new instance of ModuleResource
   */
  public ModuleResource() {
  }

  /**
   * List basic SEHR categories (system wide).
   *
   * @return List of DefOptions
   */
  @GET
  @Path("listSysCat")
  @Produces("application/json")
  public List<DefOptions> listSEHRSysCatOptions() {
    return ejbModuleAdmin.listSEHRSysCatOptions();
  }

  /**
   * Check if module record exists and if it has been registered.
   *
   * @param guid
   * @return
   */
  @GET
  @Path("checkGUID")
  @Produces("application/json")
  public String checkModuleGUID(@QueryParam("guid") String guid) {
    JSONObject result = new JSONObject();
    try {
      result.put("status", ejbModuleAdmin.checkModuleGUID(guid));
      result.put("success", true);
    } catch (JSONException ex) {
      Log.severe(ex.getMessage());
      return "{\"success\":\"0\"}";
    }
    return result.toString();
  }

  /**
   * PUT method for updating or creating an instance of ModuleResource.
   *
   * @param content representation for the resource
   */
  @PUT
  @Consumes("application/json")
  public void putJson(String content) {
  }
}
