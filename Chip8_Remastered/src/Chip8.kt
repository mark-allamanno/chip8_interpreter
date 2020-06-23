import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.File
import java.io.IOException
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
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
class Chip8 constructor(upscale: Int, verboseLog: Boolean) : JPanel(),
        KeyListener, ActionListener {

    internal var registers = IntArray(0x10)                     // The 16 CPU registers
    internal var memory = IntArray(0x1000)                      // The 4k of onboard memory
    internal var stack = IntArray(0xC)                          // The 12 levels of stack nesting
    internal var gfx = Array(32) { IntArray(64) }          // The memory we are using for the graphical data
    internal var delay = 0x3c                                       // The delay timer
    internal var sound = 0x3c                                       // The sound timer
    internal var opcode = 0x0                                       // The current opcode of the emulation
    internal var registerI = 0x0                                    // The special I register
    internal var pc = 0x200                                         // The program counter
    internal var sp = 0x0                                           // The stack pointer
    private val opcodeTable = OpcodeMap(this)               // The opcode table that we use for opcode execution
    private val debug = verboseLog                                  // The flag for the debug menu

    private val frame = JFrame("Chip8 Interpreter")            // A frame to hold the JPanel
    private val timer = Timer(17, this)                // A timer to regulate the CPU cycle speed
    private val upscaleRatio = upscale                              // A factor to upscale the original frame by
    internal var currentKey: Char? = null                           // The current character being input by the user

    init {
        // Initializes the size of the JPanel and add a key listener
        this.preferredSize = Dimension(64 * upscale, 32 * upscale)
        this.isFocusable = true
        addKeyListener(this)
        // Initializes the close operation and location of the window
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocation(200, 100)
        // Put the JPanel inside of the JFrame and pack them together and make it all visible
        frame.contentPane = this
        frame.pack()
        frame.isVisible = true
        // Finally load the Chip8 font set into memory
        loadFonts()
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

    fun emulate(filename: String) {
        loadRom(filename)       // Load the specified ROM into memory
        timer.start()           // Starts the timer for the display
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
            println("ROM File is too big for the Chip8, did you load the incorrect file?")
            println("Make sure your ROM file is less than ___ kilobytes")
            exitProcess(1)
        }
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
        this.repaint()
        // Decrement the sound and delay timers for each frame drawn
        delay -= 1; sound -= 1
        // Print the debug menu to the console if the user wishes
        if (debug) debugPrint()
    }

    // When a key is pressed we store its character into the class variable and if its released reset it
    override fun keyPressed(e: KeyEvent) { currentKey = e.keyChar }
    override fun keyReleased(e: KeyEvent) { currentKey = null }
    override fun keyTyped(e: KeyEvent) { currentKey = e.keyChar }

    override fun paintComponent(g: Graphics) {
        // Firstly call to the super method
        super.paintComponent(g)
        // And then we change the pixel values to reflect the sprite data in memory
        for (i in 0 until 64) {
            for (j in 0 until 32) {
                // Sets the color of the current pixel to be drawn
                if (gfx[j][i] == 1) g.color = Color.WHITE else g.color = Color.BLACK
                // Draws the pixel at its corresponding location onscreen
                g.fillRect(i * upscaleRatio, j * upscaleRatio, upscaleRatio, upscaleRatio)
            }
        }
    }
}

fun main(args: Array<String>) {
    // Create a new Chip8 object and start the emulation
    val chip8 = Chip8(25, true)
    chip8.emulate("roms/pong.ch8")
}