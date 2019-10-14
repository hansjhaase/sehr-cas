/*
 * (C) IFETH Development Team
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import org.ifeth.sehr.intrasec.entities.NetZones;
import org.ifeth.sehr.p1507291734.ejb.ZoneAdmin;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@FacesConverter("NetZonesConverter")
public class NetZonesConverter implements Converter {
  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");
 
//	@ManagedProperty("#{dsZones}")
//	private ZonesDataSource ds;
//
//	public ZonesDataSource getDs() {
//		return ds;
//	}
//
//	public void setDs(ZonesDataSource ds) {
//		this.ds = ds;
//	}

  @EJB
  private ZoneAdmin ejbZoneAdmin;

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) {
    Log.info(NetZonesConverter.class.getName() + ":getAsObject():UIComponent=" + component.getId());
    if (value != null && value.trim().length() > 0) {
      String[] t = value.split("\\|");
      try {
        int zid = Integer.parseInt(t[0]);
        //int zid = Integer.valueOf(value);
        if (zid >= 0) {
          NetZones z = ejbZoneAdmin.readNetZonesByID(zid);
          return z;
        }
      } catch (NumberFormatException nfe) {
        //throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Not a valid Zone."));
      }
    }
    return null;
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) {
    Log.log(Level.FINEST, "{0}:getAsString():UIComponent={1}", new Object[]{NetZonesConverter.class.getName(), component.getId()});
    //somDrugProduct
    if (value != null) {
      if (value instanceof NetZones) {
        NetZones z = (NetZones) value;
        return z.getZoneid() + "|" + z.getTitle();
        //return z.getTitle(); //the name of the zone
      }
    }
    return null;
  }
}
