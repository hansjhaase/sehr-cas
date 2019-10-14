package org.ifeth.sehr.p1507291734.lib;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *
 * @author hansjhaase
 */
public class LoggerUtility {

  public static void assignLevelByDebug(int debug, Logger logger) {
    switch (debug) {
      case 0:
        logger.setLevel(Level.OFF);
        break;
      case 1:
        logger.setLevel(Level.SEVERE);
        break;
      case 2:
        logger.setLevel(Level.WARNING);
        break;
      case 3:
      case 4:
        logger.setLevel(Level.INFO);
        break;
      case 5:
      case 6:
        logger.setLevel(Level.CONFIG);
        break;
      case 7:
        logger.setLevel(Level.FINE);
        break;
      case 8:
        logger.setLevel(Level.FINER);
        break;
      case 9:
        logger.setLevel(Level.FINEST);
        break;
      default:
        logger.setLevel(Level.INFO);
        break;
    }
  }

  public static void assignConfig(FileInputStream fis) throws IOException {
    LogManager.getLogManager().readConfiguration(fis);
  }
}
