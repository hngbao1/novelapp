package com.novel.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.novel.assistant.ui.home.HomeScreen
import com.novel.assistant.ui.novel.creation.NovelCreationScreen
import com.novel.assistant.ui.novel.editor.WritingScreen
import com.novel.assistant.ui.novel.reader.NovelReaderScreen
import com.novel.assistant.ui.novel.timeline.TimelineScreen
import com.novel.assistant.ui.character.CharacterScreen
import com.novel.assistant.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val CREATE_NOVEL = "create_novel"
    const val WRITING = "writing/{novelId}"
    const val READER = "reader/{novelId}"
    const val CHARACTERS = "characters/{novelId}"
    const val TIMELINE = "timeline/{novelId}"
    const val SETTINGS = "settings"

    fun writing(novelId: Long) = "writing/$novelId"
    fun reader(novelId: Long) = "reader/$novelId"
    fun characters(novelId: Long) = "characters/$novelId"
    fun timeline(novelId: Long) = "timeline/$novelId"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNovelClick = { navController.navigate(Routes.writing(it)) },
                onCreateNovel = { navController.navigate(Routes.CREATE_NOVEL) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.CREATE_NOVEL) {
            NovelCreationScreen(
                onBack = { navController.popBackStack() },
                onNovelCreated = { novelId ->
                    navController.navigate(Routes.writing(novelId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(
            Routes.WRITING,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) {
            WritingScreen(
                onBack = { navController.popBackStack() },
                onCharacters = { navController.navigate(Routes.characters(it)) },
                onReader = { navController.navigate(Routes.reader(it)) },
                onTimeline = { navController.navigate(Routes.timeline(it)) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            Routes.READER,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) {
            NovelReaderScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.CHARACTERS,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) {
            CharacterScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.TIMELINE,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) {
            TimelineScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
