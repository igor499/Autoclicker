package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SelectedPoint

@Composable
fun PointSelectionDialog(
    initialPoint: SelectedPoint,
    onDismiss: () -> Unit,
    onPointConfirmed: (Float, Float) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Real screen pixel dimensions approximation based on dp
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var selectedX by remember {
        mutableStateOf(if (initialPoint.isSet) initialPoint.x else screenWidthPx / 2f)
    }
    var selectedY by remember {
        mutableStateOf(if (initialPoint.isSet) initialPoint.y else screenHeightPx / 2f)
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Select Screen Point",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap anywhere on the screen canvas to place target point",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                // Interactive Touch Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 72.dp, bottom = 100.dp)
                        .onGloballyPositioned { coordinates ->
                            canvasSize = coordinates.size
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                // Scale tap coordinates to device screen pixel scale
                                val widthScale = if (canvasSize.width > 0) screenWidthPx / canvasSize.width else 1f
                                val heightScale = if (canvasSize.height > 0) screenHeightPx / canvasSize.height else 1f

                                selectedX = offset.x * widthScale
                                selectedY = offset.y * heightScale
                            }
                        }
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cWidth = size.width
                        val cHeight = size.height

                        // Draw background grid lines
                        val gridSpacing = 60.dp.toPx()
                        val strokeGrid = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        var x = 0f
                        while (x <= cWidth) {
                            drawLine(
                                color = onBackgroundColor.copy(alpha = 0.1f),
                                start = Offset(x, 0f),
                                end = Offset(x, cHeight),
                                strokeWidth = 1f,
                                pathEffect = strokeGrid.pathEffect
                            )
                            x += gridSpacing
                        }
                        var y = 0f
                        while (y <= cHeight) {
                            drawLine(
                                color = onBackgroundColor.copy(alpha = 0.1f),
                                start = Offset(0f, y),
                                end = Offset(cWidth, y),
                                strokeWidth = 1f,
                                pathEffect = strokeGrid.pathEffect
                            )
                            y += gridSpacing
                        }

                        // Map selected screen pixel X/Y back to canvas relative offset
                        val wScale = if (screenWidthPx > 0) cWidth / screenWidthPx else 1f
                        val hScale = if (screenHeightPx > 0) cHeight / screenHeightPx else 1f

                        val targetCanvasX = (selectedX * wScale).coerceIn(0f, cWidth)
                        val targetCanvasY = (selectedY * hScale).coerceIn(0f, cHeight)

                        // Draw target crosshairs
                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = Offset(0f, targetCanvasY),
                            end = Offset(cWidth, targetCanvasY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = Offset(targetCanvasX, 0f),
                            end = Offset(targetCanvasX, cHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Draw target crosshair ring
                        drawCircle(
                            color = primaryColor,
                            radius = 28.dp.toPx(),
                            center = Offset(targetCanvasX, targetCanvasY),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.25f),
                            radius = 38.dp.toPx(),
                            center = Offset(targetCanvasX, targetCanvasY)
                        )
                        drawCircle(
                            color = Color.Red,
                            radius = 6.dp.toPx(),
                            center = Offset(targetCanvasX, targetCanvasY)
                        )
                    }
                }

                // Bottom Control Bar with Coordinate Readout & Confirm Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "X COORDINATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${selectedX.toInt()} px",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Y COORDINATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${selectedY.toInt()} px",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    onPointConfirmed(selectedX, selectedY)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("confirm_point_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirm Point", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
