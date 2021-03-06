/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class Constants {
  //00000001, zone (SEHR CAS) WEB is avlb. by port 8080
  public static final int maskIsURLSEHRWeb = 1;   
  //00000010, is zone in LDAP?
  public static final int maskExistSEHRDirCtx = 2;
  //00000100, is secured by 8181?
  public static final int maskIsURLSEHRWebSSL = 3;
}
