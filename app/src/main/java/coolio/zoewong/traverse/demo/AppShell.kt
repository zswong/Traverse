@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package coolio.zoewong.traverse.ui.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coolio.zoewong.traverse.ui.screen.splash.LoadingSplashScreen
import coolio.zoewong.traverse.ui.state.DatabaseState
import coolio.zoewong.traverse.ui.state.LoadStatus
import coolio.zoewong.traverse.ui.state.shouldSplashScreenBeVisible
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color


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

    AnimatedVisibility(
        visible = shouldSplashScreenBeVisible(),
        exit = fadeOut(),
        modifier = Modifier.zIndex(1000f)
    ) {
        LoadingSplashScreen()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0066FF),  // Sidebar background
                drawerContentColor = Color.White           // Default text color
            ) {
                Text(
                    "Traverse",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            DrawerDest.Journal.label,
                            color = Color.White
                        )
                    },
                    selected = currentTitle == "Journal",
                    onClick = {
                        scope.launch { drawerState.close() };
                        nav.navigate(DrawerDest.Journal.route) { launchSingleTop = true }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF91BEFF),  
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            DrawerDest.MyStories.label,
                            color = Color.White
                        )
                    },
                    selected = currentTitle == "My Stories",
                    onClick = {
                        scope.launch { drawerState.close() };
                        nav.navigate(DrawerDest.MyStories.route) { launchSingleTop = true }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF91BEFF),
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            DrawerDest.Map.label,
                            color = Color.White
                        )
                    },
                    selected = currentTitle == "Map",
                    onClick = {
                        scope.launch { drawerState.close() };
                        nav.navigate(DrawerDest.Map.route) { launchSingleTop = true }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF91BEFF),
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            DrawerDest.Settings.label,
                            color = Color.White
                        )
                    },
                    selected = currentTitle == "Settings",
                    onClick = {
                        scope.launch { drawerState.close() };
                        nav.navigate(DrawerDest.Settings.route) { launchSingleTop = true }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF91BEFF),
                        unselectedContainerColor = Color.Transparent
                    )
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
