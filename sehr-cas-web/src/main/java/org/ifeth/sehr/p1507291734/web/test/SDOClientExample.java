/*
 * (C)2016 MDI; developed for the SEHR Community
 */
package org.ifeth.sehr.p1507291734.web.test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
//import org.glassfish.jersey.client.JerseyClient;
//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;
import org.glassfish.jersey.client.ClientConfig;
import org.ifeth.sehr.core.exception.ObjectHandlerException;
import org.ifeth.sehr.core.lib.DeSerializer;
import org.ifeth.sehr.core.objects.SEHRDataObject;
import org.ifeth.sehr.p1507291734.web.model.SEHRDataObjectTxHandler;
import static org.ifeth.sehr.p1507291734.web.model.SEHRDataObjectTxHandler.nextValue;

/**
 * Simple example and testing of REST access 'lifecard'.
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class SDOClientExample {

  public static void main(String[] args) {
    ClientConfig config = new ClientConfig();
    Client client = ClientBuilder.newClient(config);
    WebTarget target = client.target(getBaseURI());
    //--- 
    String xmlSEHRDataObject = target.path("rest").path("sdotx").path("get").path("0").request()
            .accept(MediaType.APPLICATION_XML).get(String.class);
    System.out.println("=== xmlAppResponse ===");
    System.out.println(xmlSEHRDataObject);
    //--- Get list of messages of a queue
    //String xmlResponseLcMain = target.path("rest").path("tx").path("list").path("queue").request()
    //        .accept(MediaType.TEXT_XML).get(String.class);
    //System.out.println("=== xmlAppResponse '...' ===");
    //System.out.println(xmlQueueList);
    // For JSON response also add the Jackson libraries to your webapplication
    // In this case you would also change the client registration to
    // ClientConfig config = new ClientConfig().register(JacksonFeature.class);
    // Get JSON for application
    // System.out.println(target.path("rest").path("tx").path("list").path("queue").request()
    // .accept(MediaType.APPLICATION_JSON).get(String.class));
    //WebResource webResource = client.
    //*** dummy entry for testing purposes ***
    SEHRDataObject sdo = new SEHRDataObject();
    Integer id = nextValue();
    sdo.setObjID(id);

    Map<String, Object> msgProperties = new HashMap<>();
    msgProperties.put("MsgType", "orderentry");
    msgProperties.put("ordertype", "pt");
    msgProperties.put("dataset", "Entity#Field");
    msgProperties.put("JMSType", "NONSEHR#JMED");
    msgProperties.put("origCenterId", 0601311);
    //we should not use this... Camel rewrites it
    msgProperties.put("JMSReplyTo", "sehr.9999998.0601311.queue");
    //to be discussed: rplyDestType
    //to be discussed: rplyDest
    //instead of 
    msgProperties.put("sehrReplyTo", "queue://sehr.9999998.0601311.queue");

    msgProperties.put("rcvCenterId", 99999980);
    //to be discussed: rcvDestType
    msgProperties.put("rcvDestType", "queue");
    msgProperties.put("rcvDest", "sehr.9999999.9999980.queue");
    //or msgProperties.put("rcvDest", "queue://sehr.9999999.0306010.queue");
    //msgProperties.put("rcvDest", "topic://sehr.9999999.0306010.queue");
    sdo.setSDOProperties(msgProperties);
    Map data = new HashMap();
    data.put("NoteXml#mndid", -1);
    //byte[] b = "I'm a JMed NOTE_XML or VDT data set".getBytes();
    //JMSS4C uses Entity#Field data set; this is based on a HashMap, 
    //so we must convert them to a byte stream
    //otherwise JaxB throws an error...
    //see also
    //https://www.evernote.com/Home.action#n=ae013491-35a9-47af-93d8-b931a2b6b64f&ses=4&sh=2&sds=5&
    if (data instanceof Map) {
      byte[] b;
      try {
        b = DeSerializer.serialize(data);
        sdo.setDataobject(b);
      } catch (ObjectHandlerException ex) {
        Logger.getLogger(SEHRDataObjectTxHandler.class.getName()).log(Level.SEVERE, null, ex);
      }
    } else {
      sdo.setDataobject(data);
    }
    //*** end of testing object ***
//    File f = new File(File.separator + "tmp" + File.separator + "Test.xml");
//    try {
//      SEHRObjectHandler.ObjectToXMLFile(sdo, f);
//      String input = Files.toString(f, null);
//    } catch (GenericSEHRException | IOException ex) {
//      Logger.getLogger(SDOClientExample.class.getName()).log(Level.SEVERE, null, ex);
//    }

    Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_XML);
    Response response = invocationBuilder.post(Entity.entity(sdo, MediaType.APPLICATION_XML));

    System.out.println(response.getStatus());
    System.out.println(response.readEntity(String.class));

  }

  private static URI getBaseURI() {
    return UriBuilder.fromUri("http://localhost:8080/sehr-cas-web").build();
  }
}
