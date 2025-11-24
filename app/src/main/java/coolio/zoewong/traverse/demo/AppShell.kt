@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch


enum class DrawerDest(val route: String, val label: String) {
    Journal("journal", "Journal"),
    MyStories("list", "My Stories"),

    Map("map", "Map"),
    CreateStory("create", "Create Story"),
    Settings("settings", "Settings"),
}

@Composable
fun AppShell(
    nav: NavController,
    currentTitle: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = nav.currentBackStackEntry?.destination?.route
    val isMapScreen = currentRoute?.startsWith("map") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isMapScreen,
        drawerContent = {
            ModalDrawerSheet {
                Text("Traverse", style = MaterialTheme.typography.titleLarge, modifier = androidx.compose.ui.Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text(DrawerDest.Journal.label) },
                    selected = currentTitle == "Journal",
                    onClick = { scope.launch { drawerState.close() }; nav.navigate(DrawerDest.Journal.route) { launchSingleTop = true } }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDest.MyStories.label) },
                    selected = currentTitle == "My Stories",
                    onClick = { scope.launch { drawerState.close() }; nav.navigate(DrawerDest.MyStories.route) { launchSingleTop = true } }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDest.Settings.label) },
                    selected = currentTitle == "Settings",
                    onClick = { scope.launch { drawerState.close() }; nav.navigate(DrawerDest.Settings.route) { launchSingleTop = true } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (subtitle != null) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(currentTitle)
                        }
                    },
                    navigationIcon = navigationIcon ?: {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "menu")
                        }
                    },
                    actions = actions ?: {
                        IconButton(onClick = { /* TODO overflow */ }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "more")
                        }
                    }
                )
            }
        ) { padding ->
            Surface(tonalElevation = 0.dp, modifier = androidx.compose.ui.Modifier.padding(padding)) {
                content()
            }
        }
    }
}
