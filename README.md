# Chip8_Remastered

A simple Chip8 interpreter. Simply compile and run the file "Display.kt" in the frontend package to start the program. Once started select a ROM by clicking on the "File" menu and selecting the "Open" submenu which will open a file chooser window to select a ROM file. Once a ROM file is selected the emulation will begin. Important note about controls is since the original Chip 8 used a 16 button keypad to control the system we need to emulate that. The mappings are given below with the QWERTY keyboard input on the left and Chip 8 mapping on the right:

'1' = 0 | 
'2' = 1 | 
'3' = 3 | 
'4' = 4 | 
'Q' = 5 | 
'W' = 6 | 
'E' = 7 | 
'R' = 8 | 
'A' = 9 | 
'S' = 10 | 
'D' = 11 | 
'F' = 12 | 
'Z' = 13 | 
'X' = 14 | 
'C' = 15 | 
'V' = 16 | 

Note: this is not a native Kotlin implementation, it is meant to be run on the JVM with Java dependencies such as swing.
