/*
 * (C) 2015 MDI GmbH
 *
 * Based on an idea of Hans J Haase
 * Development Team: Hans J.
 */
package org.ifeth.sehr.p1507291734.web.control;


import java.io.Serializable;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.util.UidGenerator;

/**
 * Schedule control of mobile pages to add, edit events.
 *
 * @author HansJ
 */
@Named(value = "mobSchedCtrl")
@SessionScoped
public class MobileScheduleControl implements Serializable {

  private static final long serialVersionUID = 1L;
 private static final Logger Log = Logger.getLogger("org.ifeth.p1505040435.web");
 
  private String username;
  private VEvent vevent;
  private List<VEvent> lstVEvents;

  //============================================= constructors, initialization
  public MobileScheduleControl() {
  }

  @PostConstruct
  public void init() {
    vevent = new VEvent();
    lstVEvents = new ArrayList<>();
  }

  //============================================= getter/setter
  public VEvent getEvent() {
    return vevent;
  }

  public void setEvent(VEvent vevent) {
    this.vevent = vevent;
  }

  public List<VEvent> getListEvents() {
    if (lstVEvents == null || lstVEvents.isEmpty()) {
      //lstVEvents = new ArrayList<>(); //done by init()...
      lstVEvents.add(createEPICLunchEvent());
    }
    return lstVEvents;
  }

  //============================================= methods of actions etc.
  public String doSaveEvent() {
    if (!lstVEvents.contains(vevent)) {
      lstVEvents.add(vevent);
    }

    return "pm:list?transition=flip";
  }

  public void doDeleteEvent() {
    if (lstVEvents.contains(vevent)) {
      lstVEvents.remove(vevent);
    }
  }

  public String doPrepareNewEvent() {
    vevent = new VEvent();

    return "pm:edit?transition=flip";
  }

  public String showEvent() {
    Log.info(vevent.toString());
    return "schedShowEvent?transition=flip";
  }

  private VEvent createEPICLunchEvent() {
    // Create a TimeZone
    TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
    TimeZone timezone = registry.getTimeZone("Europe/Berlin");
    VTimeZone tz = timezone.getVTimeZone();

    // Start Date 
    java.util.Calendar startDate = new GregorianCalendar();
    startDate.setTimeZone(timezone);
    startDate.set(java.util.Calendar.MONTH, java.util.Calendar.MAY);
    startDate.set(java.util.Calendar.DAY_OF_MONTH, 31);
    startDate.set(java.util.Calendar.YEAR, 2015);
    startDate.set(java.util.Calendar.HOUR_OF_DAY, 9);
    startDate.set(java.util.Calendar.MINUTE, 0);
    startDate.set(java.util.Calendar.SECOND, 0);

    // End Date  
    java.util.Calendar endDate = new GregorianCalendar();
    endDate.setTimeZone(timezone);
    endDate.set(java.util.Calendar.MONTH, java.util.Calendar.MAY);
    endDate.set(java.util.Calendar.DAY_OF_MONTH, 31);
    endDate.set(java.util.Calendar.YEAR, 2015);
    endDate.set(java.util.Calendar.HOUR_OF_DAY, 18);
    endDate.set(java.util.Calendar.MINUTE, 0);
    endDate.set(java.util.Calendar.SECOND, 0);

// Create the event
    String eventName = "Lunch EPIC";
    DateTime start = new DateTime(startDate.getTime());
    DateTime end = new DateTime(endDate.getTime());
    VEvent meeting = new VEvent(start, end, eventName);

// add timezone info..
    meeting.getProperties().add(tz.getTimeZoneId());

// generate unique identifier..
    UidGenerator ug;
    try {
      ug = new UidGenerator("1");
      meeting.getProperties().add(ug.generateUid());
    } catch (SocketException ex) {
      Logger.getLogger(MobileScheduleControl.class.getName()).log(Level.SEVERE, null, ex);
    }

// add attendees..
    Attendee dev1 = new Attendee(URI.create("mailto:thilo@mycompany.com"));
    dev1.getParameters().add(Role.REQ_PARTICIPANT);
    dev1.getParameters().add(new Cn("The father"));
    meeting.getProperties().add(dev1);

    Attendee dev2 = new Attendee(URI.create("mailto:petzi@mycompany.com"));
    dev2.getParameters().add(Role.OPT_PARTICIPANT);
    dev2.getParameters().add(new Cn("The Advisor"));
    meeting.getProperties().add(dev2);

// Create ICS calendar to print/import at mobile
//    net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
//    icsCalendar.getProperties().add(new ProdId("-//Events Calendar//EPIC 0.1//EN"));
//    icsCalendar.getProperties().add(CalScale.GREGORIAN);
//    icsCalendar.getComponents().add(meeting);
//    System.out.println(icsCalendar);
    return meeting;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }
}
