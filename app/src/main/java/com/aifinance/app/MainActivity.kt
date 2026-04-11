package com.aifinance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.aifinance.app.navigation.AiFinanceNavHost
import com.aifinance.core.designsystem.theme.AiFinanceTheme
import com.aifinance.feature.category_management.navigation.CATEGORY_MANAGEMENT_ROUTE
import com.aifinance.feature.budget.navigation.BUDGET_ENTRY_ROUTE
import com.aifinance.feature.home.HomeSidebarDrawerContent
import com.aifinance.feature.home.ASSET_MANAGEMENT_ROUTE
import com.aifinance.feature.home.navigation.HOME_ROUTE
import com.aifinance.feature.scheduled.navigation.SCHEDULED_TRANSACTION_ROUTE
import com.aifinance.feature.settings.navigation.SETTINGS_ROUTE
import com.aifinance.feature.statistics.navigation.STATISTICS_ROUTE
import com.aifinance.feature.transactions.navigation.TRANSACTIONS_ROUTE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiFinanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = false,
                        drawerContent = {
                            HomeSidebarDrawerContent(
                                onNavigateHome = {
                                    navController.navigate(HOME_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateStatistics = {
                                    navController.navigate(STATISTICS_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateTransactions = {
                                    navController.navigate(TRANSACTIONS_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateSettings = {
                                    navController.navigate(SETTINGS_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateAssetManagement = {
                                    navController.navigate(ASSET_MANAGEMENT_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateCategoryManagement = {
                                    navController.navigate(CATEGORY_MANAGEMENT_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateScheduledTransaction = {
                                    navController.navigate(SCHEDULED_TRANSACTION_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateToAllRecords = { date ->
                                    navController.navigate("all_records/${date}") {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                onNavigateToBudget = {
                                    navController.navigate(BUDGET_ENTRY_ROUTE) {
                                        launchSingleTop = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                            )
                        },
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = MaterialTheme.colorScheme.background,
                            ) { innerPadding ->
                                AiFinanceNavHost(
                                    navController = navController,
                                    onOpenDrawer = {
                                        scope.launch { drawerState.open() }
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            if (drawerState.isOpen) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.32f))
                                        .clickable(
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                            },
                                            indication = null,
                                            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
