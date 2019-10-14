/*
 * Some reusable methods for bulding web based test pages
 */
package org.ifeth.sehr.p1507291734.web.inc;

/**
 * Class with static constants and methods get common web based tags
 *
 * Preferred method is {@link htmlHeaderBasicCss(title)}
 *
 * @author hansjhaase
 */
public class HTMLFrameUtils {

  /**
   * Implements a DOCTYPE definition
   *
   */
  public static final String DOCTYPE
          = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";

  /**
   * Implements a very basic CSS.
   *
   */
  public static final String BASICCSS
          = "<style type='text/css'>"
          + "html, body {"
          + "  font-family: Arial, sans-serif;"
          + "  font-size: 1.0em;"
          + "  font-weight: normal;"
          + "}"
          + "h1{"
          + "  font-size: 1.4em;"
          + "  font-weight: bold;"
          + "  display: block;"
          + "  background-color: #66b8dc;" //JMed Blue
          + "}"
          + "h2{"
          + "  font-size: 1.2em;"
          + "  font-weight: bold;"
          + "}"
          + "h3{"
          + "  font-size: 1.0em;"
          + "  font-weight: normal;"
          + "  font-style: italic;"
          + "}"
          + "p {"
          + "  line-height: 1.1em;"
          + "}"
          + "ul {"
          + "  font-size: 0.9em;"
          + "}"
          + ".error {"
          + "  color: red;"
          + "}"
          + "</style>";

  /**
   * Builds a HTML header including a basic CSS
   *
   * @param title
   * @return (String) HTML Header
   */
  public static String htmlHeaderBasicCss(String title) {
    return (DOCTYPE + "\n"
            + "<HTML>\n"
            + "<HEAD><TITLE>" + title + "</TITLE>\n"
            + BASICCSS
            + "</HEAD>\n");
  }
  /**
   * Builds a HTML header including JQuery and a basic CSS
   *
   * @param title
   * @return (String) HTML Header
   */
  public static String htmlHeaderJMedCss(String title) {
    return (DOCTYPE + "\n"
            + "<HTML>\n"
            + "<HEAD><TITLE>" + title + "</TITLE>\n"
            + BASICCSS
            + "<link href='./res/style/sehr2/jquery-ui-1.10.3.custom.min.css' rel='stylesheet' type='text/css' />"
            + "<link href='./res/style/jquery.stickynotes.css' rel='stylesheet' type='text/css' />"
            + "<link href='./res/style/sehr.css' rel='stylesheet' type='text/css' />"
            + "<script type='text/javascript' src='./lib/jquery-1.11.1.js'></script>"
            + "<script type='text/javascript' src='./lib/jquery-ui-1.10.3.custom.min.js'></script>" 
            + "</HEAD>\n");
  }

  public static String htmlFooter() {
    return ("</body></html>");
  }
}
