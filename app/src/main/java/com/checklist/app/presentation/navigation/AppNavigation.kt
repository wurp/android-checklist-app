package com.checklist.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.checklist.app.presentation.features.main.MainScreen
import com.checklist.app.presentation.features.template_editor.TemplateEditorScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToTemplateEditor = { templateId ->
                    if (templateId != null) {
                        navController.navigate("template_editor/$templateId")
                    } else {
                        navController.navigate("template_editor/new")
                    }
                }
            )
        }
        
        composable(
            route = "template_editor/{templateId}",
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")
            TemplateEditorScreen(
                templateId = if (templateId == "new") null else templateId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}