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

package vsim.riscv.instructions;

/**
 * The class MachineCode represents the machine code of an instruction.
 */
public final class MachineCode {

  /** machine code */
  private int code;

  /**
   * Creates a initialized machine code.
   *
   * @param code initial machine code
   */
  public MachineCode(int code) {
    this.code = code;
  }

  /**
   * Creates a new machine code initialized with {@code 0x00000000}.
   */
  public MachineCode() {
    this(0x0);
  }

  /**
   * This method returns the value of the instruction at the given field.
   *
   * @param field the instruction field to get
   * @return the instruction field value
   */
  public int get(InstructionField field) {
    return (this.code >>> field.lo) & field.mask;
  }

  /**
   * This method sets the value of the instruction at the given field.
   *
   * @param field the instruction field to set
   * @param value the new instruction field value
   */
  public void set(InstructionField field, int value) {
    this.code = (this.code & ~(field.mask << field.lo)) | ((value & field.mask) << field.lo);
  }

  /**
   * This method returns a String representation of a MachineCode object.
   *
   * @return the String representation
   */
  @Override
  public String toString() {
    return String.format("0x%08x", this.code);
  }

}
