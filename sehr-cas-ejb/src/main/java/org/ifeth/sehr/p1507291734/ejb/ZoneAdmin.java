/*
 * (C)MDI GmbH
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.util.Date;
import org.ifeth.sehr.p1507291734.lib.Constants;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetZones;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class ZoneAdmin {

  private static final Logger Log = Logger.getLogger("org.ifeth.p1505040435.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  private Properties p;

  private boolean init() {
    InitialContext ic;
    try {
      ic = new InitialContext();
      p = (Properties) ic.lookup(Constants.ICPropName);
    } catch (NamingException ne) {
      Log.severe(ZoneAdmin.class.getName() + ":init():" + ne.toString());
      return false;
    }
    return true;
  }

  public List<NetZones> listActiveZones() {
    Log.info(ZoneAdmin.class.getName() + ":listActiveZones()");
    Query query = em.createQuery("Select n from NetZones n where n.endofservice is null");
    //query.setParameter( "param", "%abc%" );
    List<NetZones> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ZoneAdmin.class.getName() + ":listActiveZones():" + pe.getMessage());
    }
    return list;
  }

  /**
   * Returns a list of active centers (enddt is null) of a given zone by ID.
   *
   * @param zid
   * @return
   */
  public List<NetCenter> listActiveCenters(int zid) {
    Log.info(ZoneAdmin.class.getName() + ":listActiveCenters()");
    Query query = em.createQuery("Select c from NetCenter c where c.netCenterPK.zoneid=" + zid + " and c.enddt is null",NetCenter.class);
    //query.setParameter( "param", "%abc%" );
    List<NetCenter> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ZoneAdmin.class.getName() + ":listActiveCenters():" + pe.getMessage());
    }
    return list;
  }

  public int countCentersOfZone(int zoneid, boolean isActive) {
    String sql = "select count(*) as n from net_center where zoneid=" + zoneid;
    if (isActive) {
      sql += " and enddt is null";
    }
    int c = 0;
    Query query = em.createNativeQuery(sql);
    try {
      c = (Integer) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(ZoneAdmin.class.getName() + ":countCentersOfZone():" + pe.getMessage());
    }
    return c;
  }

  /**
   * Returns a NetZones entity of the local zone this host is configured for.
   *
   * @return
   */
  public NetZones readNetZonesByConfiguration() {
    if (!init()) {
      return null;
    }
    int zid = Integer.valueOf(p.getProperty("zoneID", "0"));
    return (zid <= 0 ? null : readNetZonesByID(zid));
  }

  /**
   * Returns a NetZones entity of a given zone by ID.
   *
   * @param zid
   * @return
   */
  public NetZones readNetZonesByID(Integer zid) {
    Log.info(ZoneAdmin.class.getName() + ":readNetZonesByID()");
    if (!init()) {
      return null;
    }
    NetZones netZones = null;
    Query query = em.createNamedQuery("NetZones.findByZoneid", NetZones.class);
    query.setParameter("zoneid", zid);

    try {
      netZones = (NetZones) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(ZoneAdmin.class.getName() + ":readNetZonesByID():" + pe.getMessage());
    }
    return netZones;
  }

  public List<NetZones> listZonesByTitle(String filter) {
    Log.info(ZoneAdmin.class.getName() + ":listZonesByTitle():filter=" + filter);
    if (!init()) {
      return null;
    }
    List<NetZones> list = null;
    Query query = em.createQuery("SELECT n FROM NetZones n WHERE n.title like :title", NetZones.class);
    query.setParameter("title", "%" + filter + "%");

    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(ZoneAdmin.class.getName() + ":listZonesByTitle():" + pe.getMessage());
    }
    return list;
  }

  public boolean save(NetZones netZones) {
    if (netZones == null) {
      return false;
    }
    try {
      //netZones.setChanged(new Date());
      if (em.find(netZones.getClass(), netZones.getZoneid()) != null) {
        Log.info(ZoneAdmin.class.getName() + ":save():Updating " + netZones.toString());
        em.merge(netZones);
      } else {
        Log.info(ZoneAdmin.class.getName() + ":save():Creating " + netZones.toString());
        em.persist(netZones);
      }
    } catch (PersistenceException pe) {
      Log.warning(ZoneAdmin.class.getName() + ":save():" + pe.getMessage());
      return false;
    }
    return true;
  }
}
