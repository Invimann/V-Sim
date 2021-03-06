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

package vsim.riscv.instructions.stype;

import vsim.Globals;
import vsim.riscv.exceptions.SimulationException;


/**
 * The Sb class represents a sb instruction.
 */
public final class Sb extends SType {

  /**
   * Unique constructor that initializes a newly Sb instruction.
   *
   * @see vsim.riscv.instructions.stype.SType
   */
  public Sb() {
    super("sb", "sb rs2, offset(rs1)", "set memory[x[rs1] + sext(offset)] = x[rs2][7:0]");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void setMemory(int rs1, int rs2, int imm) throws SimulationException {
    Globals.memory.storeByte(rs1 + imm, rs2);
  }

}
