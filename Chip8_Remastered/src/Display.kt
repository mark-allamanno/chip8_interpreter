import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer

class Display constructor(upscale: Int, chip8: Chip8) : JPanel(),
    ActionListener, KeyListener {

    private val frame = JFrame("Chip8 Interpreter")
    private var frameNumber = 0
    private val timer = Timer(20, this)
    private val processor = chip8
    private val upscale = upscale

    init {
        // Initializes the size of the JPanel and add a key listener
        this.preferredSize = Dimension(64 * upscale,32 * upscale)
        addKeyListener(this)
        // Initializes the close operation and location of the window
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocation(200, 100)
        // Put the JPanel inside of the JFrame and pack them together
        frame.contentPane = this
        frame.pack()
        // Finally make it visible
        frame.isVisible = true
    }

    // Override methods for the keyboard listener
    override fun keyPressed(e: KeyEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}

    // Methods to start and end the emulation of the Chip8 CPU
    fun startEmulation() {timer.start()}
    fun endEmulation() {timer.stop()}

    override fun paintComponent(g: Graphics) {
        // Firstly call to the super method
        super.paintComponent(g)
        // And then we change the pixel values to reflect the sprite data in memory
        for (i in 0 until 64) {
            for (j in 0 until 32) {
                // Sets the color of the current pixel to be drawn
                if (processor.gfx[j][i] == 1) g.color = Color.WHITE else g.color = Color.BLACK
                // Draws the pixel at its corresponding location onscreen
                g.fillRect(i * upscale, j * upscale, upscale, upscale)
            }
        }
    }

    override fun paint(g: Graphics) {
        // Each timer we repaint we need to draw the current frame and keep track of the fram count
        paintComponent(g)
        frameNumber++
    }

    override fun actionPerformed(e: ActionEvent) {
        // On each repaint of the frame we first need to run a CPU cycle
        processor.emulateCycle()
        // Then we need to draw the corresponding frame for the cycle
        repaint()
    }
}

fun main() {
    // Create a new Chip8 object and start the emulation
    val chip8 = Chip8("roms/test_opcode.ch8", true)
    val screen = Display(20, chip8)
    screen.startEmulation()
}