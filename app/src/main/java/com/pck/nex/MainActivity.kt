package com.pck.nex

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.nex.ui.theme.NeXTheme
import com.pck.nex.ui.screen.day.DayScreen
import com.pck.nex.ui.screen.library.LibraryScreen
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            NeXTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val today = LocalDate.now().toString()

                    NavHost(
                        navController = nav,
                        startDestination = "day/$today"
                    ) {
                        composable(
                            route = "day/{dateIso}",
                            arguments = listOf(navArgument("dateIso") { type = NavType.StringType })
                        ) { backStack ->
                            val dateIso = backStack.arguments?.getString("dateIso") ?: today

                            DayScreen(
                                initialDateIso = dateIso,
                                onOpenLibrary = { nav.navigate("library") },
                                onOpenDay = { iso ->
                                    nav.navigate("day/$iso") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable("library") {
                            LibraryScreen(
                                onOpenDay = { iso ->
                                    nav.navigate("day/$iso") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
