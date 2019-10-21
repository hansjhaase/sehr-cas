/*
 * (C) MDI GmbH
 */
package org.ifeth.sehr.p1507291734.web;

import java.util.Properties;
import javax.jms.Connection;
import javax.servlet.ServletContext;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
public class MessagingManagerTest {

  private MessagingManager msgMessenger;
  private Properties pMessenger;

  public MessagingManagerTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
    System.out.println("@Before:setUp()");
    ServletContext ctx = null;
    msgMessenger = MessagingManager.getInstance(ctx);
    pMessenger = new Properties();
    pMessenger.setProperty("sehrxnetzoneurl", "failover:(tcp://127.0.0.1:61616)?timeout=3000");
    pMessenger.setProperty("sehrxnetzoneuser", "sehruser");
    pMessenger.setProperty("sehrxnetzonepw", "user4sehr");
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of configure method, of class MessagingManager.
   */
  @Test
  public void test1Configure() {
    System.out.println("configure");
    boolean expResult = true;
    boolean result = msgMessenger.configure(pMessenger);
    if(result==false){
      System.out.println("For developing and testing purposes start activemq!");
    }
    assertEquals(expResult, result);
  }

  /**
   * Test of getConnection method, of class MessagingManager.
   */
  @Test
  public void test2GetConnection() {
    System.out.println("getConnection");
    Connection jmsCon = msgMessenger.getConnection();
    //assertNotNull(jmsCon);
  }

  /**
   * Test of getAMQConnectionFactory method, of class MessagingManager.
   */
  @Test
  public void test3GetAMQConnectionFactory() {
    System.out.println("getAMQConnectionFactory");
    ActiveMQConnectionFactory result = msgMessenger.getAMQConnectionFactory();
    //assertNotNull(result);
  }

  /**
   * Test of isConnected method, of class MessagingManager.
   */
  @Test
  public void test5IsConnected() {
    System.out.println("isConnected");
    boolean result = msgMessenger.isConnected();
    assertEquals(true, result);
  }

  /**
   * Test of close method, of class MessagingManager.
   */
  @Test
  public void test6Close() {
    System.out.println("close");
    msgMessenger.close();
  }

}
