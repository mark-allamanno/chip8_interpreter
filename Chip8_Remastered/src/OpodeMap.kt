import java.lang.Integer.toHexString
import java.util.regex.Pattern.matches
import kotlin.math.pow

/*
    An interface for all opcodes to implement so they can be stored as a hash map in our main Chip8 emulation to
    sidestep using a gross big switch statement like in my preliminary implementation
*/
interface Opcode {
    fun execute(instance: Chip8)    // The method to be implemented to execute opcode functions
}

/*
    Below is the implementation of all of the opcode needed for the Chip8 interpretation in class form.
    They extend the opcode interface which grants them one method, execute, wherein they execute their specified
    action and then return. They are given the current Chip 8 instance as a parameter in order to manipulate
    class variables. What each class does to the Chip 8 instance is outline before and in each opcode's execute method
*/
internal class ClearScreen : Opcode {
    // Clears the screen of any pixel data held before the next request
    override fun execute(instance: Chip8) {
        for (i in 0 until 64)
            for (j in 0 until 32)
                instance.gfx[j][i] = 0
        instance.pc += 2
    }
}

internal class ReturnSubroutine : Opcode {
    // If the opcode is in the form 0x00EE then we need to return from a subroutine
    override fun execute(instance: Chip8) {
        instance.sp--
        instance.pc = instance.stack[instance.sp]
        instance.pc += 2
    }
}

internal class JumpAddress : Opcode {
    // If the opcode is in the form 1NNN then we jump to address NNN
    override fun execute(instance: Chip8) {
        instance.pc = instance.opcode and 0x0fff
    }
}

internal class JumpSubroutine : Opcode {
    // If the opcode is in the form 2NNN then we need to run the subroutine at NNN
    override fun execute(instance: Chip8) {
        instance.stack[instance.sp] = instance.pc
        instance.sp++
        instance.pc = instance.opcode and 0x0fff
    }
}

internal class SkipIfEqualValue : Opcode {
    // If the opcode is in the form 3XNN then we need to check if VX and NN are equal
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        val value = instance.opcode and 0x00ff
        instance.pc += if (instance.registers[index] == value) 4 else 2
    }
}

internal class SkipIfNotEqualValue : Opcode {
    // If the opcode is in the form 4XNN then we need to check if VX and NN are not equal
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        val value = instance.opcode and 0x00ff
        instance.pc += if (instance.registers[index] != value) 4 else 2
    }
}

internal class SkipIfEqualRegister : Opcode {
    // If the opcode is in the form 5XY0 then we need to check if the registers VX and VY are equal
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.pc += if (instance.registers[registerX] == instance.registers[registerY]) 4 else 2
    }
}

internal class StoreToRegister : Opcode {
    // If the opcode is in the form ____ then we need to store the value NN to the register VX
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        instance.registers[index] = instance.opcode and 0x00ff
        instance.pc += 2
    }
}

internal class IncrementRegisterValue : Opcode {
    // If the opcode is in the form ____ then we need to increment the value in VX by the value NN
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        instance.registers[index] += instance.opcode and 0x00ff
        instance.pc += 2
    }
}

internal class SetRegistersEqual : Opcode {
    // If the opcode is in the form 8XY0 then we need to set VX to VY
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.registers[registerX] = instance.registers[registerY]
        instance.pc += 2
    }
}

internal class BitwiseAndRegisters : Opcode {
    // If the opcode is in the form 8XY1 then we set VX equal to VX and VY
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.registers[registerX] = instance.registers[registerX] and instance.registers[registerY]
        instance.pc += 2
    }
}

internal class BitwiseOrRegisters : Opcode {
    // If the opcode is in the form 8XY2 then we set VX equal to VX or VY
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.registers[registerX] = instance.registers[registerX] or instance.registers[registerY]
        instance.pc += 2
    }
}

internal class BitwiseXorRegisters : Opcode {
    // If the opcode is in the form 8XY3 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.registers[registerX] = instance.registers[registerX] xor instance.registers[registerY]
        instance.pc += 2
    }
}

internal class IncrementRegister : Opcode {
    // If the opcode is in the form 8XY3 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.registers[registerX] += instance.registers[registerY]
        // Check if there is an integer overflow and set flags accordingly
        if (255 < instance.registers[registerX] + instance.registers[registerY]) {
            instance.registers[registerX] = instance.registers[registerX] and 0xff
            instance.registers[0xf] = 1
        }
        instance.pc += 2
    }
}

internal class DecrementRegister : Opcode {
    // If the opcode is in the form 8XY3 then we set VX equal to VX xor VY
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.registers[registerX] -= instance.registers[registerY]
        // Check to see if there is an integer underflow and set the flags accordingly
        if (instance.registers[registerX] < instance.registers[registerY]) {
            instance.registers[registerX] = instance.registers[registerX] and 0xff
            instance.registers[0xf] = 0
        }
        instance.pc += 2
    }
}

internal class BitwiseRightShift : Opcode {
    // If the opcode is in the form ____ then we need to shift VY right by one bit and store that bit into Vf
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        instance.registers[registerX] = instance.registers[registerX] ushr 1
        instance.registers[0xf] = instance.registers[registerX] and 0x0001
        instance.pc += 2
    }
}

internal class BitwiseLeftShift : Opcode {
    // If the opcode is in the form ____ then we need to shift VY right by one bit and store that but into Vf
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        instance.registers[registerX] = instance.registers[registerX] shl 1
        instance.registers[0xf] = instance.registers[registerX] and 0x8000 ushr 12
        instance.pc += 2
    }
}

internal class SkipIfNotEqualRegister : Opcode {
    // If the opcode is in the form ____ then we need to check if the registers VX and VY are not equal
    override fun execute(instance: Chip8) {
        val registerX = instance.opcode and 0x0f00 ushr 8
        val registerY = instance.opcode and 0x00f0 ushr 4
        instance.pc += if (instance.registers[registerX] != instance.registers[registerY]) 4 else 2
    }
}

internal class SaveAddressToI : Opcode {
    // If the opcode is in the form ANNN then we need to store the register NNN into the I register
    override fun execute(instance: Chip8) {
        instance.registerI = instance.opcode and 0x0fff
        instance.pc += 2
    }
}

internal class JumpToAddressSum : Opcode {
    // If the opcode is in the form BNNN then we jump to NNN summed with the value in register 0
    override fun execute(instance: Chip8) {
        instance.pc = (instance.opcode and 0x0fff) + instance.registers[0]
    }
}

internal class RandomNumMask : Opcode {
    // If the opcode is in the form CXNN then we store a random number between 00-ff masked with NN into VX
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        val random = (0..255).random()
        val mask = instance.opcode and 0x00ff
        instance.registers[index] = random and mask
        instance.pc += 2
    }
}

internal class DrawSprite : Opcode {
    // Draw Pixel Opcode
    override fun execute(instance: Chip8) {
        instance.pc += 2
    }
}

internal class StoreDelayToRegister : Opcode {
    // If the opcode is in the form ____ then we need to save the value of the delay register to VX
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        instance.registers[index] = instance.delay
        instance.pc += 2
    }
}

internal class SetDelayToRegister : Opcode {
    // If the opcode is in the form ____ then we need to set the delay register to VX
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        instance.delay = instance.registers[index]
        instance.pc += 2
    }
}

internal class SetSoundToRegister : Opcode {
    // If the opcode is in the form ____ then we need to set the sound timer to the value in VX
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        instance.sound = instance.registers[index]
        instance.pc += 2
    }
}

internal class AddRegisterToSpecial : Opcode {
    // If the opcode is in the form ____ then we need to increment the I register by VX
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0x0f00 ushr 8
        instance.registerI += instance.registers[index]
        instance.pc += 2
    }
}

internal class JumpToSpriteData : Opcode {
    // Draw Pixel Opcode
    override fun execute(instance: Chip8) {
        val index = instance.opcode and 0xf00 ushr 8
        instance.registerI = index * 5
        instance.pc += 2
    }
}

internal class BinaryCodedDecimal : Opcode {
    // Draw Pixel Opcode
    override fun execute(instance: Chip8) {
        val regIndex = instance.opcode and 0x0f00 ushr 8
        for (i in 0..2)
            instance.memory[instance.registerI + i] = instance.registers[regIndex] % 10.0.pow(i.toDouble()).toInt()
        instance.pc += 2
    }
}

internal class RegisterDump : Opcode {
    // If the opcode is in the form ____ then we need to dump the contents of V0-VX into memory
    override fun execute(instance: Chip8) {
        val regIndex = instance.opcode and 0x0f00 ushr 8
        for (i in 0..regIndex)
            instance.memory[instance.registerI + i] = instance.registers[i] and 0xff
        instance.registerI += regIndex + 1
        instance.pc += 2
    }
}

internal class RegisterLoad : Opcode {
    // If the opcode is in the form ____ then we need to load values from memory into V0-VX
    override fun execute(instance: Chip8) {
        val regIndex = instance.opcode and 0x0f00 ushr 8
        for (i in 0..regIndex)
            instance.registers[i] = instance.memory[instance.registerI + i] and 0xff
        instance.registerI += regIndex + 1
        instance.pc += 2
    }
}

/*
    Now that we have implemented all of the opcodes we need to organize them into a hashmap
    to be abe to cleanly access them from the Chip8 class itself. This is best abstracted by
    creating a class whose purpose is to collect all of these classes and organize them into such
    a hashmap.
*/
class OpcodeMap : HashMap<String, Opcode>() {

    // The array of regex compliant strings to match against opcodes
    private val regexKeys: Array<String>

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
        this["8..e"] = BitwiseLeftShift()
        this["9..0"] = SkipIfNotEqualRegister()
        this["a..."] = SaveAddressToI()
        this["b..."] = JumpToAddressSum()
        this["c..."] = RandomNumMask()
        this["d..."] = DrawSprite()
        this["f.07"] = StoreDelayToRegister()
        this["f.15"] = SetDelayToRegister()
        this["f.18"] = SetSoundToRegister()
        this["f.1e"] = AddRegisterToSpecial()
        this["f.29"] = JumpToSpriteData()
        this["f.33"] = BinaryCodedDecimal()
        this["f.55"] = RegisterDump()
        this["f.65"] = RegisterLoad()
        // Initializes the regexKeys to the regex compliant keys of the HashMap
        regexKeys = keys.toTypedArray()
    }

    fun functionFromString(instruction: Int): Opcode? {
        // Convert the opcode to a hex string for comparisons
        val opcode = toHexString(instruction)
        // Iterate over the regex keys and check to see what opcode it matches
        for (test in regexKeys)
            if (matches(test, opcode)) return this[test]
        // If we cannot find a matching opcode then return null
        return null
    }
}