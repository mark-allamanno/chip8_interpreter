package backend

import fontend.Display
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.File
import java.io.IOException
import javax.swing.Timer
import kotlin.system.exitProcess

/*
    Author - Mark Allamanno
    Date - 18 June 2020

    This is one of my first Kotlin projects in which I seek to emulate a backend.Chip8 CPU. This is far from 100% accurate to the
    original specs as the original used bytes for registers and memory and shorts for the stack, I register, and program
    counter. However, I am unable to make a more accurate emulation as the tools in Kotlin are not suited for it. The simple
    fact that Kotlin (and the JVM for that matter) does not like unsigned values creates a whole host of problems with
    making an accurate emulation. So instead of making a mess over trying to make a 100% accurate emulation in terms of
    types I elected to make the project as organized, straightforward, and well documented as possible. Enjoy either for
    learning or review future me or someone else! Thanks for your time.
*/
class Chip8 constructor(screen: Display, verboseLog: Boolean): ActionListener {

    internal var registers = IntArray(0x10)                         // The 16 CPU registers
    internal var memory = IntArray(0x1000)                          // The 4k of onboard memory
    internal var stack = IntArray(0xC)                              // The 12 levels of stack nesting
    internal var gfx = Array(32) { IntArray(64) }                   // The memory we are using for the graphical data
    internal var delay = 0x3c                                       // The delay timer
    internal var sound = 0x3c                                       // The sound timer
    internal var opcode = 0x0                                       // The current opcode of the emulation
    internal var registerI = 0x0                                    // The special I register
    internal var pc = 0x200                                         // The program counter
    internal var sp = 0x0                                           // The stack pointer
    private val display = screen                                    // The display for the Chip 8 
    private val timer = Timer(5, this)                              // A timer to regulate the CPU cycle speed
    private val opcodeTable = OpcodeMap(this, display)              // The opcode table that we use for opcode execution
    private val debug = verboseLog                                  // The flag for the debug menu

    private fun loadFonts() {
        // The standard backend.Chip8 font set used for rendering
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
        }
        // Handles the situation where the user has specified a file that doesn't exist and helps to guide them
        catch (e: FileNotFoundException) {
            println("File not found! Did you type the right name?")
            println("Put the rom into the same directory as the executable and then specify the ROM name")
            exitProcess(1)
        }
        // Handles the situation where the api fails to read the specified file into memory
        catch (e: IOException) {
            println("There was an error when loading the specified ROM file into memory. Try again")
            exitProcess(1)
        }
        // Handles the situation where the user specifies a file tha is too large for the interpreter
        catch (e: IndexOutOfBoundsException) {
            println("ROM File is too big for the backend.Chip8, did you load the incorrect file?")
            println("Make sure your ROM file is less than ___ kilobytes")
            exitProcess(1)
        }
    }

    private fun resetState() {
        registers = IntArray(0x10)
        memory = IntArray(0x1000)
        stack = IntArray(0xC)
        gfx = Array(32) { IntArray(64) }
        delay = 0x3c
        sound = 0x3c
        opcode = 0x0
        registerI = 0x0
        pc = 0x200
        sp = 0x0
    }

    fun emulate(filename: String) {
        resetState()            // Zeros the state of the Chip8 instance
        loadFonts()             // Load the backend.Chip8 font set into memory
        loadRom(filename)       // Load the specified ROM into memory
        timer.start()           // Starts the timer for the display
    }

    private fun debugPrint() {
        // Print the current opcode and program counter position
        println(String.format("Opcode: %x", opcode))
        println(String.format("Program Counter: %x", pc))
        // Iterate over all of the registers and print their values in a line for help debugging
        print("Registers -> ")
        for (i in registers.indices) {
            print(String.format("V%x: ", i))
            print(String.format("%1x", registers[i]))
            print(" | ")
        }
        // Print two newline characters for readability between cpu cycles
        print("\n\n")
    }

    override fun actionPerformed(e: ActionEvent) {
        // Merge the two bytes at the program counter and the program counter + 1 to make the current opcode
        opcode = ((memory[pc] shl 8) and 0xFF00) or (memory[pc + 1] and 0xFF)
        // Use the opcode table to execute the current opcode
        opcodeTable.executeOpcode(opcode)
        // Render the current frame of the emulation after running a CPU cycle
        display.repaint()
        // Decrement the sound and delay timers for each frame drawn
        delay--; sound--
        // Print the debug menu to the console if the user wishes
        if (debug) debugPrint()
    }
}
