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

package vsim.riscv.exceptions;

import vsim.Globals;


/**
 * Breakpoint exception class throwed by ebreak statement.
 */
public final class BreakpointException extends SimulationException {

  /**
   * Creates a new breakpoint exception.
   */
  public BreakpointException() {
    super(String.format("breakpoint exception at: 0x%08x", Globals.regfile.getProgramCounter()));
  }

}
