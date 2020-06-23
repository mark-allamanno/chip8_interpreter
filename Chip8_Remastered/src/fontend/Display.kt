package fontend

import backend.Chip8
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*


class Display constructor(upscale: Int, verbose: Boolean): JPanel(), KeyListener {

    private val frame = JFrame("backend.Chip8 Interpreter")
    private val processor = Chip8(this, verbose)
    private val upscaleRatio = upscale
    internal var currentKey: Char? = null

    init {
        // Initializes the size of the JPanel and add a key listener
        this.preferredSize = Dimension(64 * upscale,32 * upscale)
        this.isFocusable = true
        addKeyListener(this)
        // Initializes the close operation and location of the window
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocation(200, 100)
        // Adds the menu bar to the JFrame for GUI Rom loading
        frame.jMenuBar = Chip8Menu(this, processor)
        // Put the JPanel inside of the JFrame and pack them together
        frame.contentPane = this
        frame.pack()
        // Finally make it visible
        frame.isVisible = true
    }

    // We create a companion object tagged with the JVM static header so the display can self instance itself
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val display = Display(20, false)
        }
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
                g.fillRect(i * upscaleRatio, j * upscaleRatio, upscaleRatio, upscaleRatio)
            }
        }
    }
}