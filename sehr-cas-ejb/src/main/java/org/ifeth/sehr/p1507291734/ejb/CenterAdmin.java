/*
 * (C)MDI GmbH
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
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
import org.ifeth.sehr.intrasec.entities.AdrMain;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.intrasec.entities.NetCenterPK;
import org.ifeth.sehr.p1507291734.lib.Constants;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class CenterAdmin {

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
      Log.severe(CenterAdmin.class.getName() + ":init():" + ne.toString());
      return false;
    }
    return true;
  }

  /**
   * Read NetCenter entity by 'centerid' and 'zoneid'.
   *
   * @param cid
   * @param zid
   * @return
   */
  public NetCenter readNetCenterByID(int cid, int zid) {
    Log.fine(CenterAdmin.class.getName() + ":readNetCenterByID()");
    Query query = em.createQuery("Select c from NetCenter c where c.netCenterPK.centerid=:cid AND c.netCenterPK.zoneid=:zid", NetCenter.class);
    query.setParameter("cid", cid);
    query.setParameter("zid", zid);

    NetCenter nc = null;
    try {
      nc = (NetCenter) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(CenterAdmin.class.getName() + ":readNetCenterByID():" + pe.getMessage());
    }
    return nc;
  }

  /**
   * Read NetCenter entity by 'centerid'.
   *
   * @deprecated - same cid may exist at another zone!
   * @param cid
   * @return
   */
  public NetCenter readNetCenterByID(int cid) {
    Log.fine(CenterAdmin.class.getName() + ":readNetCenterByID()");
    Query query = em.createQuery("Select c from NetCenter c where c.netCenterPK.centerid=" + cid, NetCenter.class);
    NetCenter nc = null;
    try {
      nc = (NetCenter) query.getSingleResult();
    } catch (PersistenceException pe) {
      Log.warning(CenterAdmin.class.getName() + ":readNetCenterByID():" + pe.getMessage());
    }
    return nc;
  }

  public List<NetCenter> listCentersByZoneId(int zid) {
    Log.fine(CenterAdmin.class.getName() + ":listCentersByZoneId()");
    Query query = em.createQuery("Select c from NetCenter c where c.netCenterPK.zoneid=" + zid, NetCenter.class);
    List<NetCenter> list = null;
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(CenterAdmin.class.getName() + ":listCentersByZoneId():" + pe.getMessage());
    }
    return list;
  }

  public List<NetCenter> listCenters() {
    Log.fine(CenterAdmin.class.getName() + ":listCenters()");
    Query query = em.createQuery("Select c from NetCenter c", NetCenter.class);
    List<NetCenter> list = null;
    try {
      list = query.getResultList();
      Log.info(CenterAdmin.class.getName() + ":listCenters():found entries:" + (list != null ? list.size() : "null"));
    } catch (PersistenceException pe) {
      Log.warning(CenterAdmin.class.getName() + ":listCenters():" + pe.getMessage());
    }
    return list;
  }

  public List<NetCenter> listCentersByName(String filter) {
    Log.fine(CenterAdmin.class.getName() + ":listCentersByName():filter=" + filter);
    if (!init()) {
      return null;
    }
    List<NetCenter> list = null;
    Query query = em.createQuery("SELECT n FROM NetCenter n WHERE n.name like :name", NetCenter.class);
    query.setParameter("name", "%" + filter + "%");

    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(CenterAdmin.class.getName() + ":listCentersByName():" + pe.getMessage());
    }
    return list;
  }

  public List<NetCenter> listCentersByParams(Map<String, String> params) {
    Log.fine(CenterAdmin.class.getName() + ":listCentersByParams():params " + (params != null ? params.size() : "none"));
    if (!init()) {
      return null;
    }
    List<NetCenter> list = null;
    CharSequence csql;
    String sql = "SELECT n.* FROM net_center n ";
    if (params != null) {
      sql += "WHERE ";
      for (Map.Entry<String, String> entry : params.entrySet()) {
        switch (entry.getKey()) {
          case "name":
            sql += "n.name='" + entry.getValue() + "' ";
            break;
          case "zoneid":
            sql += "n.zoneid=" + entry.getValue() + " ";
            break;
        }
        sql += "AND ";
        //Log.info(CenterAdmin.class.getName() + ":listCentersByParams():sql=" + sql);
      }
      sql = sql.substring(0, sql.length() - 4);
    }
    Log.finer(CenterAdmin.class.getName() + ":listCentersByParams():sql=" + sql);

    //Query query = em.createQuery("SELECT n FROM NetCenter n WHERE n.name like :name", NetCenter.class);
    //query.setParameter("name", "%" + filter + "%");
    Query query = em.createNativeQuery(sql, NetCenter.class);
    try {
      list = query.getResultList();
    } catch (PersistenceException pe) {
      Log.warning(CenterAdmin.class.getName() + ":listCentersByName():" + pe.getMessage());
    }
    return list;
  }

  public X509Certificate getX509Certificate(int cid) {
    Log.fine(CenterAdmin.class.getName() + ":getX509Certificate()");
    //TODO to check center against keystore
    return null;
  }

  public NetCenterPK saveNetCenter(NetCenter netCenter) {
    Log.fine(CenterAdmin.class.getName() + ":saveNetCenter():" + netCenter.toString());
    if (netCenter.getNetCenterPK() == null) {
      Log.warning(CenterAdmin.class.getName() + ":saveNetCenter():No primary key (CID,ZID)");
      return null;
    }
    if (em.find(NetCenter.class, netCenter.getNetCenterPK()) == null) {
      em.persist(netCenter);
    } else {
      netCenter = em.merge(netCenter);
    }
    return netCenter.getNetCenterPK();
  }

  public Integer saveAdrMain(AdrMain adrMain) {
    if (adrMain.getAdrid() == null || em.find(AdrMain.class, adrMain.getAdrid()) == null) {
      em.persist(adrMain);
    } else {
      adrMain = em.merge(adrMain);
    }
    return adrMain.getAdrid();
  }
}
