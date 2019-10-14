/*
 * (C) MDI for the IFETH community
 */
package org.ifeth.sehr.p1507291734.web.ws;

import java.util.List;
import javax.ejb.EJB;
import javax.jws.WebService;
import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.ifeth.sehr.intrasec.entities.DefCategory;
import org.ifeth.sehr.intrasec.entities.DefModule;
import org.ifeth.sehr.intrasec.entities.DefOptions;
import org.ifeth.sehr.intrasec.entities.NetServices;
import org.ifeth.sehr.p1507291734.ejb.ModuleAdmin;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@WebService(serviceName = "ModuleWS")
//@Stateless()
public class ModuleWS {
  @EJB
  private ModuleAdmin ejbRef;
  

  @WebMethod(operationName = "listSEHRSysCatOptions")
  public List<DefOptions> listSEHRSysCatOptions() {
    return ejbRef.listSEHRSysCatOptions();
  }

  @WebMethod(operationName = "listEHRCategories")
  public List<DefCategory> listEHRCategories() {
    return ejbRef.listEHRCategories();
  }

  @WebMethod(operationName = "readEHRCategoryById")
  public DefCategory readEHRCategoryById(@WebParam(name = "catid") short catid) {
    return ejbRef.readEHRCategoryById(catid);
  }

  @WebMethod(operationName = "checkModuleGUID")
  public boolean checkModuleGUID(@WebParam(name = "guid") String guid) {
    return ejbRef.checkModuleGUID(guid);
  }

  @WebMethod(operationName = "listModuleServicesByPik")
  public List<NetServices> listModuleServicesByPik(@WebParam(name = "pik") String pik) {
    return ejbRef.listModuleServicesByPik(pik);
  }

  @WebMethod(operationName = "registerModuleByParams")
  public DefModule registerModuleByParams(@WebParam(name = "name") String name, @WebParam(name = "title") String title, @WebParam(name = "pik") String pik, @WebParam(name = "guid") String guid, @WebParam(name = "xnet") boolean xnet, @WebParam(name = "catid") short catid) {
    return ejbRef.registerModuleByParams(name, title, pik, guid, xnet, catid);
  }

  @WebMethod(operationName = "registerModule")
  public Integer registerModule(@WebParam(name = "defModule") DefModule defModule) {
    return ejbRef.registerModule(defModule);
  }

//  @WebMethod(operationName = "saveModule")
//  public DefModule saveModule(@WebParam(name = "entity") DefModule entity) {
//    return ejbRef.saveModule(entity);
//  }

//  @WebMethod(operationName = "updateModule")
//  public DefModule updateModule(@WebParam(name = "defModule") DefModule defModule) {
//    return ejbRef.updateModule(defModule);
//  }

//  @WebMethod(operationName = "listModules")
//  public List<DefModule> listModules() {
//    return ejbRef.listModules();
//  }

  @WebMethod(operationName = "readModuleById")
  public DefModule readModuleById(@WebParam(name = "modid") int modid) {
    return ejbRef.readModuleById(modid);
  }

  @WebMethod(operationName = "readModuleByReg")
  public DefModule readModuleByReg(@WebParam(name = "pik") String pik, @WebParam(name = "guid") String guid) {
    return ejbRef.readModuleByReg(pik, guid);
  }
  
}
