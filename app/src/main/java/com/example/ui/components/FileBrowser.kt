package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FolderEntity
import com.example.data.NoteFileEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileBrowser(
    currentFolderId: Long?,
    foldersList: List<FolderEntity>,
    filesList: List<NoteFileEntity>,
    allFoldersList: List<FolderEntity>,
    activeNoteId: Long?,
    onNavigateToFolder: (Long?) -> Unit,
    onMakeFolder: (String) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onImportPdf: (Uri, String) -> Unit,
    onMakeNewNote: (String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onRenameNote: (Long, String) -> Unit,
    onDuplicateNote: (Long) -> Unit,
    onOpenNote: (NoteFileEntity) -> Unit,
    onMergeSelectedNotes: (List<Long>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteNameInput by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetId by remember { mutableStateOf<Long?>(null) }
    var renameIsFolder by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    // Merging mode states
    var isMergeMode by remember { mutableStateOf(false) }
    val selectedNotesForMerge = remember { mutableStateListOf<Long>() }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergedFileNameInput by remember { mutableStateOf("") }

    // Activity launcher for choosing external PDF file offline
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportPdf(uri, "Imported PDF Notebook")
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(Color(0xFF1E293B)) // Primary Sidebar Surface (30%)
            .border(width = 1.dp, color = Color(0xFF334155))
            .padding(16.dp)
    ) {
        // Folder Explorer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Libraries",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Toggle merging operations
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        isMergeMode = !isMergeMode
                        selectedNotesForMerge.clear()
                    }
                ) {
                    Icon(
                        imageVector = if (isMergeMode) Icons.Default.Close else Icons.Default.MergeType,
                        contentDescription = "Merge Mode Toggle",
                        tint = if (isMergeMode) Color.Red else Color(0xFF06B6D4)
                    )
                }
                
                IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Import PDF", tint = Color(0xFF06B6D4))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Breadcrumbs
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Root",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (currentFolderId == null) Color(0xFF06B6D4) else Color(0xFF94A3B8),
                modifier = Modifier.combinedClickable(onClick = { onNavigateToFolder(null) })
            )
            
            if (currentFolderId != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = "crumb", tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                val activeFolder = allFoldersList.find { it.id == currentFolderId }
                Text(
                    text = activeFolder?.name ?: "Folder",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF06B6D4),
                    maxLines = 1
                )
                
                // Go back button inside breadcrumb bounds
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onNavigateToFolder(activeFolder?.parentFolderId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick File Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showFolderDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3748)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "+ Folder", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Folder", fontSize = 11.sp)
            }

            Button(
                onClick = { showNoteDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3748)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.PostAdd, contentDescription = "+ Note", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Notebook", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isMergeMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2563EB).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedNotesForMerge.size} selected to compile",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = {
                        if (selectedNotesForMerge.isNotEmpty()) {
                            mergedFileNameInput = "Compiled Merge Notebook"
                            showMergeDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    enabled = selectedNotesForMerge.isNotEmpty()
                ) {
                    Text("Merge Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Folders and Notes listing
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            // FOLDERS LAYER
            items(foldersList) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onNavigateToFolder(folder.id) },
                            onLongClick = {
                                renameTargetId = folder.id
                                renameIsFolder = true
                                renameInput = folder.name
                                showRenameDialog = true
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Subfolders", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = { onDeleteFolder(folder.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // NOTEFILES / PDFS LAYER
            items(filesList) { file ->
                val isActive = file.id == activeNoteId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) Color(0xFF2563EB).copy(alpha = 0.25f) else Color.Transparent)
                        .combinedClickable(
                            onClick = {
                                if (isMergeMode) {
                                    if (selectedNotesForMerge.contains(file.id)) {
                                        selectedNotesForMerge.remove(file.id)
                                    } else {
                                        selectedNotesForMerge.add(file.id)
                                    }
                                } else {
                                    onOpenNote(file)
                                }
                            },
                            onLongClick = {
                                renameTargetId = file.id
                                renameIsFolder = false
                                renameInput = file.name
                                showRenameDialog = true
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMergeMode) {
                        Checkbox(
                            checked = selectedNotesForMerge.contains(file.id),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedNotesForMerge.add(file.id)
                                } else {
                                    selectedNotesForMerge.remove(file.id)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF06B6D4),
                                uncheckedColor = Color(0xFF64748B)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Icon(
                        imageVector = if (file.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                        contentDescription = "Document",
                        tint = if (file.isPdf) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${file.pageCount} ${if (file.isPdf) "PDF pages" else "compiled pages"}", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onDuplicateNote(file.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { onDeleteNote(file.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }

    // Creating Subfolder dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Assemble New Subfolder", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Folder Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF06B6D4),
                        focusedBorderColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            onMakeFolder(folderNameInput)
                            folderNameInput = ""
                            showFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Build Folder")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Creating Custom Notebook dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Initiate Premium Notebook", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = noteNameInput,
                    onValueChange = { noteNameInput = it },
                    label = { Text("Notebook Label") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF06B6D4),
                        focusedBorderColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteNameInput.isNotBlank()) {
                            onMakeNewNote(noteNameInput)
                            noteNameInput = ""
                            showNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Assemble Notebook")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Rename Entity dialog (Folders & PDF Notebooks)
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Modify Label", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Identifier") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF06B6D4),
                        focusedBorderColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = renameTargetId
                        if (renameInput.isNotBlank() && targetId != null) {
                            if (renameIsFolder) {
                                onRenameFolder(targetId, renameInput)
                            } else {
                                onRenameNote(targetId, renameInput)
                            }
                            renameInput = ""
                            renameTargetId = null
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Apply Change")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Merge Documents Dialog
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Compile & Merge Documents", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a name for the compiled, multi-document compiled PDF notebook:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = mergedFileNameInput,
                        onValueChange = { mergedFileNameInput = it },
                        label = { Text("Merged File Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF06B6D4)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mergedFileNameInput.isNotBlank()) {
                            onMergeSelectedNotes(selectedNotesForMerge.toList(), mergedFileNameInput)
                            selectedNotesForMerge.clear()
                            isMergeMode = false
                            showMergeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Text("Compile Document", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
