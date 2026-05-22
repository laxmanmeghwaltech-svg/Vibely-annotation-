package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.BrushType
import com.example.domain.TemplateStyle
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.FileBrowser
import com.example.ui.components.PageSorter
import com.example.ui.components.ThreeDPanel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CanvasViewModel = viewModel()
) {
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val noteFiles by viewModel.noteFiles.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()

    val activeNote by viewModel.activeNote.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val isPageSorterVisible by viewModel.isPageSorterVisible.collectAsState()

    val activeBrush by viewModel.currentBrushType.collectAsState()
    val brushColor by viewModel.currentBrushColor.collectAsState()
    val brushWidth by viewModel.currentBrushWidth.collectAsState()

    val strokes by viewModel.strokes.collectAsState()
    val threeDObjects by viewModel.threeDObjects.collectAsState()
    val isShapeSnappingEnabled by viewModel.isShapeSnappingEnabled.collectAsState()

    var isEraserActive by remember { mutableStateOf(false) }
    var is3DSelectionActive by remember { mutableStateOf(false) }
    var selected3DId by remember { mutableStateOf<String?>(null) }
    
    // Collapsible side manager for optimized tablet viewports
    var isSidebarVisible by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
    ) {
        // 1. Tablet permanent Navigation Rail (Aesthetics)
        NavigationRail(
            containerColor = Color(0xFF0F172A),
            header = {
                Spacer(modifier = Modifier.height(16.dp))
                // Brand Box exactly mirroring the HTML mockup
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { /* Brand Action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            },
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            NavigationRailItem(
                selected = isSidebarVisible,
                onClick = { isSidebarVisible = !isSidebarVisible },
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Libraries", tint = Color.White) },
                label = { Text("Libraries", color = Color.White, fontSize = 10.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF2563EB),
                    indicatorColor = Color(0xFF1E293B)
                )
            )

            NavigationRailItem(
                selected = activeNote != null,
                onClick = { /* Jump to canvas */ },
                icon = { Icon(Icons.Default.Gesture, contentDescription = "Canvas Sketch", tint = Color.White) },
                label = { Text("Workspace", color = Color.White, fontSize = 10.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFF2563EB),
                    indicatorColor = Color(0xFF1E293B)
                ),
                enabled = activeNote != null
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }

        // 2. Multi-pane Hierarchical Directory Column (Middle Pane)
        AnimatedVisibility(
            visible = isSidebarVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            FileBrowser(
                currentFolderId = currentFolderId,
                foldersList = folders,
                filesList = noteFiles,
                allFoldersList = allFolders,
                activeNoteId = activeNote?.id,
                onNavigateToFolder = { viewModel.navigateToFolder(it) },
                onMakeFolder = { viewModel.makeFolder(it) },
                onRenameFolder = { id, text -> viewModel.renameFolder(id, text) },
                onDeleteFolder = { viewModel.deleteFolder(it) },
                onImportPdf = { uri, tag -> viewModel.importPdfFile(uri, tag) },
                onMakeNewNote = { viewModel.makeNewNote(it) },
                onDeleteNote = { viewModel.deleteNote(it) },
                onRenameNote = { id, text -> viewModel.renameNote(id, text) },
                onDuplicateNote = { viewModel.duplicateNote(it) },
                onOpenNote = { viewModel.openNote(it) },
                onMergeSelectedNotes = { list, label -> viewModel.mergeSelectedFiles(list, label) }
            )
        }

        // 3. Central Working Workspace (Canvas / Page Sorter)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val note = activeNote
            if (note != null) {
                if (isPageSorterVisible) {
                    PageSorter(
                        pageCount = note.pageCount,
                        isPdf = note.isPdf,
                        pdfPath = note.localPath,
                        currentPageIndex = currentPageIndex,
                        onPageSelected = { 
                            viewModel.changePage(it)
                            viewModel.togglePageSorter() 
                        },
                        onDuplicatePage = { viewModel.duplicatePage(it) },
                        onDeletePage = { viewModel.deletePage(it) },
                        onInsertBlankPage = { viewModel.insertBlankPageAtEnd(it) },
                        onReorderPage = { from, to -> viewModel.reorderPage(from, to) },
                        onCloseSorter = { viewModel.togglePageSorter() }
                    )
                } else {
                    // Full Dynamic Active Drawing Canvas Layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Floating Architectural Pen / Color Palette Toolbars
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .border(width = 1.dp, color = Color(0xFF334155)),
                            color = Color(0xFF0F172A),
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left Tools: Selector/Brushes/Eraser selection
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.closeActiveNote() },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Exit Note", tint = Color.Red)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Selection tool for 3D elements
                                    IconButton(
                                        onClick = {
                                            isEraserActive = false
                                            is3DSelectionActive = !is3DSelectionActive
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (is3DSelectionActive) Color(0xFF06B6D4) else Color(0xFF1E293B)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenWith,
                                            contentDescription = "Interact 3D Tool",
                                            tint = Color.White
                                        )
                                    }

                                    // Eraser Toggle
                                    IconButton(
                                        onClick = {
                                            isEraserActive = !isEraserActive
                                            is3DSelectionActive = false
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (isEraserActive) Color(0xFFEF4444) else Color(0xFF1E293B)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CleaningServices,
                                            contentDescription = "Eraser Toggle",
                                            tint = Color.White
                                        )
                                    }

                                    // Brushes Toggles Column/Row
                                    val brushesList = listOf(
                                        Pair(BrushType.FOUNTAIN_PEN, "Fountain Pen"),
                                        Pair(BrushType.BALLPOINT, "Ballpoint"),
                                        Pair(BrushType.PENCIL, "Pencil"),
                                        Pair(BrushType.HIGHLIGHTER, "Highlighter")
                                    )

                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        brushesList.forEach { (type, label) ->
                                            val isChosen = activeBrush == type && !isEraserActive && !is3DSelectionActive
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(if (isChosen) Color(0xFF2563EB) else Color.Transparent)
                                                    .clickable {
                                                        isEraserActive = false
                                                        is3DSelectionActive = false
                                                        viewModel.setBrushType(type)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Center: Ink Colors selection chips & Sizes slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val palette = listOf(
                                        0xFF0A0F1D.toInt(), // Ink black
                                        0xFF2563EB.toInt(), // Electric Blue
                                        0xFF06B6D4.toInt(), // Neon Cyan
                                        0xFFEF4444.toInt(), // Rose Red
                                        0xFFF59E0B.toInt()  // Yellow Highlighter
                                    )

                                    palette.forEach { colorVal ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorVal))
                                                .border(
                                                    width = if (brushColor == colorVal) 2.dp else 0.dp,
                                                    color = Color.White,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.setBrushColor(colorVal) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Weight Slider
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Size", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Slider(
                                            value = brushWidth,
                                            onValueChange = { viewModel.setBrushWidth(it) },
                                            valueRange = 2f..40f,
                                            modifier = Modifier.width(100.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF06B6D4),
                                                activeTrackColor = Color(0xFF2563EB)
                                            )
                                        )
                                    }
                                }

                                // Right: Sorter grid switcher, Shape snapping switcher, undo, redo, clear workspace
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Shape Correction snaper Toggle
                                    IconButton(
                                        onClick = { viewModel.isShapeSnappingEnabled.value = !isShapeSnappingEnabled },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (isShapeSnappingEnabled) Color(0xFF1E293B) else Color.Transparent
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pentagon,
                                            contentDescription = "Shape Snapping Toggle",
                                            tint = if (isShapeSnappingEnabled) Color(0xFF06B6D4) else Color(0xFF64748B)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.undo() }) {
                                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White)
                                    }
                                    IconButton(onClick = { viewModel.redo() }) {
                                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.White)
                                    }
                                    IconButton(onClick = { viewModel.clearCanvas() }) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = "Clear Page", tint = Color(0xFFEF4444))
                                    }

                                    Button(
                                        onClick = { viewModel.togglePageSorter() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.GridView, contentDescription = "Pages grid", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sorter", fontSize = 11.sp)
                                    }

                                    // Page pointer counters
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = { viewModel.changePage(currentPageIndex - 1) }, enabled = currentPageIndex > 0) {
                                            Icon(Icons.Default.NavigateBefore, contentDescription = "Previous page", tint = Color.White)
                                        }
                                        Text("${currentPageIndex + 1}/${note.pageCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { viewModel.changePage(currentPageIndex + 1) }, enabled = currentPageIndex < note.pageCount - 1) {
                                            Icon(Icons.Default.NavigateNext, contentDescription = "Next page", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Content Body split: Drawing Canvas on center/left, and 3D palette on right
                        Row(modifier = Modifier.weight(1f)) {
                            // Sub-working canvas
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0A0F1D)).padding(24.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.aspectRatio(0.707f).fillMaxHeight().background(Color.White, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                        .fillMaxHeight()
                                        .background(Color.White)
                            ) {
                                DrawingCanvas(
                                    pdfPath = note.localPath,
                                    pageIndex = currentPageIndex,
                                    isPdf = note.isPdf,
                                    strokeList = strokes,
                                    objectList = threeDObjects,
                                    activeBrush = activeBrush,
                                    brushColor = brushColor,
                                    brushWidth = brushWidth,
                                    isEraserActive = isEraserActive,
                                    is3DSelectionActive = is3DSelectionActive,
                                    selected3DId = selected3DId,
                                    onStrokeDrawn = { viewModel.addStroke(it) },
                                    onEraseGesture = { x, y -> viewModel.eraseStrokesAt(x, y) },
                                    onSelect3D = { selected3DId = it },
                                    onUpdate3D = { viewModel.updateThreeDObject(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            } }
                            // Collapsible Right Panel specifically for 3D Geometric tools
                            ThreeDPanel(
                                onAddPrimitive = { viewModel.addThreeDObject(it) },
                                onDeleteSelected = {
                                    val id = selected3DId
                                    if (id != null) {
                                        viewModel.deleteThreeDObject(id)
                                        selected3DId = null
                                    }
                                },
                                hasSelection = selected3DId != null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .border(width = 1.dp, color = Color(0xFF334155))
                            )
                        }
                    }
                }
            } else {
                // Architectural tablet introductory dark dashboard empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = "Welcome logo",
                        tint = Color(0xFF2563EB).copy(alpha = 0.6f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Unleash Vibely Vector Space",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose an organic notebook list from Libraries sidebar on left, drop high-resolution PDF blueprints or tap 'New Notebook' to commence low-latency architectural sketches.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.widthIn(max = 500.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.makeNewNote("Vibely Scratchpad") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)), // Cyan accent
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "Add default page icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instant Sketchpad", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
