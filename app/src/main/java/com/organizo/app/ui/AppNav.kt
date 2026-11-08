package com.organizo.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private data class BottomDest(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomDest("home", "Home", Icons.Filled.Home),
    BottomDest("notes", "Notes", Icons.Filled.StickyNote2),
    BottomDest("tasks", "Tasks", Icons.Filled.TaskAlt),
    BottomDest("calendar", "Calendar", Icons.Filled.CalendarMonth),
    BottomDest("settings", "Settings", Icons.Filled.Settings)
)

@Composable
fun OrganizoApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (bottomItems.any { it.route == route }) {
                NavigationBar {
                    bottomItems.forEach { d ->
                        NavigationBarItem(
                            selected = route == d.route,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icon, d.label) },
                            label = { Text(d.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad).consumeWindowInsets(pad)
        ) {
            composable("home") { HomeScreen(onOpen = { nav.navigate(it) }) }
            composable("notes") { NotesScreen(onOpen = { id -> nav.navigate("note/$id") }) }
            composable(
                "note/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { e ->
                NoteEditorScreen(id = e.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
            composable("tasks") { TasksScreen() }
            composable("calendar") { CalendarScreen() }
            composable("pdf") { PdfScreen(onBack = { nav.popBackStack() }) }
            composable("docs") { DocsScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen() }
        }
    }
}
