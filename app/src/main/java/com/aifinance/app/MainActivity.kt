package com.aifinance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

                    val openDrawer: () -> Unit = {
                        scope.launch { drawerState.open() }
                    }

                    val closeDrawer: () -> Unit = {
                        scope.launch { drawerState.close() }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.background,
                        ) { innerPadding ->
                            AiFinanceNavHost(
                                navController = navController,
                                onOpenDrawer = openDrawer,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        AnimatedVisibility(
                            visible = drawerState.isOpen,
                            enter = slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(300)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(300)
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                HomeSidebarDrawerContent(
                                    onNavigateHome = {
                                        navController.navigate(HOME_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateStatistics = {
                                        navController.navigate(STATISTICS_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateTransactions = {
                                        navController.navigate(TRANSACTIONS_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateSettings = {
                                        navController.navigate(SETTINGS_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateAssetManagement = {
                                        navController.navigate(ASSET_MANAGEMENT_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateCategoryManagement = {
                                        navController.navigate(CATEGORY_MANAGEMENT_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateScheduledTransaction = {
                                        navController.navigate(SCHEDULED_TRANSACTION_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateToAllRecords = { date ->
                                        navController.navigate("all_records/${date}") {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    onNavigateToBudget = {
                                        navController.navigate(BUDGET_ENTRY_ROUTE) {
                                            launchSingleTop = true
                                        }
                                        closeDrawer()
                                    },
                                    modifier = Modifier.fillMaxHeight()
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.Transparent)
                                        .clickable(
                                            onClick = closeDrawer,
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
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
