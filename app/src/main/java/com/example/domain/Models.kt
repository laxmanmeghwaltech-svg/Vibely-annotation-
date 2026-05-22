package com.example.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector

enum class BrushType {
    FOUNTAIN_PEN,
    BALLPOINT,
    PENCIL,
    HIGHLIGHTER
}

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class CanvasStroke(
    val id: String,
    val points: List<StrokePoint>,
    val color: Int, // ARGB integer
    val brushType: BrushType,
    val strokeWidth: Float,
    val isSnappedShape: Boolean = false,
    val snappedShapeType: Shape2DType = Shape2DType.NONE
)

enum class Shape2DType {
    NONE,
    RECTANGLE,
    SQUARE,
    TRIANGLE,
    CIRCLE,
    POLYGON
}

enum class ThreeDPrimitiveType {
    CUBE,
    CUBOID,
    CONE,
    CYLINDER,
    PYRAMID,
    SPHERE
}

data class ThreeDObject(
    val id: String,
    val type: ThreeDPrimitiveType,
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int,
    val rotation: Float = 0f, // in degrees, simple rotation around z-axis
    val isSelected: Boolean = false
)

data class PageTemplate(
    val name: String,
    val style: TemplateStyle
)

enum class TemplateStyle {
    BLANK,
    LINED,
    GRID,
    DOT
}
