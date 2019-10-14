/*
 * (C)2016 MDI; developed for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.test;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

/**
 * Simple example and testing of REST access 'lifecard'.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class LifeCARDClient {

  public static void main(String[] args) {
    ClientConfig config = new ClientConfig();
    Client client = ClientBuilder.newClient(config);

    WebTarget target = client.target(getBaseURI());
    // Get XML
    String xmlResponse = target.path("rest").path("lcis").request()
            .accept(MediaType.TEXT_XML).get(String.class);
    System.out.println("=== xmlResponse ===");
    System.out.println(xmlResponse);
    // Get XML for application
    String xmlAppResponse = target.path("rest").path("lcis").request()
            .accept(MediaType.APPLICATION_XML).get(String.class);
    System.out.println("=== xmlAppResponse ===");
    System.out.println(xmlAppResponse);
    // Get XML
    String xmlResponseLcMain = target.path("rest").path("lifecard").path("list").path("DE").request()
            .accept(MediaType.TEXT_XML).get(String.class);
    System.out.println("=== xmlAppResponse 'xmlResponseLcMain' ===");
    System.out.println(xmlResponseLcMain);
    // For JSON response also add the Jackson libraries to your webapplication
    // In this case you would also change the client registration to
    // ClientConfig config = new ClientConfig().register(JacksonFeature.class);
    // Get JSON for application
    // System.out.println(target.path("rest").path("lifecard").path("list").path("DE").request()
    // .accept(MediaType.APPLICATION_JSON).get(String.class));

  }

  private static URI getBaseURI() {
    return UriBuilder.fromUri("http://localhost:8080/sehr-cas-web").build();
  }
}
