package com.checklist.app.presentation.features.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.checklist.app.presentation.features.active_checklists.ActiveChecklistsScreen
import com.checklist.app.presentation.features.current_checklist.CurrentChecklistScreen
import com.checklist.app.presentation.features.templates.TemplatesScreen
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToTemplateEditor: (templateId: String?) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val hasPurchased by viewModel.hasPurchased.collectAsState()
    val activity = LocalContext.current as? Activity
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (state.currentTab) {
                            Tab.TEMPLATES -> "Templates"
                            Tab.ACTIVE_CHECKLISTS -> "Active Checklists"
                            Tab.CURRENT_CHECKLIST -> "Current Checklist"
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            activity?.let { viewModel.throwDevABone(it) }
                        }
                    ) {
                        Icon(
                            imageVector = if (hasPurchased) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (hasPurchased) "Thank you!" else "Throw Dev a Bone",
                            tint = if (hasPurchased) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.currentTab == Tab.TEMPLATES,
                    onClick = { viewModel.selectTab(Tab.TEMPLATES) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Templates") },
                    label = { Text("Templates", modifier = Modifier.semantics { role = Role.Tab }) },
                    modifier = Modifier.semantics { role = Role.Tab }
                )
                NavigationBarItem(
                    selected = state.currentTab == Tab.ACTIVE_CHECKLISTS,
                    onClick = { viewModel.selectTab(Tab.ACTIVE_CHECKLISTS) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Active") },
                    label = { Text("Active", modifier = Modifier.semantics { role = Role.Tab }) },
                    modifier = Modifier.semantics { role = Role.Tab }
                )
                NavigationBarItem(
                    selected = state.currentTab == Tab.CURRENT_CHECKLIST,
                    onClick = { viewModel.selectTab(Tab.CURRENT_CHECKLIST) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Current") },
                    label = { Text("Current", modifier = Modifier.semantics { role = Role.Tab }) },
                    modifier = Modifier.semantics { role = Role.Tab }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (state.currentTab) {
                Tab.TEMPLATES -> {
                    TemplatesScreen(
                        onNavigateToEditor = onNavigateToTemplateEditor,
                        onTemplateClick = { templateId ->
                            viewModel.createChecklistFromTemplate(templateId)
                        }
                    )
                }
                Tab.ACTIVE_CHECKLISTS -> {
                    ActiveChecklistsScreen(
                        onChecklistClick = { checklistId ->
                            viewModel.selectCurrentChecklist(checklistId)
                            viewModel.selectTab(Tab.CURRENT_CHECKLIST)
                        }
                    )
                }
                Tab.CURRENT_CHECKLIST -> {
                    CurrentChecklistScreen(
                        checklistId = state.currentChecklistId
                    )
                }
            }
        }
    }
    
    state.duplicateWarning?.let { templateName ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateWarning() },
            title = { Text("Checklist Already Active") },
            text = { 
                Text("You already have an active checklist from template \"$templateName\". Create another one?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmCreateDuplicate()
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDuplicateWarning() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}