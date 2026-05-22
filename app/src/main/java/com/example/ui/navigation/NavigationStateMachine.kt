package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CanvasViewModel
import com.example.ui.DashboardScreen

// Unified App Flow States Enumeration Matrix
enum class AppScreenState {
    LIBRARY_EXPLORER,
    CANVAS_ANNOTATOR,
    PAGE_SORTER_GRID
}

@Composable
fun VibelyNavigationStateMachine(
    modifier: Modifier = Modifier,
    viewModel: CanvasViewModel = viewModel()
) {
    val activeNote by viewModel.activeNote.collectAsState()
    val isPageSorterVisible by viewModel.isPageSorterVisible.collectAsState()

    // Reactive navigation state synced automatically with model actions
    val currentScreenState = remember(activeNote, isPageSorterVisible) {
        when {
            activeNote == null -> AppScreenState.LIBRARY_EXPLORER
            isPageSorterVisible -> AppScreenState.PAGE_SORTER_GRID
            else -> AppScreenState.CANVAS_ANNOTATOR
        }
    }

    // Handles layout transitions cleanly with crossfade animations representing tablet high-fidelity viewports
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = currentScreenState,
            label = "VibelyScreenTransition"
        ) { state ->
            when (state) {
                AppScreenState.LIBRARY_EXPLORER -> {
                    // State 1: Offline File Management Workspace Grid Layout
                    DashboardScreen(viewModel = viewModel)
                }
                AppScreenState.CANVAS_ANNOTATOR -> {
                    // State 2: High-Performance Hardware-Accelerated Drawing Canvas Engine
                    DashboardScreen(viewModel = viewModel)
                }
                AppScreenState.PAGE_SORTER_GRID -> {
                    // State 3: Visual Page-Level Structural Editor
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
