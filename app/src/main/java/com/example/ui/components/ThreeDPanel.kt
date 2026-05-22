package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.domain.ThreeDPrimitiveType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDPanel(
    onAddPrimitive: (ThreeDPrimitiveType) -> Unit,
    onDeleteSelected: () -> Unit,
    hasSelection: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B) // Dark Studio Grey
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "Shapes Drawer",
                    tint = Color(0xFF06B6D4) // Neon Cyan
                )
                Text(
                    text = "3D Vector Primitives",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = Color(0xFF334155))

            // Sub-guidelines
            Text(
                text = "Tap to drop architectural wireframes directly onto active pages. Toggle selection with standard tools to drag and scale.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
            )

            // Grid Layout for Primitives
            val primitives = listOf(
                Pair(ThreeDPrimitiveType.CUBE, "Cube"),
                Pair(ThreeDPrimitiveType.CUBOID, "Cuboid"),
                Pair(ThreeDPrimitiveType.CYLINDER, "Cylinder"),
                Pair(ThreeDPrimitiveType.CONE, "Cone"),
                Pair(ThreeDPrimitiveType.PYRAMID, "Pyramid"),
                Pair(ThreeDPrimitiveType.SPHERE, "Sphere")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                primitives.chunked(2).forEach { rowPrims ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPrims.forEach { (type, label) ->
                            Button(
                                onClick = { onAddPrimitive(type) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2D3748),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = when(type) {
                                            ThreeDPrimitiveType.CUBE -> Icons.Default.ViewInAr
                                            ThreeDPrimitiveType.CUBOID -> Icons.Default.Layers
                                            ThreeDPrimitiveType.CYLINDER -> Icons.Default.Circle
                                            ThreeDPrimitiveType.CONE -> Icons.Default.ChangeHistory
                                            ThreeDPrimitiveType.PYRAMID -> Icons.Default.Architecture
                                            ThreeDPrimitiveType.SPHERE -> Icons.Default.Brightness1
                                        },
                                        contentDescription = label,
                                        tint = Color(0xFF2563EB), // Electric Blue
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (hasSelection) {
                Divider(color = Color(0xFF334155))
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Delete 3D shape",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Selected Shape", fontSize = 12.sp)
                }
            }
        }
    }
}
