import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.File
import java.lang.Integer.toHexString
import kotlin.system.exitProcess

/*
    Author - Mark Allamanno
    Date - 18 June 2020

    This is one of my first Kotlin projects in which I seek to emulate a Chip8 CPU. This is far from 100% accurate to the
    original specs as the original used bytes for registers and memory and shorts for the stack, I register, and program
    counter. However, I am unable to make a more accurate emulation as the tools in Kotlin are not suited for it. The simple
    fact that Kotlin (and the JVM for that matter) does not like unsigned values creates a whole host of problems with
    making an accurate emulation. So instead of making a mess over trying to make a 100% accurate emulation in terms of
    types I elected to make the project as organized, straightforward, and well documented as possible. Enjoy either for
    learning or review future me or someone else! Thanks for your time.
*/

class Chip8 constructor(verboseLog: Boolean){

    var registers: IntArray = IntArray(0x10)        // The 16 CPU registers
    var memory: IntArray = IntArray(0x1000)         // The 4k of onboard memory
    var stack: IntArray = IntArray(0xC)             // The 12 levels of stack nesting
    var delay: Int = 0x3C                               // The delay timer
    var sound: Int = 0x3C                               // The sound timer
    var opcode: Int = 0x0                               // The current opcode of the emulation
    var registerI: Int = 0x0                            // The special I register
    var pc: Int = 0x200                                 // The program counter
    var sp: Int = 0x0                                   // The stack pointer
    private var opcodeTable = OpcodeMap()               // The opcode table that we use for opcode execution
    private val debug = verboseLog                      // The flag for the debug meu

    private fun loadRom(filename: String) {
        try {
            // Attempts to load the rom via a file input stream
            val rom = FileInputStream(filename)
            // Initializes a byte array to the length of the input file
            val romSize = File(filename)
            val data = ByteArray(romSize.length().toInt())
            // Reads the input stream into this byte array for storage
            rom.read(data)
            // Then reads this byte array into our memory array for use in the emulator
            for (i in data.indices) memory[0x200 + i] = data[i].toInt()
        } catch (e: FileNotFoundException) {
            // Handles the situation where the user has specified a file that doesn't exist and helps to guide them
            println("File not found! Did you type the right name")
            println("Put the rom into the same directory as the executable and then specify the ROM name")
            exitProcess(1)
        } catch (e: IOException) {
            // Handles the situation where reading the file does wrong and prints the stack trace
            println("Error occurred while reading the ROM")
            e.printStackTrace(System.out)
            exitProcess(1)
        }
    }

    private fun loadFonts() {
        // The standard Chip8 font set used for rendering
        val fontSet = intArrayOf(
                0xF0, 0x90, 0x90, 0x90, 0xF0,  // 0
                0x20, 0x60, 0x20, 0x20, 0x70,  // 1
                0xF0, 0x10, 0xF0, 0x80, 0xF0,  // 2
                0xF0, 0x10, 0xF0, 0x10, 0xF0,  // 3
                0x90, 0x90, 0xF0, 0x10, 0x10,  // 4
                0xF0, 0x80, 0xF0, 0x10, 0xF0,  // 5
                0xF0, 0x80, 0xF0, 0x90, 0xF0,  // 6
                0xF0, 0x10, 0x20, 0x40, 0x40,  // 7
                0xF0, 0x90, 0xF0, 0x90, 0xF0,  // 8
                0xF0, 0x90, 0xF0, 0x10, 0xF0,  // 9
                0xF0, 0x90, 0xF0, 0x90, 0x90,  // A
                0xE0, 0x90, 0xE0, 0x90, 0xE0,  // B
                0xF0, 0x80, 0x80, 0x80, 0xF0,  // C
                0xE0, 0x90, 0x90, 0x90, 0xE0,  // D
                0xF0, 0x80, 0xF0, 0x80, 0xF0,  // E
                0xF0, 0x80, 0xF0, 0x80, 0x80   // F
        )
        // Loads each font into the system memory for use in the program
        for (i in fontSet.indices) memory[i] = fontSet[i]
    }

    private fun debugPrint() {
        // Print the current opcode and program counter position
        println(String.format("Opcode: %x", opcode))
        println(String.format("Program Counter: %x", pc))
        // Iterate over all of the registers and print their values in a line for help debuging
        print("Registers -> ")
        for (i in registers.indices) {
            print(String.format("V%x: ", i))
            print(String.format("%1x", registers[i]))
            print(" | ")
        }
        // Print two newline characters for readability between cpu cycles
        print("\n\n")
    }

    private fun executeOpcode() {
        // Merge the two bytes at the program counter and the program counter + 1 to make the current opcode
        opcode = ((memory[pc] shl 8) and 0xFF00) or (memory[pc + 1] and 0xFF)
        // Attempt to execute the current opcode and sleep the thread after execution
        try {
            val function = opcodeTable.functionFromString(opcode)!!
            function.execute(this)
            Thread.sleep(40)
        }
        // Catch the exception where there is no corresponding opcode for the instruction given
        catch (e: NullPointerException) {
            print("There was an undefined opcode: ${toHexString(opcode)}")
            exitProcess(1)
        }
        // Catch the exception were the thread is interrupted while attempting to sleep
        catch (e: InterruptedException) {
            print("Thread sleep was interrupted")
            exitProcess(1)
        }
    }

    fun emulate(gameRom: String) {
        // Sets up the Chip8 emulation by loading the font set and roms into memory
        loadFonts()
        loadRom(gameRom)
        // Continuously execute opcodes and print to the debug menu if the user wishes
        while (true) {
            executeOpcode()
            if(debug) debugPrint()
        }
    }
}

fun main() {
    // Create a new Chip8 object and start the emulation
    val chip8 = Chip8(true)
    chip8.emulate("test_opcode.ch8")
}
