/*
Copyright (C) 2018 Andres Castellanos

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>
*/

package vsim.utils;

import vsim.Globals;
import java.util.ArrayList;


/**
 * The Message class contains useful methods for printing to stdout and stderr
   with a predefined format.
 */
public final class Message {

  private Message() { /* NOTHING */ }

  /**
   * This method prints to stdout a log message with the format
   * "vsim: <i>msg</i>".
   *
   * @param msg the log message
   */
  public static void log(String msg) {
    IO.stdout.println(Colorize.cyan("vsim: " + msg));
  }

  /**
   * This method prints to stdout a warning message with the format
   * "vsim: (warning) <i>msg</i>".
   *
   * @param msg the warning message
   */
  public static void warning(String msg) {
    IO.stdout.println(Colorize.yellow("vsim: (warning) " + msg));
  }

  /**
   * This method prints to stderr an error message with the format
   * "vsim: (error) <i>msg</i>".
   *
   * @param msg the error message
   */
  public static void error(String msg) {
    IO.stderr.println(Colorize.red("vsim: (error) " + msg));
  }

  /**
   * This method prints to stderr an error message with the format
   * "vsim: (error) <i>msg</i>" and then exits with a status code of 1.
   *
   * @param msg the error message
   */
  public static void panic(String msg) {
    IO.stderr.println(Colorize.red("vsim: (error) " + msg));
    System.exit(1);
  }

}
