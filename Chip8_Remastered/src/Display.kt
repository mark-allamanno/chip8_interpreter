import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.security.Key
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer


class Display constructor(upscale: Int, chip8: Chip8): JPanel(), KeyListener {

    private val frame = JFrame("Chip8 Interpreter")
    private var frameNumber = 0
    private val processor = chip8
    private val upscale = upscale
    internal var currentKey: Char? = null

    init {
        // Initializes the size of the JPanel and add a key listener
        this.preferredSize = Dimension(64 * upscale,32 * upscale)
        this.isFocusable = true
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
}