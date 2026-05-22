package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.*

private val pdfMutex = Mutex()

@Composable
fun rememberPdfPageBitmap(pdfPath: String?, pageIndex: Int, maxDimension: Int = 2048): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(pdfPath, pageIndex, maxDimension) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pdfPath, pageIndex, maxDimension) {
        if (pdfPath == null) {
            bitmap = null
            return@LaunchedEffect
        }
        val file = File(pdfPath)
        if (!file.exists()) {
            bitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                pdfMutex.withLock {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    if (pageIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        // Multi-scale resolution rendering optimized for tablet viewport memory footprints
                        var scale = 2.0f
                        val pageMax = if (page.width > page.height) page.width else page.height
                        if (pageMax * scale > maxDimension) {
                            scale = maxDimension.toFloat() / pageMax
                        }
                        val rawWidth = (page.width * scale).toInt()
                        val rawHeight = (page.height * scale).toInt()
                        val width = if (rawWidth > 1) rawWidth else 1
                        val height = if (rawHeight > 1) rawHeight else 1

                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bitmap = bmp
                    }
                    renderer.close()
                    fd.close()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
    return bitmap
}

@Composable
fun DrawingCanvas(
    pdfPath: String?,
    pageIndex: Int,
    isPdf: Boolean,
    strokeList: List<CanvasStroke>,
    objectList: List<ThreeDObject>,
    activeBrush: BrushType,
    brushColor: Int,
    brushWidth: Float,
    isEraserActive: Boolean,
    is3DSelectionActive: Boolean,
    selected3DId: String?,
    onStrokeDrawn: (CanvasStroke) -> Unit,
    onEraseGesture: (Float, Float) -> Unit,
    onSelect3D: (String?) -> Unit,
    onUpdate3D: (ThreeDObject) -> Unit,
    modifier: Modifier = Modifier
) {
    val pdfBitmap = rememberPdfPageBitmap(pdfPath, pageIndex)
    
    // Local stroke point accumulator
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    // Touch processing
    val canvasModifier = modifier
        .pointerInput(isEraserActive, is3DSelectionActive, activeBrush, brushColor, brushWidth) {
            detectDragGestures(
                onDragStart = { offset ->
                    if (isEraserActive) {
                        onEraseGesture(offset.x, offset.y)
                    } else if (is3DSelectionActive && selected3DId != null) {
                        // Handled separately or select on Tap/Start
                    } else {
                        currentPoints.clear()
                        currentPoints.add(StrokePoint(offset.x, offset.y, 1f))
                        val path = Path()
                        path.moveTo(offset.x, offset.y)
                        currentPath = path
                    }
                },
                onDragEnd = {
                    if (!isEraserActive && currentPoints.isNotEmpty()) {
                        val strokeId = "stroke_${System.currentTimeMillis()}"
                        val newStroke = CanvasStroke(
                            id = strokeId,
                            points = currentPoints.toList(),
                            color = brushColor,
                            brushType = activeBrush,
                            strokeWidth = brushWidth
                        )
                        onStrokeDrawn(newStroke)
                        currentPoints.clear()
                        currentPath = null
                    }
                },
                onDragCancel = {
                    currentPoints.clear()
                    currentPath = null
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val pos = change.position
                    if (isEraserActive) {
                        onEraseGesture(pos.x, pos.y)
                    } else if (is3DSelectionActive && selected3DId != null) {
                        // Drag active 3D object to reposition
                        val obj = objectList.find { it.id == selected3DId }
                        if (obj != null) {
                            onUpdate3D(obj.copy(x = obj.x + dragAmount.x, y = obj.y + dragAmount.y))
                        }
                    } else {
                        // Estimate velocity/pressure simulation for Fountain pen
                        val velocity = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                        val pressureFraction = max(0.2f, min(1.8f, 2.5f - (velocity / 20f)))
                        
                        currentPoints.add(StrokePoint(pos.x, pos.y, pressureFraction))
                        currentPath?.lineTo(pos.x, pos.y)
                    }
                }
            )
        }
        .pointerInput(objectList, is3DSelectionActive) {
            detectTapGestures { offset ->
                if (is3DSelectionActive) {
                    val hit = objectList.find { obj ->
                        sqrt((obj.x - offset.x).pow(2) + (obj.y - offset.y).pow(2)) < obj.size
                    }
                    onSelect3D(hit?.id)
                }
            }
        }

    Canvas(modifier = canvasModifier) {
        // 1. Draw PDF Page background or custom note page templates (blank, lined, grid, dot)
        if (isPdf) {
            if (pdfBitmap != null) {
                drawImage(
                    image = pdfBitmap.asImageBitmap(),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            } else {
                // Loading PDF template placeholder
                drawRect(
                    color = Color.White,
                    size = size
                )
            }
        } else {
            // Render custom premium Note Templates
            drawRect(
                color = Color.White,
                size = size
            )
            // Lined background (similar to high-school notebook)
            val lineSpacing = 32.dp.toPx()
            val gridSpacing = 24.dp.toPx()
            
            // Draw neat lined style
            for (y in (lineSpacing.toInt()..size.height.toInt() step lineSpacing.toInt())) {
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        // 2. Render Existing Drawn Strokes
        strokeList.forEach { stroke ->
            drawStroke(stroke)
        }

        // 3. Render Current Hot Drawing Stroke
        if (currentPoints.size > 1) {
            val strokeColor = Color(brushColor)
            val path = Path().apply {
                val first = currentPoints.first()
                moveTo(first.x, first.y)
                for (i in 1 until currentPoints.size) {
                    val p = currentPoints[i]
                    lineTo(p.x, p.y)
                }
            }
            
            val alpha = if (activeBrush == BrushType.HIGHLIGHTER) 0.4f else 1f
            val blend = if (activeBrush == BrushType.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver

            drawPath(
                path = path,
                color = strokeColor,
                alpha = alpha,
                style = Stroke(
                    width = brushWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = blend
            )
        }

        // 4. Render All Scalable 3D primitives
        objectList.forEach { obj ->
            val selectionColor = if (obj.id == selected3DId) Color(0xFF06B6D4) else null
            draw3DPrimitive(obj, selectionColor)
        }
    }
}

private fun DrawScope.drawStroke(stroke: CanvasStroke) {
    if (stroke.points.size < 2) return
    val strokeColor = Color(stroke.color)
    val alpha = if (stroke.brushType == BrushType.HIGHLIGHTER) 0.4f else 1.0f
    val blendMode = if (stroke.brushType == BrushType.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver

    when (stroke.brushType) {
        BrushType.FOUNTAIN_PEN -> {
            // Fountain pen: Pressure-sensitive thickness variance. We render segment-by-segment with variable path width!
            for (i in 0 until stroke.points.size - 1) {
                val p1 = stroke.points[i]
                val p2 = stroke.points[i + 1]
                val currentWidth = stroke.strokeWidth * p1.pressure
                drawLine(
                    color = strokeColor,
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = currentWidth,
                    cap = StrokeCap.Round,
                    alpha = alpha,
                    blendMode = blendMode
                )
            }
        }
        BrushType.PENCIL -> {
            // Pencil: Semi-translucent stroke with simulated noise/graphite density overlay
            val p = Path().apply {
                val first = stroke.points.first()
                moveTo(first.x, first.y)
                for (i in 1 until stroke.points.size) {
                    val pt = stroke.points[i]
                    lineTo(pt.x, pt.y)
                }
            }
            // First layered pencil lines
            drawPath(
                path = p,
                color = strokeColor,
                alpha = 0.5f,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = blendMode
            )
            // Stippled graphite texture using points
            stroke.points.forEachIndexed { idx, pt ->
                if (idx % 2 == 0) {
                    drawCircle(
                        color = strokeColor,
                        radius = stroke.strokeWidth / 3f,
                        center = Offset(pt.x, pt.y),
                        alpha = 0.3f,
                        blendMode = blendMode
                    )
                }
            }
        }
        else -> {
            // Ballpoint/Highlighter
            val path = Path().apply {
                val first = stroke.points.first()
                moveTo(first.x, first.y)
                for (i in 1 until stroke.points.size) {
                    val pt = stroke.points[i]
                    lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = path,
                color = strokeColor,
                alpha = alpha,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        }
    }
}

/**
 * Direct orthographic vector projection shaders for beautiful 3D geometrics
 */
private fun DrawScope.draw3DPrimitive(obj: ThreeDObject, selectionColor: Color?) {
    val x = obj.x
    val y = obj.y
    val size = obj.size
    val color = Color(obj.color)
    val accentTone = color.copy(alpha = 0.7f)
    val lightTone = color.copy(alpha = 0.3f)
    val shadowTone = color.copy(alpha = 0.85f)

    // Outline path for selected primitives to support drag and drop operations
    if (selectionColor != null) {
        drawCircle(
            color = selectionColor,
            radius = size + 16.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
    }

    when (obj.type) {
        ThreeDPrimitiveType.CUBE -> {
            // Front face
            drawRect(
                color = accentTone,
                topLeft = Offset(x - size/2, y - size/2),
                size = Size(size, size)
            )

            // Top isometric face projection
            val topFace = Path().apply {
                moveTo(x - size/2, y - size/2)
                lineTo(x - size/4, y - size)
                lineTo(x + size * 3/4, y - size)
                lineTo(x + size/2, y - size/2)
                close()
            }
            drawPath(path = topFace, color = lightTone)

            // Right face projection
            val rightFace = Path().apply {
                moveTo(x + size/2, y - size/2)
                lineTo(x + size * 3/4, y - size)
                lineTo(x + size * 3/4, y)
                lineTo(x + size/2, y + size/2)
                close()
            }
            drawPath(path = rightFace, color = shadowTone)

            // Wireframe drawing lines of high contrast architectural style
            drawRect(
                color = Color.Black,
                topLeft = Offset(x - size/2, y - size/2),
                size = Size(size, size),
                style = Stroke(width = 1.5f.dp.toPx())
            )
            drawPath(path = topFace, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
            drawPath(path = rightFace, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
        }
        ThreeDPrimitiveType.CUBOID -> {
            val width = size * 1.5f
            val height = size * 0.8f
            val depth = size * 0.5f

            // Front Rect
            drawRect(
                color = accentTone,
                topLeft = Offset(x - width/2, y - height/2),
                size = Size(width, height)
            )

            // Top Face
            val topFace = Path().apply {
                moveTo(x - width/2, y - height/2)
                lineTo(x - width/2 + depth, y - height/2 - depth)
                lineTo(x + width/2 + depth, y - height/2 - depth)
                lineTo(x + width/2, y - height/2)
                close()
            }
            drawPath(path = topFace, color = lightTone)

            // Right Face
            val rightFace = Path().apply {
                moveTo(x + width/2, y - height/2)
                lineTo(x + width/2 + depth, y - height/2 - depth)
                lineTo(x + width/2 + depth, y + height/2 - depth)
                lineTo(x + width/2, y + height/2)
                close()
            }
            drawPath(path = rightFace, color = shadowTone)

            // Borders
            drawRect(
                color = Color.Black,
                topLeft = Offset(x - width/2, y - height/2),
                size = Size(width, height),
                style = Stroke(width = 1.5f.dp.toPx())
            )
            drawPath(path = topFace, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
            drawPath(path = rightFace, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
        }
        ThreeDPrimitiveType.CYLINDER -> {
            val radiusX = size / 2
            val radiusY = size / 5
            val height = size * 1.2f

            // Column cylinder core gradient
            val rectPath = Path().apply {
                moveTo(x - radiusX, y - height/2 + radiusY)
                lineTo(x + radiusX, y - height/2 + radiusY)
                lineTo(x + radiusX, y + height/2)
                lineTo(x - radiusX, y + height/2)
                close()
            }
            drawPath(
                path = rectPath,
                brush = Brush.linearGradient(
                    colors = listOf(lightTone, accentTone, shadowTone),
                    start = Offset(x - radiusX, y),
                    end = Offset(x + radiusX, y)
                )
            )

            // Lower Ellipse projection
            val bottomEllipse = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(x - radiusX, y + height/2 - radiusY, x + radiusX, y + height/2 + radiusY))
            }
            drawPath(path = bottomEllipse, color = shadowTone)

            // Top architectural lid
            val topEllipse = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(x - radiusX, y - height/2 - radiusY, x + radiusX, y - height/2 + radiusY))
            }
            drawPath(path = topEllipse, color = lightTone)

            // Architectural Wireframe outlines
            drawLine(Color.Black, Offset(x - radiusX, y - height/2), Offset(x - radiusX, y + height/2), strokeWidth = 1.5f.dp.toPx())
            drawLine(Color.Black, Offset(x + radiusX, y - height/2), Offset(x + radiusX, y + height/2), strokeWidth = 1.5f.dp.toPx())
            drawPath(path = topEllipse, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
            drawPath(path = bottomEllipse, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
        }
        ThreeDPrimitiveType.CONE -> {
            val radiusX = size / 2
            val radiusY = size / 5
            val height = size * 1.2f

            // Apex apex to bottom base
            val coneBody = Path().apply {
                moveTo(x, y - height/2)
                lineTo(x + radiusX, y + height/2)
                lineTo(x - radiusX, y + height/2)
                close()
            }
            drawPath(
                path = coneBody,
                brush = Brush.linearGradient(
                    colors = listOf(lightTone, accentTone, shadowTone),
                    start = Offset(x - radiusX, y),
                    end = Offset(x + radiusX, y)
                )
            )

            // Bottom base plane
            val baseEllipse = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(x - radiusX, y + height/2 - radiusY, x + radiusX, y + height/2 + radiusY))
            }
            drawPath(path = baseEllipse, color = shadowTone)

            // Outlines
            drawPath(path = coneBody, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
            drawPath(path = baseEllipse, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
        }
        ThreeDPrimitiveType.PYRAMID -> {
            val baseW = size * 1.1f
            val baseH = size * 0.4f
            val height = size * 1.2f

            val topOffset = Offset(x, y - height/2)
            val leftOffset = Offset(x - baseW/2, y + baseW/4)
            val centerBaseOffset = Offset(x, y + baseW/3)
            val rightOffset = Offset(x + baseW/2, y + baseW/4)

            // Front left pane
            val paneLeft = Path().apply {
                moveTo(topOffset.x, topOffset.y)
                lineTo(leftOffset.x, leftOffset.y)
                lineTo(centerBaseOffset.x, centerBaseOffset.y)
                close()
            }
            drawPath(path = paneLeft, color = lightTone)

            // Front right pane shadows
            val paneRight = Path().apply {
                moveTo(topOffset.x, topOffset.y)
                lineTo(centerBaseOffset.x, centerBaseOffset.y)
                lineTo(rightOffset.x, rightOffset.y)
                close()
            }
            drawPath(path = paneRight, color = shadowTone)

            // Outlines
            drawPath(path = paneLeft, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
            drawPath(path = paneRight, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
        }
        ThreeDPrimitiveType.SPHERE -> {
            // Radial modern sphere highlight shading
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, accentTone, shadowTone),
                    center = Offset(x - size/3, y - size/3),
                    radius = size
                ),
                radius = size,
                center = Offset(x, y)
            )
            // Sphere silhouette
            drawCircle(
                color = Color.Black,
                radius = size,
                center = Offset(x, y),
                style = Stroke(width = 1.5f.dp.toPx())
            )
        }
    }
}
