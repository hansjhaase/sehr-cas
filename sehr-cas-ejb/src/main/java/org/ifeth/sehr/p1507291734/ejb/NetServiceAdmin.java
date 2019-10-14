/*
 * (C)MDI GmbH
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetServices;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class NetServiceAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  /**
   * Returns a list of services (apps, modules) registered by a zone that can be
   * used by centers.
   *
   * @param zid
   * @return
   */
  public List<NetServices> listNetServicesByZID(int zid) {
    Log.fine(NetServiceAdmin.class.getName() + ":listNetServicesByZID()");
    List<NetServices> list = new ArrayList<>();
    String sql = "Select s from NetServices s where s.centerid=0 ";
    if (zid >= 0) {
      sql += " and s.zoneid=" + zid;
    }
    Log.finer(NetServiceAdmin.class.getName() + ":listNetServicesByZID():sql=" + sql);
    Query query = em.createQuery(sql, NetServices.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(NetServiceAdmin.class.getName() + ":listNetServicesByZID():" + pe.getMessage());
    }
    return list;
  }

  /**
   * Returns a list of services (apps, modules) registered for a zone and a
   * center by given params.
   *
   * @param zid
   * @param cid
   * @return
   */
  public List<NetServices> listNetServicesByZIDCID(int zid, int cid) {
    Log.fine(NetServiceAdmin.class.getName() + ":listNetServicesByZIDCID()");
    List<NetServices> list = new ArrayList<>();
    String sql = "Select s from NetServices s";
    if (zid >= 0) {
      sql += " where s.zoneid=" + zid;
    }
    if (cid >= 0) {
      sql += " and s.centerid=" + cid;
    }
    Log.finer(NetServiceAdmin.class.getName() + ":listNetServicesByZIDCID():sql=" + sql);
    Query query = em.createQuery(sql, NetServices.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(NetServiceAdmin.class.getName() + ":listNetServicesByZIDCID():" + pe.getMessage());
    }
    return list;
  }

  /**
   * Get a list of services of a given app (module).
   * <p>
   * A module can be used at different zones and center hosts.
   * </p>
   *
   * @param modid
   * @return
   */
  public List<NetServices> listNetServicesByModule(int modid) {
    Log.finer(NetServiceAdmin.class.getName() + ":listNetServicesByModule()");
    List<NetServices> list = new ArrayList<>();
    Query query = em.createQuery("Select s from NetServices s where s.modid=" + modid, NetServices.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(NetServiceAdmin.class.getName() + ":listNetServicesByModule():" + pe.getMessage());
    }
    return list;
  }

  /**
   * Get a list of all registered services at this DB (host).
   *
   * @return
   */
  public List<NetServices> listNetServices() {
    Log.finer(NetServiceAdmin.class.getName() + ":listNetServices()");
    List<NetServices> list = new ArrayList<>();
    Query query = em.createNativeQuery("Select s.* from Net_Services s order by s.modid, s.zoneid, s.centerid ", NetServices.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(NetServiceAdmin.class.getName() + ":listNetServices():" + pe.getMessage());
    }
    return list;
  }

  /**
   * Save or update NetServices object.
   *
   * @param netServices
   * @return
   */
  public NetServices saveNetServices(NetServices netServices) {
    Log.finer(NetServiceAdmin.class.getName() + ":saveNetServices():" + netServices);
    if (netServices.getSvid() == null) {
      em.persist(netServices);
    } else {
      netServices = em.merge(netServices);
    }
    return netServices;
  }
}
