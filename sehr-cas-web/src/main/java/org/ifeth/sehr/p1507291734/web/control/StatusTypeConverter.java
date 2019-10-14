/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.control;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.ifeth.sehr.core.spec.SEHRConstants;

/**
 *
 * @author Hans J Haase <hansjhaase@mdigmbh.de>
 */
@FacesConverter("StatusTypeConverter")
public class StatusTypeConverter implements Converter {

  private static final Logger Log = Logger.getLogger("org.ifeth.sehr.p1507291734.web");

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) {
    Log.info(StatusTypeConverter.class.getName() + ":getAsObject():UIComponent=" + component.getId());
    if (value != null && value.trim().length() > 0) {
      Log.log(Level.INFO, "{0}:getAsObject():value={1}", new Object[]{StatusTypeConverter.class.getName(), value});
      if (listStatusConstants().containsValue(value)) {
        Map<Short, String> map = listStatusConstants();
        for (Map.Entry<Short, String> entry : map.entrySet()) {
          System.out.println(StatusTypeConverter.class.getName() +"Key = " + entry.getKey() + ", Value = " + entry.getValue());
          if (value.equalsIgnoreCase(entry.getValue())) {
            return new KeyValueItem(entry.getKey(), entry.getValue());
          }
        }
      }

    }
    return null;
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) {
    Log.log(Level.INFO, "{0}:getAsString():UIComponent={1}", new Object[]{StatusTypeConverter.class.getName(), component.getId()});
    if (value != null) {
      Log.log(Level.INFO, "{0}:getAsString():value={1}", new Object[]{StatusTypeConverter.class.getName(), value});
      if (value instanceof KeyValueItem) {
        KeyValueItem item = (KeyValueItem) value;
        return item.getValue();
      }
    }
    return "";
  }

  //TODO use LifeCARDObjectHandler.listStatusConstants()
  private Map<Short, String> listStatusConstants() {
    Map<Short, String> c = new HashMap<>();
    c.put(SEHRConstants.LifeCARD_STS_NEW, "New Account (in progress)");
    c.put(SEHRConstants.LifeCARD_STS_REG, "Registration Phase");
    c.put(SEHRConstants.LifeCARD_STS_REGVFYD, "Reg./Person Verified");
    c.put(SEHRConstants.LifeCARD_STS_CRDORD, "Card Ordered (for Production)");
    c.put(SEHRConstants.LifeCARD_STS_CRDPRD, "Card Produced");
    c.put(SEHRConstants.LifeCARD_STS_CRDSHP, "Card Shipped (to Holder)");
    c.put(SEHRConstants.LifeCARD_STS_CRDRCV, "Card Received (by Holder)");
    c.put(SEHRConstants.LifeCARD_STS_CRDLKP, "Card Locked by Holder/Patient");
    c.put(SEHRConstants.LifeCARD_STS_CRDLKI, "Card Locked by Issuer");
    c.put(SEHRConstants.LifeCARD_STS_OC, "Cancelled/Inactivated");
    return c;
  }
}
