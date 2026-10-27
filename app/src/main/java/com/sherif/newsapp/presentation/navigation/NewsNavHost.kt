package com.sherif.newsapp.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sherif.newsapp.presentation.articles.ArticlesScreen
import com.sherif.newsapp.presentation.details.ArticleDetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data object ArticlesRoute

@Serializable
data class ArticleDetailsRoute(val articleUrl: String)

private const val TRANSITION_MILLIS = 300

@Composable
fun NewsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Material "shared axis" style: a short horizontal slide combined with a fade.
    NavHost(
        navController = navController,
        startDestination = ArticlesRoute,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) { it / 8 } +
                fadeIn(tween(TRANSITION_MILLIS))
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) { it / 8 } +
                fadeOut(tween(TRANSITION_MILLIS))
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) { it / 8 } +
                fadeIn(tween(TRANSITION_MILLIS))
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) { it / 8 } +
                fadeOut(tween(TRANSITION_MILLIS))
        },
    ) {
        composable<ArticlesRoute> { entry ->
            ArticlesScreen(
                onArticleClick = { article ->
                    // Drops a second tap that lands while the details screen is already animating in.
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(ArticleDetailsRoute(article.url))
                    }
                },
            )
        }
        composable<ArticleDetailsRoute> { entry ->
            ArticleDetailsScreen(
                articleUrl = entry.toRoute<ArticleDetailsRoute>().articleUrl,
                onBack = dropUnlessResumed { navController.popBackStack() },
            )
        }
    }
}
