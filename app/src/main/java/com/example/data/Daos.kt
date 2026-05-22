package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId IS :parentFolderId")
    fun getFoldersInParent(parentFolderId: Long?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}

@Dao
interface NoteFileDao {
    @Query("SELECT * FROM note_files ORDER BY modifiedAt DESC")
    fun getAllNoteFiles(): Flow<List<NoteFileEntity>>

    @Query("SELECT * FROM note_files WHERE parentFolderId IS :parentFolderId ORDER BY modifiedAt DESC")
    fun getNoteFilesInParent(parentFolderId: Long?): Flow<List<NoteFileEntity>>

    @Query("SELECT * FROM note_files WHERE id = :id LIMIT 1")
    suspend fun getNoteFileById(id: Long): NoteFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteFile(noteFile: NoteFileEntity): Long

    @Update
    suspend fun updateNoteFile(noteFile: NoteFileEntity)

    @Delete
    suspend fun deleteNoteFile(noteFile: NoteFileEntity)
}

@Dao
interface PageAnnotationDao {
    @Query("SELECT * FROM page_annotations WHERE noteFileId = :noteFileId")
    fun getAnnotationsForNote(noteFileId: Long): Flow<List<PageAnnotationEntity>>

    @Query("SELECT * FROM page_annotations WHERE noteFileId = :noteFileId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getAnnotationForPage(noteFileId: Long, pageIndex: Int): PageAnnotationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAnnotation(annotation: PageAnnotationEntity)

    @Query("DELETE FROM page_annotations WHERE noteFileId = :noteFileId")
    suspend fun deleteAnnotationsForNote(noteFileId: Long)

    @Query("DELETE FROM page_annotations WHERE noteFileId = :noteFileId AND pageIndex = :pageIndex")
    suspend fun deleteAnnotationForPage(noteFileId: Long, pageIndex: Int)
}
