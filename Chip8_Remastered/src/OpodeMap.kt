import java.lang.Integer.toHexString
import java.util.regex.Pattern.matches
import java.lang.Character.getNumericValue

/*
    An interface for all opcodes to implement so they can be stored as a hash map in our main Chip8 emulation to
    sidestep using a gross big switch statement like in my preliminary implementation
*/
interface Opcode {
    fun execute(instance: Chip8)    // The method to be implemented to execute opcode functions
}

/*
    We also define a custom exception for undefined opcodes because even though it doesn't really handle anything it
    makes the program easier to debug as it will print out what opcode it failed to run.
*/
internal class UndefinedOpcode(opcode: Int): Exception() {
    init {
        println("Interpreter was given an undefined opcode: ${toHexString(opcode)}")
    }
}

/*
    Below is the implementation of all of the opcode needed for the Chip8 interpretation in class form.
    They extend the opcode interface which grants them one method, execute, wherein they execute their specified
    action and then return. They are given the current Chip 8 instance as a parameter in order to manipulate
    class variables. What each class does to the Chip 8 instance is outline before and in each opcode's execute method
*/
internal class ClearScreen: Opcode {
    // Clears the screen of any pixel data held before the next request
    override fun execute(instance: Chip8) {
        // Iterates over the graphics array and sets all pixel data to 0
        for (i in 0 until 64)
            for (j in 0 until 32)
                instance.gfx[j][i] = 0
        instance.pc += 2
    }
}

internal class ReturnSubroutine: Opcode {
    // If the opcode is in the form 0x00EE then we need to return from a subroutine
    override fun execute(instance: Chip8) {
        instance.sp--
        instance.pc = instance.stack[instance.sp]
        instance.pc += 2
    }
}

internal class JumpAddress: Opcode {
    // If the opcode is in the form 1NNN then we jump to address NNN
    override fun execute(instance: Chip8) {
        instance.pc = instance.opcode and 0x0FFF
    }
}

internal class JumpSubroutine : Opcode {
    // If the opcode is in the form 2NNN then we need to run the subroutine at NNN
    override fun execute(instance: Chip8) {
        instance.stack[instance.sp] = instance.pc
        instance.sp++
        instance.pc = instance.opcode and 0x0FFF
    }
}

internal class SkipIfEqualValue: Opcode {
    // If the opcode is in the form 3XNN then we need to check if VX and NN are equal
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        val value = instance.opcode and 0x00FF
        instance.pc += if (instance.registers[index] == value) 4 else 2
    }
}

internal class SkipIfNotEqualValue: Opcode {
    // If the opcode is in the form 4XNN then we need to check if VX and NN are not equal
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        val value = instance.opcode and 0x00FF
        instance.pc += if (instance.registers[index] != value) 4 else 2
    }
}

internal class SkipIfEqualRegister: Opcode {
    // If the opcode is in the form 5XY0 then we need to check if the registers VX and VY are equal
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.pc += if (instance.registers[registerX] == instance.registers[registerY]) 4 else 2
    }
}

internal class StoreToRegister: Opcode {
    // If the opcode is in the form 6XNN then we need to store the value NN to the register VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.registers[index] = instance.opcode and 0x00FF
        instance.pc += 2
    }
}

internal class IncrementRegisterValue: Opcode {
    // If the opcode is in the form 7XNN then we need to increment the value in VX by the value NN
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.registers[index] += instance.opcode and 0x00FF
        if (255 < instance.registers[index])
            instance.registers[index] = instance.registers[index] and 0x00FF
        instance.pc += 2
    }
}

internal class SetRegistersEqual: Opcode {
    // If the opcode is in the form 8XY0 then we need to set VX to VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] = instance.registers[registerY]
        instance.pc += 2
    }
}

internal class BitwiseAndRegisters: Opcode {
    // If the opcode is in the form 8XY1 then we set VX equal to VX or VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] = instance.registers[registerX] and instance.registers[registerY]
        instance.pc += 2
    }
}

internal class BitwiseOrRegisters: Opcode {
    // If the opcode is in the form 8XY2 then we set VX equal to VX and VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] = instance.registers[registerX] or instance.registers[registerY]
        instance.pc += 2
    }
}

internal class BitwiseXorRegisters: Opcode {
    // If the opcode is in the form 8XY3 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] = instance.registers[registerX] xor instance.registers[registerY]
        instance.pc += 2
    }
}

internal class IncrementRegister: Opcode {
    // If the opcode is in the form 8XY4 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] += instance.registers[registerY]
        // Check if there is an integer overflow and set VF to 1 if there is
        if (255 < instance.registers[registerX]) {
            instance.registers[registerX] = instance.registers[registerX] and 0x00FF
            instance.registers[0xF] = 1
        }
        // If an overflow does not occur then we need to set VF to 0
        else instance.registers[0xF] = 0
        instance.pc += 2
    }
}

internal class DecrementRegister: Opcode {
    // If the opcode is in the form 8XY5 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] -= instance.registers[registerY]
        // Check to see if there is an integer underflow and set VF to 0 if there is
        if (instance.registers[registerX] < 0) {
            instance.registers[registerX] = instance.registers[registerX] and 0x00FF
            instance.registers[0xF] = 0
        }
        // Need to set VF to 1 if an underflow does not occur
        else instance.registers[0xF] = 1
        instance.pc += 2
    }
}

internal class BitwiseRightShift: Opcode {
    // If the opcode is in the form 8XY6 then we need to shift VY right by one bit and store that bit into VF
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        instance.registers[0xF] = instance.registers[registerX] and 0x01
        instance.registers[registerX] = instance.registers[registerX] ushr 1
        instance.pc += 2
    }
}

internal class DecrementRegisterInverse: Opcode {
    // If the opcode is in the form 8XY7 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.registers[registerX] = instance.registers[registerY] - instance.registers[registerX]
        // Check to see if there is an integer underflow and set the flags accordingly
        if (instance.registers[registerX] < 0) {
            instance.registers[registerX] = instance.registers[registerX] and 0xFF
            instance.registers[0xF] = 0
        }
        // Need to set VF to 1 if an underflow occurs
        else instance.registers[0xF] = 1
        instance.pc += 2
    }
}

internal class BitwiseLeftShift: Opcode {
    // If the opcode is in the form 8XYE then we need to shift VY left by one bit and store that bit into VF
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        instance.registers[0xF] = (instance.registers[registerX] and 0x80) ushr 7
        instance.registers[registerX] = (instance.registers[registerX] shl 1) and 0xFF
        instance.pc += 2
    }
}

internal class SkipIfNotEqualRegister: Opcode {
    // If the opcode is in the form 9XY0 then we need to check if the registers VX and VY are not equal and skip the next instruction if they arent
    override fun execute(instance: Chip8) {
        val registerX = (instance.opcode and 0x0F00) ushr 8
        val registerY = (instance.opcode and 0x00F0) ushr 4
        instance.pc += if (instance.registers[registerX] != instance.registers[registerY]) 4 else 2
    }
}

internal class SaveAddressToI: Opcode {
    // If the opcode is in the form ANNN then we need to store the register NNN into the I register
    override fun execute(instance: Chip8) {
        instance.registerI = instance.opcode and 0x0FFF
        instance.pc += 2
    }
}

internal class JumpToAddressSum: Opcode {
    // If the opcode is in the form BNNN then we jump to NNN summed with the value in register 0
    override fun execute(instance: Chip8) {
        instance.pc = (instance.opcode and 0x0FFF) + instance.registers[0]
    }
}

internal class RandomNumMask: Opcode {
    // If the opcode is in the form CXNN then we store a random number between 00-FF masked with NN into VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        val random = (0..255).random()
        val mask = instance.opcode and 0x00FF
        instance.registers[index] = random and mask
        instance.pc += 2
    }
}

internal class DrawSprite: Opcode {
    // If the opcode is in the form DXYN then we need to draw a pixel at the location x, y
    override fun execute(instance: Chip8) {
        // Gets the VX and Vy registers and the height of the current sprite
        val registerX = instance.registers[(instance.opcode and 0x0F00) ushr 8]
        val registerY = instance.registers[(instance.opcode and 0x00F0) ushr 4]
        val height = instance.opcode and 0xF

        // Nested for loop tp draw the 8 bit wide sprite and height specified by the instruction
        for (col in 0 until 8) {
            for (row in 0 until height) {
                // Gets the current pixel data from memory
                val pixel = instance.memory[instance.registerI + row]
                val bit = pixel and (0b10000000 ushr col)
                // Low key unsure as to what the fuck this does
                if (bit != 0) {
                    // We need to account for the wrap around of pixels
                    if (registerX + col >= 64)
                        continue
                    if (registerY + row >= 32)
                        continue
                    // If the current pixel is already 1 then we know that a pixel went from 1 to 0 and a draw flag needs to be set
                    if (instance.gfx[registerY+row][registerX+col] == 1)
                        instance.registers[0xF] = 1
                    // Xor the register with itself to switch its current value
                    instance.gfx[registerY + row][registerX + col] = instance.gfx[registerY + row][registerX + col] xor 1
                }
            }
        }
        // Increment the program counter
        instance.pc += 2
    }
}

internal class SkipIfMatchKey constructor(chip8: Chip8): Opcode {

    private val reader = chip8
    // If the opcode is in the form EX9E then we need to load values from memory into V0-VX
    override fun execute(instance: Chip8) {
        val regIndex = (instance.opcode and 0x0F00) ushr 8
        instance.pc += if(instance.registers[regIndex] == reader.currentKey?.toInt() ?: 0) 4 else 2
    }
}

internal class SkipIfNotMatchKey constructor(chip8: Chip8): Opcode {

    private val reader = chip8
    // If the opcode is in the form EXA1 then we need to load values from memory into V0-VX
    override fun execute(instance: Chip8) {
        val regIndex = (instance.opcode and 0x0F00) ushr 8
        instance.pc += if(instance.registers[regIndex] != reader.currentKey?.toInt() ?: 0) 4 else 2
    }
}

internal class StoreDelayToRegister: Opcode {
    // If the opcode is in the form FX07 then we need to save the value of the delay register to VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.registers[index] = instance.delay and 0xFF
        instance.pc += 2
    }
}

internal class WaitForKeyPress constructor(chip8: Chip8): Opcode {

    private val reader = chip8
    // If the opcode is in the form FX0A then we need to await a keypress and store that value into VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        while (reader.currentKey == null) {
            instance.registers[index] = reader.currentKey?.toInt() ?: 0
        }
    }
}

internal class SetDelayToRegister: Opcode {
    // If the opcode is in the form FX15 then we need to set the delay register to VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.delay = instance.registers[index] and 0xFF
        instance.pc += 2
    }
}

internal class SetSoundToRegister: Opcode {
    // If the opcode is in the form ____ then we need to set the sound timer to the value in VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.sound = instance.registers[index] and 0xFF
        instance.pc += 2
    }
}

internal class AddRegisterToSpecial: Opcode {
    // If the opcode is in the form FX1E then we need to increment the I register by VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.registerI += instance.registers[index] and 0xFF
        instance.pc += 2
    }
}

internal class SpriteAddress: Opcode {
    // If the opcode is in the form FX29 then we need to set I to the location of the character in VX
    override fun execute(instance: Chip8) {
        val index = (instance.opcode and 0x0F00) ushr 8
        instance.registerI = instance.registers[index] * 5
        instance.pc += 2
    }
}

internal class BinaryCodedDecimal: Opcode {
    // If the opcode is in the form FX33 then we need to store the value at VX into 3 addresses in memory in its BSD form
    override fun execute(instance: Chip8) {
        // Get the register and the string representing the number to store to memory
        val regIndex = (instance.opcode and 0x0F00) ushr 8
        val numString = instance.registers[regIndex].toString()
        /* We iterate backwards over the string to  fill it as I+2, I+1, I with the 1's, 10's and 100's places
           respectively. We do this because if the string is too short ie less than 3 digits we need to add 0's
           in either the 100's or 10's places to extend the number according to spec which is easier to do going
           forwards*/
        for (i in numString.indices.reversed())
            instance.memory[instance.registerI + i] = getNumericValue(numString[i])
        // Now if the string was too short we fill in the gaps here with 0's going forwards
        for (i in 0 until 3-numString.length)
            instance.memory[instance.registerI + i] = 0
        instance.pc += 2
    }
}

internal class RegisterDump: Opcode {
    // If the opcode is in the form FX55 then we need to dump the contents of V0-VX into memory
    override fun execute(instance: Chip8) {
        val regIndex = (instance.opcode and 0x0F00) ushr 8
        for (i in 0..regIndex)
            instance.memory[instance.registerI + i] = instance.registers[i] and 0xFF
        instance.pc += 2
    }
}

internal class RegisterLoad: Opcode {
    // If the opcode is in the form FX65 then we need to load values from memory into V0-VX
    override fun execute(instance: Chip8) {
        val regIndex = (instance.opcode and 0x0F00) ushr 8
        for (i in 0..regIndex)
            instance.registers[i] = instance.memory[instance.registerI + i] and 0xFF
        instance.pc += 2
    }
}

/*
    Now that we have implemented all of the opcodes we need to organize them into a hashmap
    to be abe to cleanly access them from the Chip8 class itself. This is best abstracted by
    creating a class whose purpose is to collect all of these classes and organize them into such
    a hashmap.
*/
class OpcodeMap constructor(instance: Chip8): HashMap<String, Opcode>() {

    // The array of regex compliant strings to match against opcodes and the current Chip8 instance
    private val regexKeys: Array<String>
    private val chip8 = instance

    init {
        // Constructs a new hashmap whose entries are the opcodes defined above
        this["e0"] = ClearScreen()
        this["ee"] = ReturnSubroutine()
        this["1..."] = JumpAddress()
        this["2..."] = JumpSubroutine()
        this["3..."] = SkipIfEqualValue()
        this["4..."] = SkipIfNotEqualValue()
        this["5..0"] = SkipIfEqualRegister()
        this["6..."] = StoreToRegister()
        this["7..."] = IncrementRegisterValue()
        this["8..0"] = SetRegistersEqual()
        this["8..1"] = BitwiseOrRegisters()
        this["8..2"] = BitwiseAndRegisters()
        this["8..3"] = BitwiseXorRegisters()
        this["8..4"] = IncrementRegister()
        this["8..5"] = DecrementRegister()
        this["8..6"] = BitwiseRightShift()
        this["8..7"] = DecrementRegisterInverse()
        this["8..e"] = BitwiseLeftShift()
        this["9..0"] = SkipIfNotEqualRegister()
        this["a..."] = SaveAddressToI()
        this["b..."] = JumpToAddressSum()
        this["c..."] = RandomNumMask()
        this["d..."] = DrawSprite()
        this["e.9e"] = SkipIfMatchKey(chip8)
        this["e.a1"] = SkipIfNotMatchKey(chip8)
        this["f.07"] = StoreDelayToRegister()
        this["f.0a"] = WaitForKeyPress(chip8)
        this["f.15"] = SetDelayToRegister()
        this["f.18"] = SetSoundToRegister()
        this["f.1e"] = AddRegisterToSpecial()
        this["f.29"] = SpriteAddress()
        this["f.33"] = BinaryCodedDecimal()
        this["f.55"] = RegisterDump()
        this["f.65"] = RegisterLoad()
        // Initializes the regexKeys to the regex compliant keys of the HashMap
        regexKeys = keys.toTypedArray()
    }

    fun executeOpcode(instruction: Int) {
        // Convert the opcode to a hex string for comparisons and set a flag for undefined opcodes
        val opcode = toHexString(instruction)
        var isValidCode = false
        // Iterate over the regex keys and check to see what opcode it matches, if any
        for (test in regexKeys)
            if (matches(test, opcode)) {
                isValidCode = true
                this[test]!!.execute(chip8)
            }
        // If no opcode match is found then it will throw an undefined opcode exception to prevent CPU stalling
        if (!isValidCode) throw UndefinedOpcode(instruction)
    }
}
