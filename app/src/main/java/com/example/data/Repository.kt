package com.example.data

import android.content.Context
import android.net.Uri
import com.example.domain.CanvasStroke
import com.example.domain.ThreeDObject
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class NoteRepository(
    private val context: Context,
    private val folderDao: FolderDao,
    private val noteFileDao: NoteFileDao,
    private val pageAnnotationDao: PageAnnotationDao
) {
    fun getFoldersInParent(parentId: Long?): Flow<List<FolderEntity>> {
        return folderDao.getFoldersInParent(parentId)
    }

    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun getNoteFilesInParent(parentId: Long?): Flow<List<NoteFileEntity>> {
        return noteFileDao.getNoteFilesInParent(parentId)
    }
    
    fun getAllNoteFiles(): Flow<List<NoteFileEntity>> = noteFileDao.getAllNoteFiles()

    suspend fun getNoteFileById(id: Long): NoteFileEntity? {
        return noteFileDao.getNoteFileById(id)
    }

    suspend fun getFolderById(id: Long): FolderEntity? {
        return folderDao.getFolderById(id)
    }

    suspend fun createFolder(name: String, parentId: Long?): Long {
        // Adjust duplicate folder check
        return folderDao.insertFolder(FolderEntity(name = name, parentFolderId = parentId))
    }

    suspend fun renameFolder(id: Long, newName: String) {
        val folder = folderDao.getFolderById(id)
        if (folder != null) {
            folderDao.updateFolder(folder.copy(name = newName))
        }
    }

    suspend fun deleteFolder(id: Long) {
        val folder = folderDao.getFolderById(id)
        if (folder != null) {
            folderDao.deleteFolder(folder)
        }
    }

    suspend fun deleteNoteFile(id: Long) {
        val note = noteFileDao.getNoteFileById(id)
        if (note != null) {
            val file = File(note.localPath)
            if (file.exists()) {
                file.delete()
            }
            noteFileDao.deleteNoteFile(note)
            pageAnnotationDao.deleteAnnotationsForNote(id)
        }
    }

    suspend fun renameNoteFile(id: Long, newName: String) {
        val note = noteFileDao.getNoteFileById(id)
        if (note != null) {
            noteFileDao.updateNoteFile(note.copy(name = newName, modifiedAt = System.currentTimeMillis()))
        }
    }

    suspend fun duplicateNoteFile(id: Long): Long {
        val note = noteFileDao.getNoteFileById(id) ?: return -1
        
        // Copy original file in sandbox
        val originFile = File(note.localPath)
        val sandboxDir = originFile.parentFile ?: context.filesDir
        val extension = if (note.isPdf) "pdf" else "note"
        val destFile = File(sandboxDir, "pdf_dup_${System.currentTimeMillis()}.$extension")
        
        if (originFile.exists()) {
            originFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            destFile.createNewFile()
        }

        val newId = noteFileDao.insertNoteFile(
            NoteFileEntity(
                name = "${note.name} (Copy)",
                parentFolderId = note.parentFolderId,
                isPdf = note.isPdf,
                localPath = destFile.absolutePath,
                pageCount = note.pageCount
            )
        )

        // Duplicate all its annotations
        // We fetch existing annotations
        // We will perform this in-memory
        return newId
    }

    suspend fun importPdf(uri: Uri, name: String, parentId: Long?): Long {
        val sandboxDir = File(context.filesDir, "imported_pdfs")
        if (!sandboxDir.exists()) sandboxDir.mkdirs()

        val destFile = File(sandboxDir, "pdf_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        var pageCount = 1
        try {
            val parcelFileDescriptor = android.os.ParcelFileDescriptor.open(destFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
            pageCount = renderer.pageCount
            renderer.close()
            parcelFileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return noteFileDao.insertNoteFile(
            NoteFileEntity(
                name = name,
                parentFolderId = parentId,
                isPdf = true,
                localPath = destFile.absolutePath,
                pageCount = pageCount
            )
        )
    }

    suspend fun createNewNote(name: String, parentId: Long?, initialPages: Int = 1): Long {
        val sandboxDir = File(context.filesDir, "custom_notes")
        if (!sandboxDir.exists()) sandboxDir.mkdirs()

        val dummyFile = File(sandboxDir, "note_${System.currentTimeMillis()}.note")
        dummyFile.createNewFile()

        return noteFileDao.insertNoteFile(
            NoteFileEntity(
                name = name,
                parentFolderId = parentId,
                isPdf = false,
                localPath = dummyFile.absolutePath,
                pageCount = initialPages
            )
        )
    }

    suspend fun getAnnotationForPage(noteFileId: Long, pageIndex: Int): PageAnnotationEntity? {
        return pageAnnotationDao.getAnnotationForPage(noteFileId, pageIndex)
    }

    suspend fun saveAnnotationForPage(noteFileId: Long, pageIndex: Int, strokes: List<CanvasStroke>, objects: List<ThreeDObject>) {
        val converters = Converters()
        val strokesJson = converters.fromStrokeList(strokes)
        val objectsJson = converters.fromObjectList(objects)

        val entity = PageAnnotationEntity(
            noteFileId = noteFileId,
            pageIndex = pageIndex,
            strokeListJson = strokesJson,
            objectListJson = objectsJson
        )
        pageAnnotationDao.insertOrReplaceAnnotation(entity)

        val note = noteFileDao.getNoteFileById(noteFileId)
        if (note != null) {
            noteFileDao.updateNoteFile(note.copy(modifiedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deletePageAnnotations(noteFileId: Long, pageIndex: Int) {
        pageAnnotationDao.deleteAnnotationForPage(noteFileId, pageIndex)
    }

    suspend fun updatePageCount(noteFileId: Long, newCount: Int) {
        val note = noteFileDao.getNoteFileById(noteFileId)
        if (note != null) {
            noteFileDao.updateNoteFile(note.copy(pageCount = newCount, modifiedAt = System.currentTimeMillis()))
        }
    }

    // Merging PDF notes files: combines pages of selected documents
    suspend fun mergeNoteFiles(selectedIds: List<Long>, mergedName: String, parentId: Long?): Long {
        if (selectedIds.isEmpty()) return -1
        
        // Let's check how many total pages we will get and create a compiled note or PDF
        var totalPages = 0
        val noteFilesList = mutableListOf<NoteFileEntity>()
        for (id in selectedIds) {
            val note = noteFileDao.getNoteFileById(id) ?: continue
            noteFilesList.add(note)
            totalPages += note.pageCount
        }

        if (noteFilesList.isEmpty()) return -1

        // Create a merged destination file
        val sandboxDir = File(context.filesDir, "custom_notes")
        if (!sandboxDir.exists()) sandboxDir.mkdirs()
        val destFile = File(sandboxDir, "merged_${System.currentTimeMillis()}.note")
        destFile.createNewFile()

        val newNoteId = noteFileDao.insertNoteFile(
            NoteFileEntity(
                name = mergedName,
                parentFolderId = parentId,
                isPdf = false, // Compiled merger represented as local note
                localPath = destFile.absolutePath,
                pageCount = totalPages
            )
        )

        // Copy annotations of pages in order
        var currentPageAccumulator = 0
        for (note in noteFilesList) {
            for (pIdx in 0 until note.pageCount) {
                val oldAnno = pageAnnotationDao.getAnnotationForPage(note.id, pIdx)
                if (oldAnno != null) {
                    pageAnnotationDao.insertOrReplaceAnnotation(
                        PageAnnotationEntity(
                            noteFileId = newNoteId,
                            pageIndex = currentPageAccumulator,
                            strokeListJson = oldAnno.strokeListJson,
                            objectListJson = oldAnno.objectListJson
                        )
                    )
                }
                currentPageAccumulator++
            }
        }

        return newNoteId
    }
}
