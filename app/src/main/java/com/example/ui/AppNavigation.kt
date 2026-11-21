package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.UiState
import com.example.data.repository.StudentLifeRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CreateCharacterScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LivesHubScreen
import com.example.ui.viewmodel.StudentLifeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_CAMPUS = "campus_dashboard"
const val ROUTE_LIVES = "campus_lives"
const val ROUTE_HALL_OF_FAME = "hall_of_fame"
const val ROUTE_CREATE_CHARACTER = "create_character"

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = auth.currentUser }
        )
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: "Naija Student"
        key(uid) {
            AuthenticatedCampusShell(
                currentUserId = uid,
                userDisplayName = displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedCampusShell(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudentLifeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudentLifeViewModel(
                    repository = StudentLifeRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val careersState by viewModel.careersState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val selectedCareerId by viewModel.selectedCareerId.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val activeStatusEffects by viewModel.activeStatusEffects.collectAsStateWithLifecycle()
    val lastStatDelta by viewModel.lastStatDelta.collectAsStateWithLifecycle()
    val activeEvent by viewModel.activeEvent.collectAsStateWithLifecycle()
    val stageCompletion by viewModel.stageCompletion.collectAsStateWithLifecycle()
    val statusFeedback by viewModel.statusFeedback.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val careers = (careersState as? UiState.Success)?.data ?: emptyList()
    val activeCareer = careers.firstOrNull { it.id == selectedCareerId } ?: careers.firstOrNull()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CAMPUS
    val showChrome = currentRoute != ROUTE_CREATE_CHARACTER
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf(
        Triple(ROUTE_CAMPUS, stringResource(R.string.nav_play), Pair(Icons.Filled.School, Icons.Outlined.School)),
        Triple(ROUTE_LIVES, stringResource(R.string.nav_lives), Pair(Icons.Filled.Groups, Icons.Outlined.Groups)),
        Triple(ROUTE_HALL_OF_FAME, stringResource(R.string.nav_hall_of_fame), Pair(Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !useNavRail) {
                    NavigationBar {
                        tabs.forEach { (route, label, icons) ->
                            val selected = currentRoute == route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_$route")
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
                if (showChrome && useNavRail) {
                    NavigationRail {
                        tabs.forEach { (route, label, icons) ->
                            val selected = currentRoute == route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_$route")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_CAMPUS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_CAMPUS) {
                        GameplayScreen(
                            career = activeCareer,
                            diaryEntries = diaryEntries,
                            activeStatusEffects = activeStatusEffects,
                            lastStatDelta = lastStatDelta,
                            activeEvent = activeEvent,
                            stageCompletion = stageCompletion,
                            statusFeedback = statusFeedback,
                            isMutating = isMutating,
                            snackbarHostState = snackbarHostState,
                            onActionClick = viewModel::executeAction,
                            onResolveEvent = viewModel::resolveEvent,
                            onAdvanceStage = viewModel::advanceToNextLifeStage,
                            onFinalizeCareer = viewModel::finalizeCareerAtStageEnd,
                            onBuyItem = viewModel::purchaseShopItem,
                            onUpgradeLodge = viewModel::upgradeAccommodation,
                            onNewLifeClick = { navController.navigate(ROUTE_CREATE_CHARACTER) },
                            onClearFeedback = viewModel::clearFeedback
                        )
                    }

                    composable(ROUTE_LIVES) {
                        LivesHubScreen(
                            profileState = profileState,
                            careersState = careersState,
                            selectedCareerId = activeCareer?.id,
                            onSelectCareer = { id ->
                                viewModel.selectCareer(id)
                                navController.navigate(ROUTE_CAMPUS) {
                                    popUpTo(ROUTE_CAMPUS) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onDeleteCareer = viewModel::deleteCareer,
                            onCreateNewClick = { navController.navigate(ROUTE_CREATE_CHARACTER) },
                            onSignOutClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }

                    composable(ROUTE_HALL_OF_FAME) {
                        LeaderboardScreen(
                            leaderboardState = leaderboardState,
                            currentUserId = currentUserId
                        )
                    }

                    composable(ROUTE_CREATE_CHARACTER) {
                        CreateCharacterScreen(
                            isCreating = isMutating,
                            onCreateCharacter = { name, gender, school, archetype, stage ->
                                viewModel.startNewCareer(
                                    name = name,
                                    gender = gender,
                                    schoolName = school,
                                    archetype = archetype,
                                    startingStage = stage,
                                    onCreated = {
                                        navController.navigate(ROUTE_CAMPUS) {
                                            popUpTo(ROUTE_CAMPUS) { inclusive = true }
                                        }
                                    }
                                )
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
