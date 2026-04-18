package io.shahinhasanov.broker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.shahinhasanov.broker.ui.approval.ApprovalScreen
import io.shahinhasanov.broker.ui.declarations.DeclarationDetailScreen
import io.shahinhasanov.broker.ui.declarations.DeclarationsListScreen

object BrokerRoutes {
    const val LIST = "declarations"
    const val DETAIL = "declarations/{declarationId}"
    const val APPROVAL = "declarations/{declarationId}/approval"

    fun detail(id: String) = "declarations/$id"
    fun approval(id: String) = "declarations/$id/approval"
}

@Composable
fun BrokerNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = BrokerRoutes.LIST) {
        composable(BrokerRoutes.LIST) {
            DeclarationsListScreen(
                onOpenDeclaration = { id -> navController.navigate(BrokerRoutes.detail(id)) }
            )
        }
        composable(
            route = BrokerRoutes.DETAIL,
            arguments = listOf(navArgument("declarationId") { type = NavType.StringType })
        ) {
            DeclarationDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenApproval = { id -> navController.navigate(BrokerRoutes.approval(id)) }
            )
        }
        composable(
            route = BrokerRoutes.APPROVAL,
            arguments = listOf(navArgument("declarationId") { type = NavType.StringType })
        ) {
            ApprovalScreen(
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack(BrokerRoutes.LIST, inclusive = false) }
            )
        }
    }
}
