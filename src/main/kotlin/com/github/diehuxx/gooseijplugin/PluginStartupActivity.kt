package com.github.diehuxx.gooseijplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.Messages
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager


class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!isGooseInstalled()) {
            // If 'goose' is not found, show error message and prompt for installation
            val installUrl = "https://github.com/square/goose"
            ApplicationManager.getApplication().invokeLater {
                val result = Messages.showYesNoDialog(
                    project,
                    "goose is required to be installed. Do you want to install it?",
                    "Install goose",
                    "Install",
                    "Cancel",
                    Messages.getQuestionIcon()
                )

                if (result == Messages.YES) {
                    BrowserUtil.browse(installUrl)
                }
            }
        } else {
            // Flash notification
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Goose Notifications")
                .createNotification("goose agent starting, this may take a minute.. ⏰", NotificationType.INFORMATION)
                .notify(project)
        }
    }

    private fun isGooseInstalled(): Boolean {
        try {
            // Try to execute 'goose' command
            val commandLine = GeneralCommandLine("goose")
            val processHandler = OSProcessHandler(commandLine)
            processHandler.startNotify()
            processHandler.waitFor()
        } catch (error: ExecutionException) {
            return false
        }
        return true
    }
}
