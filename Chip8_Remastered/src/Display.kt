import java.awt.Graphics
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer

class Display constructor(width: Int, height: Int, chip8: Chip8) : JPanel(),
    ActionListener, KeyListener {

    private val frame = JFrame("Chip8 Emulator")
    private var frameNumber = 0
    private val timer = Timer(10, this)
    private val processor = chip8

    init {
        // Initializes all of the important JFrame attributes
        frame.setLocation(300, 200)
        frame.setSize(width, height)
        frame.contentPane = this
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
        // Initializes all of the important JPanel attributes
        this.setSize(width, height)
        this.isVisible = true
        this.isFocusable = true
        addKeyListener(this)
        // Starts the timer for the animation panel
        timer.start()
    }

    // Override methods for the keyboard listener
    override fun keyPressed(e: KeyEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}

    override fun paintComponent(g: Graphics) {
        // Firstly call to the super method
        super.paintComponent(g)
        // Then we create a new buffered image whose dimensions are that of the panel
        val render = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        // And then we change the pixel values to reflect the sprite data in memory
        for (i in 0 until 64) {
            for (j in 0 until 32) {
                val color = if (processor.gfx[j][i] == 1) 255 else 0
                render.setRGB(i, j, color)
            }
        }
        // Finally we draw the image to the panel as our current frame
        g.drawImage(render, 0, 0, null)
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
    val chip8 = Chip8("Particle Demo.ch8", true)
    val screen = Display(1024, 512, chip8)
}