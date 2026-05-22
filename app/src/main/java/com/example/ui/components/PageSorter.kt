package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.TemplateStyle

@Composable
fun PageSorter(
    pageCount: Int,
    isPdf: Boolean,
    pdfPath: String?,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onInsertBlankPage: (TemplateStyle) -> Unit,
    onReorderPage: (Int, Int) -> Unit,
    onCloseSorter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTemplateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D)) // Deep Navy Background (60%)
            .padding(24.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = onCloseSorter,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Page Manager & Sorter Grid",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showTemplateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), // Electric Blue Accent
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Insert Page")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Insert Blank Page", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid contents
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(pageCount) { index ->
                val isSelected = index == currentPageIndex
                // PDF page render loading
                val pdfThumbnail = if (isPdf && pdfPath != null) {
                    rememberPdfPageBitmap(pdfPath, index, maxDimension = 320)
                } else null

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPageSelected(index) }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF06B6D4) else Color(0xFF334155), // Cyan highlight or Slate grey
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Secondary Surface (30%)
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Visual Miniature Canvas Thumbnail box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPdf) {
                                if (pdfThumbnail != null) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawImage(
                                            image = pdfThumbnail.asImageBitmap(),
                                            dstSize = androidx.compose.ui.unit.IntSize(
                                                size.width.toInt(),
                                                size.height.toInt()
                                            )
                                        )
                                    }
                                } else {
                                    CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 2.dp)
                                }
                            } else {
                                // Draw a miniature composition of lined paper template
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val space = 20f
                                    for (y in (20..size.height.toInt() step space.toInt())) {
                                        drawLine(
                                            color = Color(0xFFE2E8F0),
                                            start = Offset(0f, y.toFloat()),
                                            end = Offset(size.width, y.toFloat()),
                                            strokeWidth = 1f
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reordering Controls Ribbon
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reorder Index", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onReorderPage(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Move Left",
                                        tint = if (index > 0) Color(0xFF06B6D4) else Color(0xFF475569),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onReorderPage(index, index + 1) },
                                    enabled = index < pageCount - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Move Right",
                                        tint = if (index < pageCount - 1) Color(0xFF06B6D4) else Color(0xFF475569),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Metadata and quick page tools row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${index + 1}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onDuplicatePage(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeletePage(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Insert Blank Page template selection modal
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Choose Page Template style", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val templates = listOf(
                        Pair(TemplateStyle.BLANK, "Blank Canvas"),
                        Pair(TemplateStyle.LINED, "Lined Notebook Pattern"),
                    )
                    templates.forEach { (style, name) ->
                        Button(
                            onClick = {
                                onInsertBlankPage(style)
                                showTemplateDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text(name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Cancel", color = Color(0xFF06B6D4))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
