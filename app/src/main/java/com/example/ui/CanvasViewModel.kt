package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

class CanvasViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(
        context = application,
        folderDao = db.folderDao(),
        noteFileDao = db.noteFileDao(),
        pageAnnotationDao = db.pageAnnotationDao()
    )

    // Folder navigation state
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    // Reactive directory item lists
    val folders = _currentFolderId.flatMapLatest { id ->
        repository.getFoldersInParent(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteFiles = _currentFolderId.flatMapLatest { id ->
        repository.getNoteFilesInParent(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All folders list for moving or copy destination
    val allFolders = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Workspace
    private val _activeNote = MutableStateFlow<NoteFileEntity?>(null)
    val activeNote: StateFlow<NoteFileEntity?> = _activeNote.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Page Sorter active state
    private val _isPageSorterVisible = MutableStateFlow(false)
    val isPageSorterVisible: StateFlow<Boolean> = _isPageSorterVisible.asStateFlow()

    // Canvas drawing states
    private val _currentBrushType = MutableStateFlow(BrushType.FOUNTAIN_PEN)
    val currentBrushType: StateFlow<BrushType> = _currentBrushType.asStateFlow()

    private val _currentBrushColor = MutableStateFlow(0xFF2563EB.toInt()) // Electric Blue
    val currentBrushColor: StateFlow<Int> = _currentBrushColor.asStateFlow()

    private val _currentBrushWidth = MutableStateFlow(8f)
    val currentBrushWidth: StateFlow<Float> = _currentBrushWidth.asStateFlow()

    // Active undo/redo stacks
    private val _strokes = MutableStateFlow<List<CanvasStroke>>(emptyList())
    val strokes: StateFlow<List<CanvasStroke>> = _strokes.asStateFlow()

    private val _threeDObjects = MutableStateFlow<List<ThreeDObject>>(emptyList())
    val threeDObjects: StateFlow<List<ThreeDObject>> = _threeDObjects.asStateFlow()

    private val undoStrokesStack = mutableListOf<List<CanvasStroke>>()
    private val redoStrokesStack = mutableListOf<List<CanvasStroke>>()

    private val undoObjectsStack = mutableListOf<List<ThreeDObject>>()
    private val redoObjectsStack = mutableListOf<List<ThreeDObject>>()

    // Shape correction snappy toggle
    val isShapeSnappingEnabled = MutableStateFlow(true)

    // Initial folder defaults
    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Populate pre-loaded directory structure if empty, welcoming first-time tablet users
            repository.getAllFolders().first().let { currentFolders ->
                if (currentFolders.isEmpty()) {
                    val quicknotesId = repository.createFolder("Quick Notes", null)
                    val highschoolId = repository.createFolder("Lectures & Mathematics", null)
                    repository.createFolder("Geometry Assets", highschoolId)
                    
                    repository.createNewNote("Quick Scratchpad", quicknotesId, initialPages = 2)
                    repository.createNewNote("3D Primitive Sketchpad", highschoolId, initialPages = 1)
                }
            }
        }
    }

    // Navigation and File / Folder management
    fun navigateToFolder(id: Long?) {
        _currentFolderId.value = id
    }

    fun makeFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name, _currentFolderId.value)
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            repository.deleteFolder(id)
        }
    }

    fun renameFolder(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameFolder(id, newName)
        }
    }

    fun importPdfFile(uri: Uri, name: String) {
        viewModelScope.launch {
            val noteId = repository.importPdf(uri, name, _currentFolderId.value)
            // Auto open the imported PDF
            val note = repository.getNoteFileById(noteId)
            if (note != null) {
                openNote(note)
            }
        }
    }

    fun makeNewNote(name: String, initialPages: Int = 1) {
        viewModelScope.launch {
            val noteId = repository.createNewNote(name, _currentFolderId.value, initialPages)
            val note = repository.getNoteFileById(noteId)
            if (note != null) {
                openNote(note)
            }
        }
    }

    fun duplicateNote(id: Long) {
        viewModelScope.launch {
            repository.duplicateNoteFile(id)
        }
    }

    fun openNote(note: NoteFileEntity) {
        viewModelScope.launch {
            saveJob?.cancel()
            // Automatically save any previous work before switching
            saveCurrentPageData()
            
            _activeNote.value = note
            _currentPageIndex.value = 0
            _isPageSorterVisible.value = false
            loadPageData(note.id, 0)
        }
    }

    fun closeActiveNote() {
        viewModelScope.launch {
            saveJob?.cancel()
            saveCurrentPageData()
            _activeNote.value = null
            _strokes.value = emptyList()
            _threeDObjects.value = emptyList()
            undoStrokesStack.clear()
            redoStrokesStack.clear()
            undoObjectsStack.clear()
            redoObjectsStack.clear()
        }
    }

    fun renameNote(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameNoteFile(id, newName)
            // Update active note metadata if renamed
            if (_activeNote.value?.id == id) {
                _activeNote.value = repository.getNoteFileById(id)
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            if (_activeNote.value?.id == id) {
                closeActiveNote()
            }
            repository.deleteNoteFile(id)
        }
    }

    fun mergeSelectedFiles(ids: List<Long>, mergedName: String) {
        viewModelScope.launch {
            repository.mergeNoteFiles(ids, mergedName, _currentFolderId.value)
        }
    }

    // Page level switching & Page Sorter grid actions
    fun changePage(index: Int) {
        val note = _activeNote.value ?: return
        if (index in 0 until note.pageCount) {
            viewModelScope.launch {
                saveJob?.cancel()
                saveCurrentPageData()
                _currentPageIndex.value = index
                loadPageData(note.id, index)
            }
        }
    }

    fun togglePageSorter() {
        _isPageSorterVisible.value = !_isPageSorterVisible.value
    }

    fun insertBlankPageAtEnd(template: TemplateStyle = TemplateStyle.BLANK) {
        val note = _activeNote.value ?: return
        viewModelScope.launch {
            saveCurrentPageData()
            val newCount = note.pageCount + 1
            repository.updatePageCount(note.id, newCount)
            _activeNote.value = note.copy(pageCount = newCount)
            _currentPageIndex.value = newCount - 1
            loadPageData(note.id, newCount - 1)
        }
    }

    fun duplicatePage(pageIdx: Int) {
        val note = _activeNote.value ?: return
        viewModelScope.launch {
            saveCurrentPageData()
            
            // Increment page counts
            val newCount = note.pageCount + 1
            repository.updatePageCount(note.id, newCount)
            _activeNote.value = note.copy(pageCount = newCount)

            // Shift upcoming page annotations
            for (p in (newCount - 2) downTo (pageIdx + 1)) {
                val anno = repository.getAnnotationForPage(note.id, p)
                if (anno != null) {
                    repository.saveAnnotationForPage(
                        note.id,
                        p + 1,
                        Converters().toStrokeList(anno.strokeListJson),
                        Converters().toObjectList(anno.objectListJson)
                    )
                }
            }

            // Copy pageIdx annotation on target duplicate pageIdx + 1
            val originAnno = repository.getAnnotationForPage(note.id, pageIdx)
            if (originAnno != null) {
                repository.saveAnnotationForPage(
                    note.id,
                    pageIdx + 1,
                    Converters().toStrokeList(originAnno.strokeListJson),
                    Converters().toObjectList(originAnno.objectListJson)
                )
            } else {
                repository.deletePageAnnotations(note.id, pageIdx + 1)
            }

            // Switch view to the duplicated page
            _currentPageIndex.value = pageIdx + 1
            loadPageData(note.id, pageIdx + 1)
        }
    }

    fun deletePage(pageIdx: Int) {
        val note = _activeNote.value ?: return
        if (note.pageCount <= 1) return // Keep at least one page

        viewModelScope.launch {
            // Delete current annotations
            repository.deletePageAnnotations(note.id, pageIdx)

            // Shift annotations back
            for (p in (pageIdx + 1) until note.pageCount) {
                val anno = repository.getAnnotationForPage(note.id, p)
                if (anno != null) {
                    repository.saveAnnotationForPage(
                        note.id,
                        p - 1,
                        Converters().toStrokeList(anno.strokeListJson),
                        Converters().toObjectList(anno.objectListJson)
                    )
                } else {
                    repository.deletePageAnnotations(note.id, p - 1)
                }
            }

            // delete highest index duplicate page annotation
            repository.deletePageAnnotations(note.id, note.pageCount - 1)

            val newCount = note.pageCount - 1
            repository.updatePageCount(note.id, newCount)
            _activeNote.value = note.copy(pageCount = newCount)

            // Re-adjust page pointer
            val targetPage = if (_currentPageIndex.value >= newCount) newCount - 1 else _currentPageIndex.value
            _currentPageIndex.value = targetPage
            loadPageData(note.id, targetPage)
        }
    }

    fun reorderPage(fromIdx: Int, toIdx: Int) {
        val note = _activeNote.value ?: return
        if (fromIdx == toIdx) return
        if (fromIdx !in 0 until note.pageCount || toIdx !in 0 until note.pageCount) return

        viewModelScope.launch {
            saveCurrentPageData()

            // We load all page annotations for this note.
            val list = mutableMapOf<Int, Pair<String, String>>()
            for (p in 0 until note.pageCount) {
                val anno = repository.getAnnotationForPage(note.id, p)
                if (anno != null) {
                    list[p] = Pair(anno.strokeListJson, anno.objectListJson)
                }
            }

            // Delete all existing annotations for this note to write clean keys
            for (p in 0 until note.pageCount) {
                repository.deletePageAnnotations(note.id, p)
            }

            // Re-map them according to the move:
            val keys = (0 until note.pageCount).toMutableList()
            val removed = keys.removeAt(fromIdx)
            keys.add(toIdx, removed)

            // keys[newIndex] = oldIndex
            for (newIdx in 0 until note.pageCount) {
                val oldIdx = keys[newIdx]
                val data = list[oldIdx]
                if (data != null) {
                    repository.saveAnnotationForPage(
                        note.id,
                        newIdx,
                        Converters().toStrokeList(data.first),
                        Converters().toObjectList(data.second)
                    )
                }
            }

            // Re-adjust page index
            var targetPageIdx = _currentPageIndex.value
            if (targetPageIdx == fromIdx) {
                targetPageIdx = toIdx
            } else if (fromIdx < toIdx && targetPageIdx in (fromIdx + 1)..toIdx) {
                targetPageIdx--
            } else if (fromIdx > toIdx && targetPageIdx in toIdx until fromIdx) {
                targetPageIdx++
            }
            _currentPageIndex.value = targetPageIdx
            loadPageData(note.id, targetPageIdx)
        }
    }

    // Annotation Data Load/Save Core
    private suspend fun loadPageData(noteId: Long, pageIdx: Int) {
        val pageAnno = repository.getAnnotationForPage(noteId, pageIdx)
        if (pageAnno != null) {
            val converters = Converters()
            _strokes.value = converters.toStrokeList(pageAnno.strokeListJson)
            _threeDObjects.value = converters.toObjectList(pageAnno.objectListJson)
        } else {
            _strokes.value = emptyList()
            _threeDObjects.value = emptyList()
        }
        // Reset undo history stacks for the new page open
        undoStrokesStack.clear()
        redoStrokesStack.clear()
        undoObjectsStack.clear()
        redoObjectsStack.clear()
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    suspend fun saveCurrentPageData() {
        val note = _activeNote.value ?: return
        val currentIdx = _currentPageIndex.value
        val strokesToSave = _strokes.value.toList()
        val objectsToSave = _threeDObjects.value.toList()
        
        withContext(Dispatchers.IO) {
            repository.saveAnnotationForPage(note.id, currentIdx, strokesToSave, objectsToSave)
        }
    }

    // Styling configuration
    fun setBrushType(type: BrushType) {
        _currentBrushType.value = type
    }

    fun setBrushColor(color: Int) {
        _currentBrushColor.value = color
    }

    fun setBrushWidth(width: Float) {
        _currentBrushWidth.value = width
    }

    // Canvas Stroke modifications (Drawing / Erasing)
    fun addStroke(stroke: CanvasStroke) {
        saveStrokeHistory()
        val processedStroke = if (isShapeSnappingEnabled.value) {
            detectAndSnapShape(stroke)
        } else {
            stroke
        }
        
        _strokes.value = _strokes.value + processedStroke
        saveStateDeferred()
    }

    fun eraseStrokesAt(x: Float, y: Float, radius: Float = 30f) {
        val targetStrokes = _strokes.value
        val survivingStrokes = targetStrokes.filter { stroke ->
            // Simple distance check: are any stroke points close to the eraser center?
            stroke.points.none { pt ->
                sqrt((pt.x - x).pow(2) + (pt.y - y).pow(2)) < radius
            }
        }
        if (survivingStrokes.size != targetStrokes.size) {
            saveStrokeHistory()
            _strokes.value = survivingStrokes
            saveStateDeferred()
        }
    }

    // 3D Primitive Droppers
    fun addThreeDObject(type: ThreeDPrimitiveType) {
        saveObjectHistory()
        val newObj = ThreeDObject(
            id = "obj_${System.currentTimeMillis()}",
            type = type,
            x = 300f, // center dropping location on tablet viewport
            y = 300f,
            size = 150f,
            color = _currentBrushColor.value,
            rotation = 0f
        )
        _threeDObjects.value = _threeDObjects.value + newObj
        saveStateDeferred()
    }

    fun updateThreeDObject(updated: ThreeDObject) {
        saveObjectHistory()
        _threeDObjects.value = _threeDObjects.value.map {
            if (it.id == updated.id) updated else it
        }
        saveStateDeferred()
    }

    fun deleteThreeDObject(id: String) {
        saveObjectHistory()
        _threeDObjects.value = _threeDObjects.value.filter { it.id != id }
        saveStateDeferred()
    }

    // Undo & Redo System logic
    private fun saveStrokeHistory() {
        undoStrokesStack.add(_strokes.value.toList())
        redoStrokesStack.clear()
    }

    private fun saveObjectHistory() {
        undoObjectsStack.add(_threeDObjects.value.toList())
        redoObjectsStack.clear()
    }

    fun undo() {
        if (undoStrokesStack.isNotEmpty()) {
            redoStrokesStack.add(_strokes.value.toList())
            _strokes.value = undoStrokesStack.removeAt(undoStrokesStack.size - 1)
            saveStateDeferred()
        }
        if (undoObjectsStack.isNotEmpty()) {
            redoObjectsStack.add(_threeDObjects.value.toList())
            _threeDObjects.value = undoObjectsStack.removeAt(undoObjectsStack.size - 1)
            saveStateDeferred()
        }
    }

    fun redo() {
        if (redoStrokesStack.isNotEmpty()) {
            undoStrokesStack.add(_strokes.value.toList())
            _strokes.value = redoStrokesStack.removeAt(redoStrokesStack.size - 1)
            saveStateDeferred()
        }
        if (redoObjectsStack.isNotEmpty()) {
            undoObjectsStack.add(_threeDObjects.value.toList())
            _threeDObjects.value = redoObjectsStack.removeAt(redoObjectsStack.size - 1)
            saveStateDeferred()
        }
    }

    fun clearCanvas() {
        saveStrokeHistory()
        saveObjectHistory()
        _strokes.value = emptyList()
        _threeDObjects.value = emptyList()
        saveStateDeferred()
    }

    private fun saveStateDeferred() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            saveCurrentPageData()
        }
    }

    /**
     * Perfect 2D Shape snaper algorithm:
     * Analyzes freehand gesture points. If the start and end point are near each other,
     * it evaluates the bounding box and snaps points to geometric corners.
     */
    private fun detectAndSnapShape(stroke: CanvasStroke): CanvasStroke {
        val pts = stroke.points
        if (pts.size < 8) return stroke // too few points to detect shapes

        val start = pts.first()
        val end = pts.last()

        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        val minY = pts.minOf { it.y }
        val maxY = pts.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY
        val cx = minX + width / 2
        val cy = minY + height / 2

        val startToEndDist = sqrt((start.x - end.x).pow(2) + (start.y - end.y).pow(2))
        val perimeterApprox = 2 * (width + height)

        // Closed shape classification: starts and ends close together
        val isClosed = startToEndDist < (perimeterApprox * 0.15f) || pts.size > 20 && startToEndDist < 80f

        if (isClosed) {
            // Check bounding aspect ratios
            val aspect = if (height > 0) width / height else 1f
            val isSquareLike = abs(aspect - 1.0f) < 0.15f

            // Shape classification heuristics:
            // Calculate point dispersion from theoretical circle / rect definitions
            var circleDeviations = 0f
            val averageRadius = (width + height) / 4
            pts.forEach { pt ->
                val dist = sqrt((pt.x - cx).pow(2) + (pt.y - cy).pow(2))
                circleDeviations += abs(dist - averageRadius) / averageRadius
            }
            val circleCloseness = circleDeviations / pts.size

            if (circleCloseness < 0.12f) {
                // Circular stroke snaps to mathematically perfect Circle!
                val sampleCount = 40
                val snappedPoints = (0 until sampleCount).map { i ->
                    val angle = (2 * PI * i / sampleCount).toFloat()
                    StrokePoint(
                        x = cx + averageRadius * cos(angle),
                        y = cy + averageRadius * sin(angle)
                    )
                }
                return stroke.copy(
                    points = snappedPoints,
                    isSnappedShape = true,
                    snappedShapeType = Shape2DType.CIRCLE
                )
            } else if (isSquareLike) {
                // Square Snap
                val side = max(width, height)
                val half = side / 2
                val snappedPoints = listOf(
                    StrokePoint(cx - half, cy - half), // TR
                    StrokePoint(cx + half, cy - half), // TL
                    StrokePoint(cx + half, cy + half), // BL
                    StrokePoint(cx - half, cy + half), // BR
                    StrokePoint(cx - half, cy - half)  // close loop
                )
                return stroke.copy(
                    points = snappedPoints,
                    isSnappedShape = true,
                    snappedShapeType = Shape2DType.SQUARE
                )
            } else {
                // Rectangle or Triangle snap. To classify triangles:
                // Find deep direction turns or we can do a standard Rectangle as fallback
                // Rect snap points
                val snappedPoints = listOf(
                    StrokePoint(minX, minY),
                    StrokePoint(maxX, minY),
                    StrokePoint(maxX, maxY),
                    StrokePoint(minX, maxY),
                    StrokePoint(minX, minY)
                )
                return stroke.copy(
                    points = snappedPoints,
                    isSnappedShape = true,
                    snappedShapeType = Shape2DType.RECTANGLE
                )
            }
        } else {
            // Open shape snapping: snaps to straight modern architectural Line!
            val snappedPoints = listOf(
                StrokePoint(start.x, start.y),
                StrokePoint(end.x, end.y)
            )
            return stroke.copy(
                points = snappedPoints,
                isSnappedShape = true,
                snappedShapeType = Shape2DType.NONE
            )
        }
    }
}
