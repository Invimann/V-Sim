/*
Copyright (C) 2018-2019 Andres Castellanos

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

package vsim.simulator;

import java.util.HashMap;
import vsim.Globals;
import vsim.Settings;
import vsim.assembler.statements.Statement;
import vsim.linker.LinkedProgram;
import vsim.riscv.MemorySegments;
import vsim.riscv.exceptions.*;
import vsim.riscv.instructions.MachineCode;
import vsim.utils.Data;
import vsim.utils.IO;
import vsim.utils.Message;


/**
 * The class Debugger implements a simple debugger for RISC-V programs.
 */
public final class Debugger {

  /** simulator history */
  private History history;
  /** a linked program to debug */
  private LinkedProgram program;
  /** a history of breakpoints */
  private HashMap<Integer, Boolean> breakpoints;
  /** previous command */
  private String[] args;

  /**
   * Unique constructor that takes a linked program
   *
   * @param program the linked program
   * @see vsim.linker.LinkedProgram
   */
  public Debugger(LinkedProgram program) {
    this.program = program;
    this.breakpoints = new HashMap<Integer, Boolean>();
    this.args = null;
    // set program breakpoints
    for (Integer breakpoint : program.getBreakpoints())
      this.breakpoints.put(breakpoint, true);
    // create history
    this.history = new History();
    // take a snapshot of the memory
    Globals.memory.snapshot();
  }

  /**
   * This method pretty prints the debugger help message.
   */
  private void help() {
    IO.stdout.println("Available commands: " + System.getProperty("line.separator"));
    // help
    IO.stdout.println("help/?               - show this help message");
    // exit
    IO.stdout.println("exit/quit            - exit the simulator and debugger");
    // execute previous
    IO.stdout.println("!                    - execute previous command");
    // print state
    IO.stdout.println("showx                - print all RVI registers");
    IO.stdout.println("showf                - print all RVF registers");
    IO.stdout.println("printx regname       - print RVI register");
    IO.stdout.println("printf regname       - print RVF register");
    IO.stdout.println("memory address       - print 12 x 4 cells of memory starting at address");
    IO.stdout.println("memory address rows  - print rows x 4 cells of memory starting at address");
    IO.stdout.println("globals              - print global symbols");
    IO.stdout.println("locals               - print local symbols of a file");
    // execution and breakpoints
    IO.stdout.println("step/s               - step the program for 1 instruction");
    IO.stdout.println("backstep/b           - backstep the program for 1 instruction");
    IO.stdout.println("continue/c           - continue program execution without stepping");
    IO.stdout.println("breakpoint/b address - set a breakpoint at address");
    IO.stdout.println("clear                - clear all breakpoints");
    IO.stdout.println("delete addr          - delete breakpoint at address");
    IO.stdout.println("list                 - list all breakpoints");
    // reset state and start again
    IO.stdout.println("reset                - reset all state (regs, memory) and start again");
  }

  /**
   * This method pretty prints the RVI register file.
   *
   * @see vsim.riscv.RVIRegisterFile
   */
  private void showx() {
    Globals.regfile.print();
  }

  /**
   * This method pretty prints the RVF register file.
   *
   * @see vsim.riscv.RVFRegisterFile
   */
  private void showf() {
    Globals.fregfile.print();
  }

  /**
   * This method tries to print a register of the RVI register file.
   *
   * @param reg the register name to print
   * @see vsim.riscv.RVIRegisterFile
   */
  private void printx(String reg) {
    try {
      Globals.regfile.printReg(reg);
    } catch (IllegalArgumentException e) {
      Message.error("invalid register name: " + reg);
    }
  }

  /**
   * This method tries to print a register of the RVF register file.
   *
   * @param reg the register name to print
   * @see vsim.riscv.RVFRegisterFile
   */
  private void printf(String reg) {
    try {
      Globals.fregfile.printReg(reg);
    } catch (IllegalArgumentException e) {
      Message.error("invalid register name: " + reg);
    }
  }

  /**
   * This methods prints a portion of the RISC-V memory
   *
   * @param address the address to start printing memory in hex or decimal
   * @param rows how many rows of 4 memory cells to print
   * @see vsim.riscv.Memory
   */
  private void memory(String address, String rows) {
    // default print 12 rows
    int n = 12;
    if (rows != null) {
      try {
        n = Data.parseInt(rows);
        if (n < 0) {
          Message.warning("number of rows should be > 0");
          return;
        }
      } catch (Exception e) {
        Message.warning("invalid number of rows: " + rows);
        return;
      }
    }
    int addr;
    try {
      addr = Data.parseInt(address);
    } catch (Exception e) {
      Message.error("invalid address: " + address);
      return;
    }
    Globals.memory.print(addr, n);
  }

  /**
   * This method prints the global symbol table.
   *
   * @see vsim.Globals#globl
   */
  private void globals() {
    Globals.globl.print();
  }

  /**
   * This method prints the local symbol table of all files.
   *
   * @see vsim.Globals#local
   */
  private void locals() {
    for (String filename : Globals.local.keySet()) {
      IO.stdout.println(filename);
      Globals.local.get(filename).print();
    }
  }

  /**
   * This method tries to step the program by one statement and pretty prints debug information.
   *
   * @param goStep if its a go step or a normal step
   * @return true if could step the program, false otherwise
   */
  public synchronized boolean step(boolean goStep) {
    try {
      // already done ?
      if (Status.EXIT.get())
        return false;
      Statement stmt = program.next();
      int pcVal = Globals.regfile.getProgramCounter();
      String pc = String.format("0x%08x", pcVal);
      // manage breakpoints
      if (goStep && this.breakpoints.containsKey(pcVal) && this.breakpoints.get(pcVal)) {
        // breakpoint at this point ?
        this.breakpoints.put(pcVal, false);
        return false;
      }
      // get statement machine code
      MachineCode result = stmt.result();
      // display console info (CLI mode only)
      if (!Settings.GUI && !goStep) {
        String source = stmt.getDebugInfo().getSource();
        // format all debugging info
        IO.stdout.println(String.format("FROM: %s", stmt.getDebugInfo().getFilename()));
        IO.stdout.println(String.format("PC [%s] CODE:%s    %s » %s", pc, result.toString(), source,
            Globals.iset.get(stmt.getMnemonic()).disassemble(result)));
      }
      // save current pc to history
      this.history.pushPCAndHeap();
      // execute instruction
      Globals.iset.get(stmt.getMnemonic()).execute(result);
      // save diff between prev executed state and current executed states
      this.history.pushState();
      // reset breakpoint
      if (this.breakpoints.containsKey(pcVal))
        this.breakpoints.put(pcVal, true);
    } catch (BreakpointException e) {
      Globals.regfile.incProgramCounter();
    } catch (NonInstructionException e) {
      // if self-modifying code is enabled
      // search in memory for a machine code
      if (Settings.SELF_MODIFYING) {
        try {
          int pcVal = Globals.regfile.getProgramCounter();
          String pc = String.format("0x%08x", pcVal);
          // grab machine code
          MachineCode code = new MachineCode(Globals.memory.loadWord(pcVal));
          // decode inst mnemonic
          String mnemonic = Globals.iset.decode(code);
          // if a valid inst was found, execute it
          if (mnemonic != null) {
            // manage breakpoints
            if (goStep) {
              // runtime ebreak
              if ("ebreak".equals(mnemonic) && !this.breakpoints.containsKey(pcVal)) {
                this.breakpoints.put(pcVal, false);
                return false;
              }
              // breakpoint at this point ?
              if (this.breakpoints.containsKey(pcVal) && this.breakpoints.get(pcVal)) {
                this.breakpoints.put(pcVal, false);
                return false;
              }
            }
            // display console info
            if (!goStep) {
              // format all debugging info
              String source = Globals.iset.get(mnemonic).disassemble(code);
              if (Settings.GUI) {
                IO.guistdout.postRunMessage("FROM: self-modify code" + System.getProperty("line.separator"));
                IO.guistdout
                    .postRunMessage(String.format("PC [%s] CODE:%s    %s » %s", pc, code.toString(), source, source)
                        + System.getProperty("line.separator"));
              } else {
                IO.stdout.println(String.format("FROM: %s", "self-modify code"));
                IO.stdout.println(String.format("PC [%s] CODE:%s    %s » %s", pc, code.toString(), source, source));
              }
            }
            // save current pc to history
            this.history.pushPCAndHeap();
            // execute instruction
            Globals.iset.get(mnemonic).execute(code);
            // save diff between prev executed state and current executed states
            this.history.pushState();
            // reset breakpoint
            if (this.breakpoints.containsKey(pcVal))
              this.breakpoints.put(pcVal, true);
          } else {
            // error if no exit/exit2 ecall
            if (!Status.EXIT.get()) {
              Status.EXIT.set(true);
              Message.runError(e.getMessage());
              if (!Settings.GUI)
                System.exit(1);
            }
            return false;
          }
        } catch (BreakpointException ex) {
          Globals.regfile.incProgramCounter();
        } catch (SimulationException ex) {
          // error if no exit/exit2 ecall
          if (!Status.EXIT.get()) {
            Status.EXIT.set(true);
            Message.runError(ex.getMessage());
            if (!Settings.GUI)
              System.exit(1);
          }
          return false;
        }
      } else {
        // error if no exit/exit2 ecall
        if (!Status.EXIT.get()) {
          Status.EXIT.set(true);
          Message.runError(e.getMessage());
          if (!Settings.GUI)
            System.exit(1);
        }
        return false;
      }
    } catch (SimulationException e) {
      if (!Status.EXIT.get()) {
        Status.EXIT.set(true);
        Message.runError(e.getMessage());
        if (!Settings.GUI)
          System.exit(1);
      }
      return false;
    }
    return true;
  }

  /**
   * This method tries to backstep the program by one statement restoring also the simulator state.
   */
  public void backstep() {
    this.history.pop();
  }

  /**
   * This method continues the program execution until a breakpoint or no more available statements are found.
   */
  public void go() {
    while (this.step(true))
      ;
  }

  /**
   * This method tries to create a breakpoint at the given address.
   *
   * @param address the address of the breakpoint in hex or decimal
   */
  public void breakpoint(String address) {
    try {
      int addr = Data.parseInt(address);
      if (Data.isWordAligned(addr)) {
        if (Data.inRange(addr, MemorySegments.TEXT_SEGMENT_BEGIN, MemorySegments.TEXT_SEGMENT_END)) {
          if (!this.breakpoints.containsKey(addr))
            this.breakpoints.put(addr, true);
        } else
          Message.error("breakpoint address has to be inside the text segment");
      } else
        Message.error("address is not aligned to a word boundary");
    } catch (Exception e) {
      Message.error("invalid address: " + address);
    }
  }

  /**
   * This method clears all the breakpoints that user set.
   */
  public void clear() {
    this.breakpoints.clear();
    // set program breakpoints
    for (Integer breakpoint : program.getBreakpoints())
      this.breakpoints.put(breakpoint, true);
  }

  /**
   * This method tries to delete a breakpoint that user set.
   *
   * @param address a string representing the address in hex or decimal
   */
  public void delete(String address) {
    int addr;
    try {
      addr = Data.parseInt(address);
    } catch (Exception e) {
      Message.error("invalid address: " + address);
      return;
    }
    if (this.breakpoints.containsKey(addr))
      if (this.program.getStatement(addr) != null && this.program.getStatement(addr).getMnemonic().equals("ebreak"))
        Message.warning("could not delete a ebreak breakpoint (ignoring)");
      else
        this.breakpoints.remove(addr);
    else
      Message.warning("no breakpoint at address: " + address + " (ignoring)");
  }

  /**
   * This method lists the breakpoints that user set.
   */
  private void list() {
    if (this.breakpoints.size() > 0) {
      IO.stdout.println("Breakpoints: " + System.getProperty("line.separator"));
      for (Integer address : this.breakpoints.keySet())
        IO.stdout.println(String.format("    0x%08x", address));
    } else
      Message.log("no breakpoints yet");
  }

  /**
   * This method resets the program and the state of the simulator.
   */
  public void reset() {
    Status.reset();
    Globals.resetState();
    this.history.reset();
    this.program.reset();
  }

  /**
   * This method takes an array of arguments and tries to match this with an available debug command and interprets it.
   *
   * @param args the command arguments
   */
  private void interpret(String[] args) {
    // save previous args
    if (!args[0].equals("!"))
      this.args = args;
    // exit/quit
    if ((args[0].equals("exit") || args[0].equals("quit"))) {
      if (args.length != 1)
        Message.warning("exit command does not expect any argument (ignoring)");
      System.exit(0);
    }
    // help/?
    else if ((args[0].equals("help") || args[0].equals("?"))) {
      if (args.length != 1)
        Message.warning("help command does not expect any argument (ignoring)");
      this.help();
    }
    // !
    else if (args[0].equals("!")) {
      if (args.length != 1)
        Message.warning("! command does not expect any argument (ignoring)");
      if (args != null)
        this.interpret(this.args);
    }
    // showx
    else if (args[0].equals("showx")) {
      if (args.length != 1)
        Message.warning("showx command does not expect any argument (ignoring)");
      this.showx();
    }
    // showf
    else if (args[0].equals("showf")) {
      if (args.length != 1)
        Message.warning("showf command does not expect any argument (ignoring)");
      this.showf();
    }
    // printx
    else if (args[0].equals("printx")) {
      if (args.length == 2)
        this.printx(args[1]);
      else
        Message.error("invalid usage of printx cmd, valid usage 'printx regname'");
    }
    // printf
    else if (args[0].equals("printf")) {
      if (args.length == 2)
        this.printf(args[1]);
      else
        Message.error("invalid usage of printf cmd, valid usage 'printf regname'");
    }
    // memory
    else if (args[0].equals("memory")) {
      if (args.length == 2)
        this.memory(args[1], null);
      else if (args.length == 3)
        this.memory(args[1], args[2]);
      else
        Message.error("invalid usage of memory cmd, valid usage 'memory address [rows]'");
    }
    // globals
    else if (args[0].equals("globals")) {
      if (args.length != 1)
        Message.warning("globals command does not expect any argument (ignoring)");
      this.globals();
    }
    // locals
    else if (args[0].equals("locals")) {
      if (args.length != 1)
        Message.error("locals command does not expect any argument (ignoring)");
      this.locals();
    }
    // step
    else if (args[0].equals("step") || args[0].equals("s")) {
      if (args.length != 1)
        Message.warning("step command does not expect any argument (ignoring)");
      this.step(false);
    }
    // backstep
    else if (args[0].equals("backstep") || args[0].equals("b")) {
      if (args.length != 1)
        Message.warning("backstep command does not expect any argument (ignoring)");
      this.backstep();
    }
    // continue
    else if (args[0].equals("continue") || args[0].equals("c")) {
      if (args.length != 1)
        Message.warning("continue command does not expect any argument (ignoring)");
      this.go();
    }
    // breakpoint
    else if (args[0].equals("breakpoint") || args[0].equals("b")) {
      if (args.length == 2)
        this.breakpoint(args[1]);
      else
        Message.error("invalid usage of breakpoint cmd, valid usage 'breakpoint/b address'");
    }
    // clear
    else if (args[0].equals("clear")) {
      if (args.length != 1)
        Message.warning("clear command does not expect any argument (ignoring)");
      this.clear();
    }
    // delete addr
    else if (args[0].equals("delete")) {
      if (args.length == 2)
        this.delete(args[1]);
      else
        Message.error("invalid usage of delete cmd, valid usage 'delete address'");
    }
    // list
    else if (args[0].equals("list")) {
      if (args.length != 1)
        Message.warning("list command does not expect any argument (ignoring)");
      this.list();
    }
    // reset
    else if (args[0].equals("reset")) {
      if (args.length != 1)
        Message.warning("reset command does not expect any argument (ignoring)");
      this.reset();
    } else
      Message.warning("unknown command '" + args[0] + "' (ignoring)");
  }

  /**
   * Gets linked program.
   *
   * @return linked program
   */
  public LinkedProgram getProgram() {
    return this.program;
  }

  /**
   * This method creates a command line interface that the user can use to interact with the debugger.
   */
  public void run() {
    while (true) {
      IO.stdout.print(">>> ");
      // read a line from stdin
      String line = IO.readString(Integer.MAX_VALUE);
      // nothing entered
      if ("".equals(line))
        continue;
      // interpret line
      this.interpret(line.trim().toLowerCase().split(" "));
    }
  }

}
