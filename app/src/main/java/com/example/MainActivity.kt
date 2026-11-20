package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.CampRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.GearLockerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripDetailScreen
import com.example.ui.screens.TripsScreen
import com.example.ui.screens.WeightLabScreen
import com.example.ui.theme.CampKitTheme
import com.example.ui.viewmodel.CampViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable
object TripsRoute

@Serializable
object GearLockerRoute

@Serializable
object WeightLabRoute

@Serializable
data class TripDetailRoute(val tripId: String)

@Serializable
object SettingsRoute

private data class TopTab(
    val labelRes: Int,
    val icon: ImageVector,
    val route: Any,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampKitTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedCampShell(
                currentUserId = user.uid,
                userEmail = user.email,
                userDisplayName = user.displayName
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedCampShell(
    currentUserId: String,
    userEmail: String?,
    userDisplayName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CampViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CampViewModel(
                    repository = CampRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val tripsState by viewModel.tripsState.collectAsStateWithLifecycle()
    val gearState by viewModel.gearState.collectAsStateWithLifecycle()
    val packingItemsState by viewModel.packingItemsState.collectAsStateWithLifecycle()
    val activeTripId by viewModel.activeTripId.collectAsStateWithLifecycle()
    val weightUnit by viewModel.weightUnit.collectAsStateWithLifecycle()
    val gearSearch by viewModel.gearSearchQuery.collectAsStateWithLifecycle()
    val gearCategory by viewModel.gearCategoryFilter.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchivedTrips.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentUserId) {
        viewModel.checkForUpdates()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isFullPageSubRoute = currentDest?.hasRoute<TripDetailRoute>() == true ||
        currentDest?.hasRoute<SettingsRoute>() == true

    val topTabs = remember {
        listOf(
            TopTab(R.string.nav_trips, Icons.Rounded.Explore, TripsRoute, "nav_trips"),
            TopTab(R.string.nav_gear, Icons.Rounded.Backpack, GearLockerRoute, "nav_gear"),
            TopTab(R.string.nav_weight, Icons.Rounded.Scale, WeightLabRoute, "nav_weight")
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isFullPageSubRoute) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_campkit_emblem),
                                    contentDescription = stringResource(R.string.app_name),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(SettingsRoute) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.settings_title)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!useRail && !isFullPageSubRoute) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is TripsRoute -> currentDest?.hasRoute<TripsRoute>() == true
                                is GearLockerRoute -> currentDest?.hasRoute<GearLockerRoute>() == true
                                is WeightLabRoute -> currentDest?.hasRoute<WeightLabRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isFullPageSubRoute) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is TripsRoute -> currentDest?.hasRoute<TripsRoute>() == true
                                is GearLockerRoute -> currentDest?.hasRoute<GearLockerRoute>() == true
                                is WeightLabRoute -> currentDest?.hasRoute<WeightLabRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = TripsRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        composable<TripsRoute> {
                            TripsScreen(
                                tripsState = tripsState,
                                showArchived = showArchived,
                                onToggleShowArchived = viewModel::toggleShowArchivedTrips,
                                onOpenTrip = { trip ->
                                    viewModel.selectTrip(trip.id)
                                    navController.navigate(TripDetailRoute(trip.id))
                                },
                                onCreateTrip = { title, location, type, nights, dateLabel, notes, starter ->
                                    viewModel.createTrip(
                                        title, location, type, nights, dateLabel, notes, starter
                                    ) { createdId ->
                                        viewModel.selectTrip(createdId)
                                        navController.navigate(TripDetailRoute(createdId))
                                    }
                                },
                                onUpdateTrip = { id, title, location, type, nights, dateLabel, notes ->
                                    viewModel.updateTrip(id, title, location, type, nights, dateLabel, notes)
                                },
                                onToggleArchive = { trip ->
                                    viewModel.setTripArchived(trip.id, !trip.archived)
                                },
                                onDeleteTrip = { trip ->
                                    viewModel.deleteTrip(trip.id)
                                }
                            )
                        }

                        composable<TripDetailRoute> { entry ->
                            val route = entry.toRoute<TripDetailRoute>()
                            LaunchedEffect(route.tripId) {
                                viewModel.selectTrip(route.tripId)
                            }
                            val trip = (tripsState as? UiState.Success)?.data
                                ?.firstOrNull { it.id == route.tripId }
                            val lockerGear = (gearState as? UiState.Success)?.data.orEmpty()

                            TripDetailScreen(
                                trip = trip,
                                packingItemsState = packingItemsState,
                                lockerGear = lockerGear,
                                weightUnit = weightUnit,
                                onBack = { navController.popBackStack() },
                                onTogglePacked = { item ->
                                    viewModel.togglePackingItemPacked(route.tripId, item.id, !item.packed)
                                },
                                onSavePackingItem = { itemId, name, cat, weight, qty, worn ->
                                    viewModel.savePackingItem(
                                        route.tripId, itemId, name, cat, weight, qty, worn
                                    )
                                },
                                onImportFromLocker = { chosen ->
                                    viewModel.importGearToTrip(route.tripId, chosen)
                                },
                                onDeletePackingItem = { item ->
                                    viewModel.deletePackingItem(route.tripId, item.id)
                                }
                            )
                        }

                        composable<GearLockerRoute> {
                            GearLockerScreen(
                                gearState = gearState,
                                searchQuery = gearSearch,
                                selectedCategory = gearCategory,
                                weightUnit = weightUnit,
                                onSearchChange = viewModel::updateGearSearch,
                                onSelectCategory = viewModel::selectGearCategoryFilter,
                                onSaveGear = { id, name, cat, weight, qty, essential, notes ->
                                    viewModel.saveGearItem(id, name, cat, weight, qty, essential, notes)
                                },
                                onDeleteGear = { gear ->
                                    viewModel.deleteGearItem(gear.id)
                                },
                                onLoadStarterGear = viewModel::loadStarterGear
                            )
                        }

                        composable<WeightLabRoute> {
                            WeightLabScreen(
                                tripsState = tripsState,
                                activeTripId = activeTripId,
                                packingItemsState = packingItemsState,
                                gearState = gearState,
                                weightUnit = weightUnit,
                                onSelectTripForAnalysis = viewModel::selectTrip,
                                onToggleUnit = viewModel::toggleWeightUnit
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                weightUnit = weightUnit,
                                updateUiState = updateUiState,
                                onBack = { navController.popBackStack() },
                                onSelectWeightUnit = viewModel::setWeightUnit,
                                onCheckForUpdate = viewModel::checkForUpdates,
                                onSignOut = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
