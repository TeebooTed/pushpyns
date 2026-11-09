package com.pushpyns.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pushpyns.R
import com.pushpyns.domain.repository.ProAccess
import com.pushpyns.domain.repository.ProfileRepository
import com.pushpyns.ui.categories.CategoryRoute
import com.pushpyns.ui.components.PushpynsMark
import com.pushpyns.ui.home.HomeRoute
import com.pushpyns.ui.paywall.PaywallRoute
import com.pushpyns.ui.profile.ProfileRoute
import com.pushpyns.ui.profile.StartDestination
import com.pushpyns.ui.profile.StartViewModel
import com.pushpyns.ui.pyns.PynDetailRoute
import com.pushpyns.ui.pyns.PynEditorRoute
import com.pushpyns.ui.pyns.PynListRoute
import com.pushpyns.ui.settings.SettingsRoute
import com.pushpyns.ui.settings.TermsRoute
import com.pushpyns.ui.work.SignatureRoute
import com.pushpyns.ui.work.SignedRoute
import com.pushpyns.ui.work.TermsAcceptRoute
import com.pushpyns.ui.work.WorkOrderDetailRoute
import com.pushpyns.ui.work.WorkOrderEditorRoute
import com.pushpyns.ui.work.WorkOrderListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object Routes {
    const val START = "start"
    const val PROFILE_SETUP = "profile/setup"
    const val PROFILE_EDIT = "profile/edit"
    const val HOME = "home"
    const val PYNS = "pyns"
    const val PYN_CREATE = "pyns/create?lat={lat}&lng={lng}"
    const val PYN_DETAIL = "pyns/{pynId}"
    const val PYN_EDIT = "pyns/{pynId}/edit"
    const val CATEGORIES = "categories"
    const val WORK = "workorders"
    const val WORK_CREATE = "workorders/create?pynId={pynId}"
    const val WORK_DETAIL = "workorders/{workOrderId}"
    const val WORK_EDIT = "workorders/{workOrderId}/edit"
    const val WORK_TERMS = "workorders/{workOrderId}/terms"
    const val WORK_SIGN = "workorders/{workOrderId}/sign"
    const val WORK_DONE = "workorders/{workOrderId}/done"
    const val PAYWALL = "paywall"
    const val SETTINGS = "settings"
    const val TERMS = "terms"

    fun pynDetail(id: Long) = "pyns/$id"

    fun pynEdit(id: Long) = "pyns/$id/edit"

    fun pynCreate(lat: Double?, lng: Double?) = "pyns/create?lat=${lat?.toString().orEmpty()}&lng=${lng?.toString().orEmpty()}"

    fun workDetail(id: Long) = "workorders/$id"

    fun workEdit(id: Long) = "workorders/$id/edit"

    fun workCreate(pynId: Long?) = "workorders/create?pynId=${pynId ?: -1L}"

    fun workTerms(id: Long) = "workorders/$id/terms"

    fun workSign(id: Long) = "workorders/$id/sign"

    fun workDone(id: Long) = "workorders/$id/done"
}

private val topLevel = setOf(Routes.HOME, Routes.PYNS, Routes.WORK, Routes.CATEGORIES, Routes.SETTINGS, Routes.TERMS)

data class DrawerState(val name: String, val pro: Boolean)

@HiltViewModel
class DrawerViewModel
@Inject
constructor(profiles: ProfileRepository, pro: ProAccess) : ViewModel() {
    val state =
        combine(profiles.observe(), pro.observeIsPro()) { profile, isPro ->
            DrawerState(profile?.name.orEmpty(), isPro)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerState("", false))
}

@Composable
fun PushpynsNavHost(drawerViewModel: DrawerViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val drawer = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showDrawer = route in topLevel
    val drawerState by drawerViewModel.state.collectAsStateWithLifecycle()
    val openMenu: () -> Unit = { scope.launch { drawer.open() } }
    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = showDrawer,
        drawerContent = {
            AppDrawer(
                name = drawerState.name,
                pro = drawerState.pro,
                route = route,
                onDestination = { dest ->
                    scope.launch { drawer.close() }
                    nav.navigate(dest) { launchSingleTop = true }
                },
            )
        },
    ) {
        PushpynsGraph(nav = nav, onMenu = openMenu, pro = drawerState.pro)
    }
}

@Composable
private fun AppDrawer(name: String, pro: Boolean, route: String?, onDestination: (String) -> Unit) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PushpynsMark(Modifier.height(40.dp).width(32.dp), hole = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    if (name.isNotBlank()) Text(name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_map)) },
            selected = route == Routes.HOME,
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            onClick = { onDestination(Routes.HOME) },
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_pyns)) },
            selected = route == Routes.PYNS,
            icon = { Icon(Icons.Outlined.Place, contentDescription = null) },
            onClick = { onDestination(Routes.PYNS) },
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_work)) },
            selected = route == Routes.WORK,
            icon = { Icon(Icons.Outlined.Work, contentDescription = null) },
            badge = { if (!pro) Badge { Text(stringResource(R.string.pro_badge)) } },
            onClick = { onDestination(if (pro) Routes.WORK else Routes.PAYWALL) },
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_categories)) },
            selected = route == Routes.CATEGORIES,
            icon = { Icon(Icons.Outlined.Category, contentDescription = null) },
            onClick = { onDestination(Routes.CATEGORIES) },
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_terms)) },
            selected = route == Routes.TERMS,
            icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
            onClick = { onDestination(Routes.TERMS) },
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = route == Routes.SETTINGS,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            onClick = { onDestination(Routes.SETTINGS) },
        )
    }
}

@Composable
private fun PushpynsGraph(nav: NavHostController, onMenu: () -> Unit, pro: Boolean) {
    NavHost(navController = nav, startDestination = Routes.START, modifier = Modifier.fillMaxSize()) {
        composable(Routes.START) {
            val vm: StartViewModel = hiltViewModel()
            val dest by vm.destination.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(dest) {
                when (dest) {
                    StartDestination.Profile ->
                        nav.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.START) { inclusive = true }
                        }
                    StartDestination.Home ->
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.START) { inclusive = true }
                        }
                    null -> Unit
                }
            }
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PushpynsMark(Modifier.height(72.dp).width(56.dp))
            }
        }
        composable(Routes.PROFILE_SETUP) {
            ProfileRoute(editing = false, onDone = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.PROFILE_SETUP) { inclusive = true } }
            })
        }
        composable(Routes.PROFILE_EDIT) {
            ProfileRoute(editing = true, onDone = { nav.popBackStack() })
        }
        composable(Routes.HOME) {
            HomeRoute(
                onMenu = onMenu,
                onNewPyn = { lat, lng -> nav.navigate(Routes.pynCreate(lat, lng)) },
                onPyn = { nav.navigate(Routes.pynDetail(it)) },
            )
        }
        composable(Routes.PYNS) {
            PynListRoute(
                onMenu = onMenu,
                onPyn = { nav.navigate(Routes.pynDetail(it)) },
                onCreate = { nav.navigate(Routes.pynCreate(null, null)) },
            )
        }
        composable(
            Routes.PYN_CREATE,
            arguments =
            listOf(
                navArgument("lat") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("lng") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            PynEditorRoute(
                onBack = { nav.popBackStack() },
                onSaved = { id ->
                    nav.navigate(Routes.pynDetail(id)) { popUpTo(Routes.PYN_CREATE) { inclusive = true } }
                },
                onManageCategories = { nav.navigate(Routes.CATEGORIES) },
            )
        }
        composable(
            Routes.PYN_DETAIL,
            arguments = listOf(navArgument("pynId") { type = NavType.LongType }),
        ) {
            PynDetailRoute(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.pynEdit(it)) },
                onWorkOrder = { id ->
                    nav.navigate(if (pro) Routes.workCreate(id) else Routes.PAYWALL)
                },
            )
        }
        composable(
            Routes.PYN_EDIT,
            arguments = listOf(navArgument("pynId") { type = NavType.LongType }),
        ) {
            PynEditorRoute(
                onBack = { nav.popBackStack() },
                onSaved = { _ -> nav.popBackStack() },
                onManageCategories = { nav.navigate(Routes.CATEGORIES) },
            )
        }
        composable(Routes.CATEGORIES) { CategoryRoute(onMenu) }
        composable(Routes.WORK) {
            WorkOrderListRoute(
                onMenu = onMenu,
                onCreate = { nav.navigate(Routes.workCreate(null)) },
                onOpen = { nav.navigate(Routes.workDetail(it)) },
            )
        }
        composable(
            Routes.WORK_CREATE,
            arguments =
            listOf(
                navArgument("pynId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            WorkOrderEditorRoute(
                onBack = { nav.popBackStack() },
                onSaved = { id ->
                    nav.navigate(Routes.workDetail(id)) { popUpTo(Routes.WORK_CREATE) { inclusive = true } }
                },
                onContinue = { id ->
                    nav.navigate(Routes.workTerms(id)) { popUpTo(Routes.WORK_CREATE) { inclusive = true } }
                },
            )
        }
        composable(
            Routes.WORK_DETAIL,
            arguments = listOf(navArgument("workOrderId") { type = NavType.LongType }),
        ) {
            WorkOrderDetailRoute(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.workEdit(it)) },
                onComplete = { nav.navigate(Routes.workTerms(it)) },
                onPyn = { nav.navigate(Routes.pynDetail(it)) },
            )
        }
        composable(
            Routes.WORK_EDIT,
            arguments = listOf(navArgument("workOrderId") { type = NavType.LongType }),
        ) {
            WorkOrderEditorRoute(
                onBack = { nav.popBackStack() },
                onSaved = { _ -> nav.popBackStack() },
                onContinue = { id -> nav.navigate(Routes.workTerms(id)) },
            )
        }
        composable(
            Routes.WORK_TERMS,
            arguments = listOf(navArgument("workOrderId") { type = NavType.LongType }),
        ) {
            TermsAcceptRoute(
                onBack = { nav.popBackStack() },
                onAccepted = { nav.navigate(Routes.workSign(it)) },
            )
        }
        composable(
            Routes.WORK_SIGN,
            arguments = listOf(navArgument("workOrderId") { type = NavType.LongType }),
        ) {
            SignatureRoute(
                onBack = { nav.popBackStack() },
                onDone = { id ->
                    nav.navigate(Routes.workDone(id)) {
                        popUpTo(Routes.WORK_DETAIL) { inclusive = false }
                    }
                },
            )
        }
        composable(
            Routes.WORK_DONE,
            arguments = listOf(navArgument("workOrderId") { type = NavType.LongType }),
        ) {
            SignedRoute(onDone = {
                nav.navigate(Routes.WORK) { popUpTo(Routes.HOME) }
            })
        }
        composable(Routes.PAYWALL) {
            PaywallRoute(
                onBack = { nav.popBackStack() },
                onUnlocked = {
                    nav.navigate(Routes.WORK) { popUpTo(Routes.PAYWALL) { inclusive = true } }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onMenu = onMenu,
                onEditProfile = { nav.navigate(Routes.PROFILE_EDIT) },
                onPaywall = { nav.navigate(Routes.PAYWALL) },
                onTerms = { nav.navigate(Routes.TERMS) },
            )
        }
        composable(Routes.TERMS) { TermsRoute(onMenu = onMenu, onBack = null) }
    }
}
