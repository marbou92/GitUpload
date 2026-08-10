package com.gitupload

import android.app.Application
import com.gitupload.data.ai.AiAssistantManager
import com.gitupload.ui.theme.ThemeManager

/**
 * Application entry point. Initializes the two singleton managers that
 * need a [android.content.Context] to read/write their persisted state
 * (theme palette/mode and AI provider configuration) via DataStore.
 */
class GitUploadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(applicationContext)
        AiAssistantManager.init(applicationContext)
    }
}
