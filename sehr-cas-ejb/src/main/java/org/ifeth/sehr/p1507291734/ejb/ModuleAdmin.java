/*
 * (C) 2015 IFETH
 *
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
 * Refactored from 'sehr-saf' Aug 2015
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.ifeth.sehr.intrasec.entities.DefCategory;
import org.ifeth.sehr.intrasec.entities.DefModule;
import org.ifeth.sehr.intrasec.entities.DefOptions;
import org.ifeth.sehr.intrasec.entities.NetServices;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class ModuleAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  /**
   * Returns a list with pre-defined usage groups applications can be assigned
   * to.
   *
   * <p>
   * By conventions here are at least '0=EHR Procedures', '1=Infoservices',
   * '2=Mail-/Messageservice'.<br>
   * Use this list to build as a module developer a global menu or desktop
   * widgets.<br>
   * N.B. <b>LifeCARD(R)</b> is assigned to '0=EHR Procedures'.
   * </p>
   *
   * @return
   */
  public List<DefOptions> listSEHRSysCatOptions() {
    Log.info(ModuleAdmin.class.getName() + ":listSEHRSysCatOptions()");
    List<DefOptions> list = new ArrayList<>();
    Query query = em.createQuery("select d from DefOptions d where mastertyp = 'sehrsyscat'", DefOptions.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":listSEHRSysCatOptions():" + pe.getMessage());
    }
    return list;
  }

  /**
   * Returns a list with pre-defined EHR categories modules MUST be assigned to.
   *
   * <p>
   * By conventions here are currently '0=n/a', '1=Laboratory', '2=Managed
   * Care', 3='Pharmaceutical Care','4=Studies', '5=EHR managed by
   * Patients/LifeCARD'.<br>
   * <b>N.B. The category ID's are subject to be used as constants. The purpose
   * of the table is to allow a language adoption (just for convenience).</b>
   * </p>
   *
   * @return
   */
  public List<DefCategory> listEHRCategories() {
    Log.info(ModuleAdmin.class.getName() + ":listEHRCategories()");
    List<DefCategory> list = new ArrayList<>();
    Query query = em.createQuery("select d from DefCategory d", DefCategory.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":listEHRCategories():" + pe.getMessage());
    }
    return list;
  }

  public DefCategory readEHRCategoryById(short catid) {
    Log.info(ModuleAdmin.class.getName() + ":readEHRCategoryById()");
    DefCategory entity = null;
    Query query = em.createQuery("select d from DefCategory d where d.catid=" + catid, DefCategory.class);
    try {
      entity = (DefCategory) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":listEHRCategories():" + pe.getMessage());
    }
    return entity;
  }

  /**
   * Check the GUID of a module.
   * <p>
   * Each module must have a GUID by convention. A developer of a SEHR service
   * requests a GUID from a SEHR provider or IFETH for global apps.<br>
   * The developer of a service app must use the GUID in the header of a SEHR
   * data object (or message) sending a messaging or socket based EHR
   * transmission. The developer of a receiver app should implement this method
   * to check received messages using an async message or WebService/REST based
   * procedure if the given GUID exists.<br>
   * N.N.: It is not a in depth security procedure (like a certificate) but a
   * small check in trusted infrastrucures.
   * </p>
   *
   * @param guid
   * @return
   */
  public boolean checkModuleGUID(String guid) {
    Log.info(ModuleAdmin.class.getName() + ":checkModuleGUID(...)");
    Query query = em.createQuery("select d from DefModule d where d.guid=:guid", DefModule.class);
    query.setParameter("guid", guid);
    DefModule defModule = null;
    try {
      defModule = (DefModule) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":checkModuleGUID("+guid+"):" + pe.getMessage());
    }
    //the GUID should exist otherwise return 'false'
    return defModule != null;
  }

  /**
   * Get list of services where a given module by PIK is used/registered for.
   * <p>
   * Each module has to be registered as a service by convention. A center of a
   * zone can book or register such a service based on a module. Also a zone can
   * serve an app for all centers connected to it - the centerid is '0' in such
   * cases.<br>
   * For each usage (access) to the service there will be a SEHRAUTHKEY
   * generated to the center or all centers (if centerid '0') using the module
   * (app). The center has to use this authentication code on every process to
   * the app like JMS, REST, WEB. The module responsible should always create a
   * map of registered centers of a zone (or the '0' record) to verify the
   * access (or decline it).
   * </p>
   *
   * @param pik
   * @return
   */
  public List<NetServices> listModuleServicesByPik(String pik) {
    Log.info(ModuleAdmin.class.getName() + ":listModuleServicesByPik()");
    Query query = em.createQuery("select s from NetServices s where s.modid in (select m.recid from DefModule m where m.pik=" + pik + ")", NetServices.class);
    List<NetServices> services = null;
    try {
      services = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":listModuleServicesByPik():" + pe.getMessage());
    }
    return services;
  }

  /**
   * Register an application (module) by given params.
   *
   * @param name - AppName, no white spaces or CR,LF
   * @param title - the visible name in menus etc....
   * @param pik - the AppToken, required, by convention the module project id
   * @param guid - required, the GUID as identifier
   * @param xnet - public (XNET) or not
   * @param catid - category (see DEF_CATEGORY) for options
   * @return
   */
  public DefModule registerModuleByParams(String name, String title, String pik, String guid, boolean xnet, short catid) {
    //check if catid is in table DEF_CATEGORY
    if (readEHRCategoryById(catid) == null) {
      Log.warning(ModuleAdmin.class.getName() + ":registerModuleByParams():catid=" + catid + " no in list of possible categories.");
      return null;
    }

    DefModule defModule = new DefModule();
    try {
      defModule.setName(new String(name.getBytes("UTF-8"), "ISO-8859-1"));
    } catch (UnsupportedEncodingException ex) {
      Log.warning(ModuleAdmin.class.getName() + ":registerModuleByParams():" + ex.getMessage());
      return null;
    }
    defModule.setTitle(title);
    defModule.setPik(pik);
    defModule.setGuid(guid);
    defModule.setXNet(xnet);
    //a module registered this way is not an internal SEHR module by convention
    defModule.setStandard(false);
    defModule.setCatid(catid);

    em.persist(defModule);
    em.flush();
    return defModule;
  }

  /**
   * Register an application (module) by Defmodule entity.
   *
   * @param defModule
   * @return modid (recid)
   */
  public Integer registerModule(DefModule defModule) {
    if (defModule == null) {
      return -1; //indicates an unsuccessful action
    }
    try {
      em.persist(defModule);
      em.flush();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":registerModule():" + pe.getMessage());
      return -1;
    }
    return defModule.getModid();
  }

  /**
   * Insert or update DefModule entity.
   *
   * @param entity
   * @return
   */
  public DefModule saveModule(DefModule entity) {
    if (entity.getModid() == null) {
      registerModule(entity);
    } else {
      updateModule(entity);
    }
    return entity;
  }

  /**
   * Update application entry (module) by DefModule entity.
   *
   * @param defModule
   * @return modid (recid)
   */
  public DefModule updateModule(DefModule defModule) {
    if (defModule == null) {
      return null; //indicates an unsuccessful action
    }
    DefModule m = null;
    try {
      m = em.merge(defModule);
      em.flush();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":updateModule():" + pe.getMessage());
      return null;
    }
    return m;
  }

  public List<DefModule> listModules() {
    Log.finer(ModuleAdmin.class.getName() + ":listModules()");
    Query query = em.createQuery("select m from DefModule m", DefModule.class);
    List<DefModule> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":listModules():" + pe.getMessage());
    }
    return list;
  }

  public DefModule readModuleById(int modid) {
    Log.finer(ModuleAdmin.class.getName() + ":readModuleById()" + modid);
    if (modid < 0) {
      return null;
    }
    Query query = em.createNamedQuery("DefModule.findByModId", DefModule.class);
    query.setParameter("modid", modid);
    DefModule entity = null;
    try {
      entity = (DefModule) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":readModuleById():" + pe.getMessage());
    }
    return entity;
  }

  /**
   * Get DefModule entry by registration params.
   * <p>
   * A developer of an app (module) should be able to get his module entry for
   * updates or information purposes. By convention a developer submits the
   * AppToken (PIK) and retrieves a GUID from a SEHR host administrator after
   * verification and implementation (deployment) of his module.</p>
   *
   * @param pik
   * @param guid
   * @return
   */
  public DefModule readModuleByReg(String pik, String guid) {
    Log.finer(ModuleAdmin.class.getName() + ":readModuleByReg()...");
    if (StringUtils.isBlank(pik) || StringUtils.isBlank(guid)) {
      return null;
    }
    Query query = em.createQuery("select m from DefModule m where m.pik=:pik and m.guid=:guid", DefModule.class);
    query.setParameter("pik", pik);
    query.setParameter("guid", guid);
    List<DefModule> list;
    DefModule entity = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ModuleAdmin.class.getName() + ":readModuleByReg():" + pe.getMessage());
      return null;
    }
    //we are expecting only one(!) registration
    if (list != null && !list.isEmpty() && list.size() == 1) {
      entity = list.get(0);
    } else {
      Log.warning(ModuleAdmin.class.getName() + ":readModuleByReg():Invalid entries found");
    }
    return entity;
  }
}
