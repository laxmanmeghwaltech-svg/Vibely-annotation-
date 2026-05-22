package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentFolderId"])]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null // null means in root folder
)

@Entity(
    tableName = "note_files",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentFolderId"])]
)
data class NoteFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null, // null means in root folder
    val isPdf: Boolean,
    val localPath: String, // local file path in sandbox container
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1
)

@Entity(
    tableName = "page_annotations",
    primaryKeys = ["noteFileId", "pageIndex"],
    indices = [Index(value = ["noteFileId"])]
)
data class PageAnnotationEntity(
    val noteFileId: Long,
    val pageIndex: Int,
    val strokeListJson: String,  // Serialized List<CanvasStroke>
    val objectListJson: String   // Serialized List<ThreeDObject>
)
