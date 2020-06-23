package fontend

import backend.Chip8
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

class Chip8Menu constructor(parent: JPanel, chip8: Chip8): JMenuBar() {

	private val file = JMenu("File")

	init {
		file.add(RomChooser(parent, chip8))
		file.add(ExitInterpreter())
		this.add(file)
	}
}

class RomChooser constructor(container: JPanel, instance: Chip8): AbstractAction("Open") {

	private val browser = JFileChooser(System.getProperty("user.dir"))
	private val chip8 = instance
	private val parent = container

	init {
		browser.dialogTitle = "Select your preferred ROM"
		browser.fileFilter = FileNameExtensionFilter("Chip8 Roms", "ch8")
	}

	override fun actionPerformed(e: ActionEvent) {
		val option = browser.showOpenDialog(parent)
		if (option == JFileChooser.APPROVE_OPTION){
			val file = browser.selectedFile.absolutePath
			chip8.emulate(file)
		}
	}
}

class ExitInterpreter: AbstractAction("Exit") {

	override fun actionPerformed(p0: ActionEvent?) {
		exitProcess(0)
	}
}