package com.supremeai.ide.learning

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Project component that manages document listeners for learning from user code edits.
 * This component registers itself with FileEditorManager to track when files are opened/closed
 * and attaches document listeners to track code changes for SupremeAI learning.
 */
class UserCodeLearningProjectComponent(private val project: Project) : 
    ProjectComponent,
    FileEditorManagerListener {

    private val documentListeners = mutableMapOf<VirtualFile, UserCodeChangeListener>()

    init {
        // Register for file opening/closing events
        FileEditorManager.getInstance(project).addFileEditorManagerListener(this)
    }

    override fun projectOpened() {
        // Component initialized
    }

    override fun projectClosed() {
        // Clean up listeners when project closes
        FileEditorManager.getInstance(project).removeFileEditorManagerListener(this)
        removeAllListeners()
    }

    /**
     * Called when a file is opened in the editor.
     */
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        attachDocumentListenerIfNeeded(file)
    }

    /**
     * Called when a file is closed in the editor.
     */
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        removeDocumentListener(file)
    }

    /**
     * Called when file selection changes (e.g., switching tabs).
     */
    override fun selectionChanged(event: FileEditorManagerEvent) {
        // No action needed
    }

    /**
     * Attaches a document listener to track changes in a file.
     */
    private fun attachDocumentListenerIfNeeded(file: VirtualFile) {
        // Don't attach if we already have a listener for this file
        if (documentListeners.containsKey(file)) {
            return
        }

        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document == null) return

        // Create and attach our custom listener
        val listener = UserCodeChangeListener(project)
        document.addDocumentListener(listener)
        documentListeners[file] = listener
    }

    /**
     * Removes the document listener from a file.
     */
    private fun removeDocumentListener(file: VirtualFile) {
        val listener = documentListeners.remove(file)
        val document = listener?.let { FileDocumentManager.getInstance().getDocument(file) }
        document?.removeDocumentListener(listener)
    }

    /**
     * Removes all document listeners.
     */
    private fun removeAllListeners() {
        documentListeners.keys.toList().forEach { removeDocumentListener(it) }
    }
}