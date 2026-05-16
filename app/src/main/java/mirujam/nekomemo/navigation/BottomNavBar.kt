package mirujam.nekomemo.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings

import androidx.compose.ui.res.stringResource

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    data class NavItem(val route: Route, val icon: androidx.compose.ui.graphics.vector.ImageVector)

    val items = listOf(
        NavItem(Route.Library, Icons.Outlined.FolderOpen),
        NavItem(Route.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(modifier = modifier) {
        items.forEach { (route, icon) ->
            val label = stringResource(route.titleResId)
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = { Text(text = label) },
                selected = currentRoute == route.route,
                onClick = { onNavigate(route) },
                alwaysShowLabel = false
            )
        }
    }
}
