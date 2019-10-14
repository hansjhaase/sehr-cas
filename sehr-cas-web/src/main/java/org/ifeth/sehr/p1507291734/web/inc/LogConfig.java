/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ifeth.sehr.p1507291734.web.inc;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.*;

public class LogConfig {

  public LogConfig(String logger, Level level) {
    try {
      // Load a properties file from class path that way can't be achieved with java.util.logging.config.file
      final LogManager logManager = LogManager.getLogManager();
      try (final InputStream is = getClass().getResourceAsStream("/logging.properties")) {
        logManager.readConfiguration(is);
      }

      // Programmatic configuration
      System.setProperty("java.util.logging.SimpleFormatter.format",
              "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");

      final ConsoleHandler consoleHandler = new ConsoleHandler();
      consoleHandler.setLevel(level);
      consoleHandler.setFormatter(new SimpleFormatter());

      final Logger log = Logger.getLogger(logger);
      log.setLevel(level);
      log.addHandler(consoleHandler);
    } catch (IOException | SecurityException e) {
      // The runtime won't show stack traces if the exception is thrown
      throw new RuntimeException(e.getMessage());
    }
  }
}
