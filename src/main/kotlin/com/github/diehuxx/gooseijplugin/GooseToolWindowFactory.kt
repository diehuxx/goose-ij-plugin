package com.github.diehuxx.gooseijplugin
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.vfs.VfsUtilCore
import java.io.OutputStreamWriter
import java.io.IOException
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JScrollPane
import java.awt.BorderLayout
import java.io.File

class GooseToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Create a panel to hold the console output area and input field
        val panel = JPanel(BorderLayout())

        // Create a JTextArea to display output from Goose
        val outputArea = JTextArea()
        outputArea.isEditable = false // Make the output area read-only
        val scrollPane = JScrollPane(outputArea) // Add scroll support for large output

        // Create an input field for user interaction
        val inputField = JTextField()

        // Add the output area (inside a scroll pane) and input field to the panel
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(inputField, BorderLayout.SOUTH)

        // Attach the panel to the tool window content
        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // Start the goose session and connect the console output and input field
        startGooseSession(project, outputArea, inputField)
    }

    private fun startGooseSession(project: Project, outputArea: JTextArea, inputField: JTextField) {
        // Command to start Goose session
        val commandLine = GeneralCommandLine("goose", "session", "start")

        try {
            // Initialize the process handler for Goose
            val processHandler = OSProcessHandler(commandLine)

            // Listen for process output and display it in the JTextArea
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val outputText = event.text
                    val contentType = if (outputType === ProcessOutputTypes.STDOUT) {
                        ConsoleViewContentType.NORMAL_OUTPUT
                    } else {
                        ConsoleViewContentType.ERROR_OUTPUT
                    }

                    // Append the output text to the JTextArea
                    outputArea.append(outputText)
                    outputArea.caretPosition = outputArea.document.length // Scroll to the bottom
                }
            })

            processHandler.startNotify()

            // Handle user input and send it to Goose
            val writer = OutputStreamWriter(processHandler.processInput)
            inputField.addActionListener {
                val userInput = inputField.text
                inputField.text = ""  // Clear input field after submission
                writer.write("$userInput\n")
                writer.flush()  // Send input to Goose
            }

        } catch (error: IOException) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Goose Notifications")
                .createNotification("Failed to start goose: ${error.message}", NotificationType.ERROR)
                .notify(project)
        }
    }
}
