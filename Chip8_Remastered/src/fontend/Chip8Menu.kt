package fontend

import backend.Chip8
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

class Chip8Menu constructor(parent: JPanel, chip8: Chip8): JMenuBar() {

	private val file = JMenu("File")        // File Menu for the menu bar

	init {
		// Adds an open and exit function to the File menu
		file.add(RomChooser(parent, chip8))
		file.add(ExitInterpreter())
		this.add(file)
	}
}

class RomChooser constructor(container: JPanel, instance: Chip8): AbstractAction("Open") {

	// We need to have a File Chooser to select Roms a chip 8 instance and display to reference
	private val browser = JFileChooser(System.getProperty("user.dir"))
	private val chip8 = instance
	private val parent = container

	init {
		// Sets the File Choosers header and extension filter
		browser.dialogTitle = "Select your preferred ROM"
		browser.fileFilter = FileNameExtensionFilter("Chip8 Roms", "ch8")
	}

	override fun actionPerformed(e: ActionEvent) {
		// When the button is clicked open a File Chooser dialog and emulate the selected rom
		val option = browser.showOpenDialog(parent)
		if (option == JFileChooser.APPROVE_OPTION){
			val file = browser.selectedFile.absolutePath
			chip8.emulate(file)
		}
	}
}

class ExitInterpreter: AbstractAction("Exit") {

	override fun actionPerformed(p0: ActionEvent?) {
		// If the exit button is clicked then simply quit the program
		exitProcess(0)
	}
}