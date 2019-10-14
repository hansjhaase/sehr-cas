/*
 * (C) IFETH Development Team
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.ifeth.sehr.intrasec.entities.NetCenter;
import org.ifeth.sehr.p1507291734.ejb.CenterAdmin;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@FacesConverter("NetCenterConverter")
public class NetCenterConverter implements Converter {
  private static final Logger Log = Logger.getLogger("org.ifeth.p1507291734.web.level");
 
  @EJB
  private CenterAdmin ejbCenterAdmin;

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) {
    Log.finest(NetCenterConverter.class.getName() + ":getAsObject():UIComponent=" + component.getId());
    if (value != null && value.trim().length() > 0) {
      String[] t = value.split("\\|");
      try {
        int cid = Integer.parseInt(t[0]);
        //int zid = Integer.valueOf(value);
        if (cid >= 0) {
          NetCenter c = ejbCenterAdmin.readNetCenterByID(cid);
          return c;
        }
      } catch (NumberFormatException nfe) {
        //throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Not a valid Zone."));
      }
    }
    return null;
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) {
    Log.log(Level.FINEST, "{0}:getAsString():UIComponent={1}", new Object[]{NetCenterConverter.class.getName(), component.getId()});
    //somDrugProduct
    if (value != null) {
      if (value instanceof NetCenter) {
        NetCenter c = (NetCenter) value;
        return c.getNetCenterPK().getCenterid()+ "|" + c.getName();
      }
    }
    return null;
  }
}
