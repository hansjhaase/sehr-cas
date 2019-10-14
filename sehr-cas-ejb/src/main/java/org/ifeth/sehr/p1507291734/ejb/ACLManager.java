/*
 * (C) IFETH
 *
 * Refactored from 'sehr-core-JPA' JSE to use component by JTA managed JPA 
 * entities.
 */
package org.ifeth.sehr.p1507291734.ejb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.ifeth.sehr.core.objects.AclPermissionObject;
import org.ifeth.sehr.intrasec.entities.AclRolePerms;
import org.ifeth.sehr.intrasec.entities.AclUserPerms;
import org.ifeth.sehr.intrasec.entities.AclUserRoles;

/**
 * ACL component to manage users and their access to sEHR using the ACL tables.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@Stateless
@LocalBean
public class ACLManager {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.ejb");

  @PersistenceContext(unitName = "HibJTAIntraSEC")
  private EntityManager em;

  public Map getUserACL(int usrid) {
    //TODO check if user exist in UsrMain
    //check first if user has roles (at least there should be guest)
    //this.roles = getUserRoles("id_only");
    //2nd buld acl based on role and individual perms
    //return it
    return buildUserACL(usrid);
  }

  private Map buildUserACL(int usrid) {
    //first, get the rules for the user's roles
    Map acl = new HashMap();
    Map userRoles = getUserRoles(usrid, "id_only");
    if (userRoles != null && !userRoles.isEmpty()) {
      Map map = getRolePerms(userRoles);
      if (map != null) {
        acl.putAll(map);
      }
    }
    //then, get the individual user permissions
    Map userPerms = getUserPerms(usrid);
    if (userPerms != null) {
      acl.putAll(userPerms);
    }
    if (acl.isEmpty()) {
      Log.log(Level.INFO, ACLManager.class.getName()+":buildUserACL():No valid ACL rules for user #{0}", usrid);
      return null;
    }
    return acl;
  }

  /**
   * Get roles
   *
   * @param usrid
   * @param filter id_only or id_name
   * @return
   */
  public Map getUserRoles(int usrid, String filter) {
    String sql = "SELECT * FROM acl_user_roles WHERE usrid = " + usrid + " ORDER BY addDate ASC";
    Map userRoles = new HashMap();
    try {
      Query query = em.createNativeQuery(sql, AclUserRoles.class);
      //Query query = em.createNamedQuery("AclUserRoles.findByUsrid");//em.createNativeQuery(sql);
      //query.setParameter(1, this.usrid);
      List<AclUserRoles> l = query.getResultList();
      Log.log(Level.INFO, "getUserRoles({0}): List<AclUserRoles> size={1}", new Object[]{filter, l.size()});

      for (AclUserRoles aclUserRoles : l) {
        int roleid = aclUserRoles.getAclUserRolesPK().getRoleid();
        if (filter.equalsIgnoreCase("id_only")) {
          userRoles.put(roleid, roleid); //value: role id only
        } else {
          userRoles.put(roleid, getRoleNameFromID(roleid));
        }
      }
    } catch (PersistenceException pe) {
    }
    Log.log(Level.FINER, "getUserRoles({0}): userRoles size={1}", new Object[]{filter, userRoles.size()});
    return (userRoles.isEmpty() ? null : userRoles);
  }

  public Map getUserPerms(int usrid) {

    String sql = "SELECT * FROM acl_user_perms WHERE usrid = " + usrid + " ORDER BY addDate ASC";
    Map userPerms = new HashMap();

    Query q = em.createNativeQuery(sql, AclUserPerms.class);
    List<AclUserPerms> l = q.getResultList();
    boolean hasPerm = false;
    for (AclUserPerms aclUserPerm : l) {
      //System.out.println(aclUserPerm.toString());
      String permkey = this.getPermKeyFromID(aclUserPerm.getPermid());
      if (permkey == null) {
        continue;
      }
      if (aclUserPerm.getPerm() == 1) {
        hasPerm = true;
      } else {
        hasPerm = false;
      }

      //$perms[$pK] = array('perm' => $pK,'inheritted' => false,'value' => $hP,'Name' => $this->getPermNameFromID($row['permID']),'ID' => $row['permID']);
      AclPermissionObject aclPerm = new AclPermissionObject();
      aclPerm.setAclUserPerms(aclUserPerm);
      aclPerm.setInerited(true);
      aclPerm.setPermission(hasPerm);
      aclPerm.setPermId(aclUserPerm.getPermid());
      aclPerm.setPermKey(permkey);
      aclPerm.setPermName(getPermNameFromID(aclUserPerm.getPermid()));
      userPerms.put(permkey, aclPerm);
    }
    return (userPerms.isEmpty() ? null : userPerms);
  }

  public Map getRolePerms(Map roles) {

    if (roles == null || roles.isEmpty()) {
      Log.info("ACLManager:getRolePerms():No roles (id's) given...");
      return null;
    }

    String rolelist = "";
    Set entries = roles.entrySet();
    Iterator roleIter = entries.iterator();
    while (roleIter.hasNext()) {
      Map.Entry entry = (Map.Entry) roleIter.next();
      int key = (Integer) entry.getKey();  // Get the roleid ...
      //Object value = entry.getValue();  // Get the value.
      rolelist += key + ",";
    }

    rolelist = rolelist.substring(0, rolelist.length() - 1);

    Map perms = new HashMap();

    String sql = "SELECT * FROM acl_role_perms WHERE roleid IN (" + rolelist + ") ORDER BY id ASC";
    Log.log(Level.FINEST, "ACLManager:getRolePerms():sql={0}", sql);
    Query q = em.createNativeQuery(sql, AclRolePerms.class);
    List<AclRolePerms> l = q.getResultList();
    boolean hasPerm = false;
    for (AclRolePerms aclRolePerm : l) {
      //System.out.println(aclUserPerm.toString());
      String permkey = this.getPermKeyFromID(aclRolePerm.getPermid());
      if (permkey == null) {
        continue;
      }
      if (aclRolePerm.getPerm() == 1) {
        hasPerm = true;
      } else {
        hasPerm = false;
      }
      AclPermissionObject aclPerm = new AclPermissionObject();
      aclPerm.setAclRolePerms(aclRolePerm);
      aclPerm.setInerited(true);
      aclPerm.setPermission(hasPerm);
      aclPerm.setPermId(aclRolePerm.getPermid());
      aclPerm.setPermKey(permkey);
      aclPerm.setPermName(getPermNameFromID(aclRolePerm.getPermid()));
      perms.put(permkey, aclPerm);
    }
    return (perms.isEmpty() ? null : perms);
  }

  public String getPermKeyFromID(int permid) {

    String sql = "SELECT permkey FROM acl_permissions WHERE ID=" + permid;
    //EMF - so much less to code...
    Query query = em.createNativeQuery(sql);
    String permkey = (String) query.getSingleResult();
    return permkey;
  }

  public String getPermNameFromID(int permid) {
    String name = null;
    String sql = "SELECT permname FROM acl_permissions WHERE id=" + permid;
    try {
      Query query = em.createNativeQuery(sql);
      name = (String) query.getSingleResult();
    } catch (PersistenceException pe) {
    }
    return name;
  }

  public String getRoleNameFromID(int roleid) {
    String sql = "SELECT rolename FROM acl_roles WHERE id=" + roleid;
    String rolename = null;
    try {
      Query query = em.createNativeQuery(sql);
      rolename = (String) query.getSingleResult();
    } catch (PersistenceException pe) {
    }
    return rolename;
  }

}
