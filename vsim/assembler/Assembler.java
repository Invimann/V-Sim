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

package vsim.assembler;

import vsim.Errors;
import vsim.Globals;
import vsim.utils.Message;
import java.util.ArrayList;
import vsim.assembler.statements.Statement;


/**
 * The Assembler class  assembles RISC-V source files.
 */
public final class Assembler {

  /** current assembler segment */
  public static Segment segment = Segment.TEXT;

  /** current assembler program */
  public static Program program = null;

  /** current assembler filename */
  public static String filename = null;

  /**
   * This method is used to assemble all the RISC-V files and it is
   * called before the linkage process.
   *
   * @param files the RISC-V files to assemble
   * @see vsim.assembler.Program
   * @return all the assembled files
   */
  public static ArrayList<Program> assemble(ArrayList<String> files) {
    ArrayList<Program> programs = new ArrayList<Program>();
    // assemble all files
    if (files.size() > 0) {
      for (String file: files) {
        // current filename
        Assembler.filename = file;
        // start in text segment
        Assembler.segment = Segment.TEXT;
        // create a new RISC-V Program
        Program program = new Program(file);
        // set current program
        Assembler.program = program;
        // parse line by line all file
        Parser.parse(file);
        // add this processed program only if it has statements
        if (program.getTextSize() > 0 || ((files.size() > 1) && (program.getDataSize() > 0)))
          programs.add(program);
        else {
          if (files.size() == 1)
            Errors.add("assembler: file '" + file + "' does not have any valid instruction");
          else
            Message.warning("assembler: file '" + file + "' does not have any valid instruction or data (ignoring)");
        }
      }
    }
    // do first pass
    for (Program program: programs) {
      // add program ST to Globals.local
      SymbolTable table = program.getST();
      String filename = program.getFilename();
      Globals.local.put(filename, table);
      // check globals of program
      for (String global: program.getGlobals()) {
        Symbol sym = table.getSymbol(global);
        DebugInfo debug = program.getGlobalDebug(global);
        if (sym != null) {
          if(!Globals.globl.add(global, sym))
            Errors.add(debug, "assembler", "'" + global + "' already defined as global in a different file");
        } else
          Errors.add(debug, "assembler", "'" + global + "' declared global label but not defined");
      }
    }
    // try to resolve all statements and collect errors if any
    for (Program program: programs) {
      for (Statement stmt: program.getStatements())
        stmt.resolve();
    }
    // no unlinked programs ?
    if (programs.size() == 0)
      Errors.add("assembler: no valid RISC-V source file was passed");
    // report errors
    if (!Errors.report()) {
      // clean all
      Assembler.program = null;
      Assembler.filename = null;
      Assembler.segment = Segment.TEXT;
      // no more parsing, we can clean the parser structure
      System.gc();
      programs.trimToSize();
      // return all processed programs, now linking ?
      return programs;
    }
    return null;
  }

}
