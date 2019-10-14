/*
 * (C) MDI GmbH for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.rest;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@javax.ws.rs.ApplicationPath("webresources")
public class ApplicationConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> resources = new java.util.HashSet<>();
    addRestResourceClasses(resources);
    return resources;
  }

  /**
   * Do not modify addRestResourceClasses() method.
   * It is automatically populated with
   * all resources defined in the project.
   * If required, comment out calling this method in getClasses().
   */
  private void addRestResourceClasses(Set<Class<?>> resources) {
    resources.add(org.ifeth.sehr.p1507291734.web.rest.LcMainResource.class);
    resources.add(org.ifeth.sehr.p1507291734.web.rest.LifeCARDItemResource.class);
    resources.add(org.ifeth.sehr.p1507291734.web.rest.LifeCARDItemsResource.class);
    resources.add(org.ifeth.sehr.p1507291734.web.rest.ModuleResource.class);
    resources.add(org.ifeth.sehr.p1507291734.web.rest.SDOTxResource.class);
    resources.add(org.ifeth.sehr.p1507291734.web.rest.SEHRConfiguration.class);
  }
  
}
