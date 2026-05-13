package com.novel.assistant.ui.novel.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novel.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(onBack: () -> Unit, viewModel: ReaderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(uiState.novelTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            uiState.chaptersWithScenes.forEach { cws ->
                // Chapter title
                item(key = "ch_${cws.chapter.id}") {
                    Text(
                        text = cws.chapter.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 32.dp, bottom = 20.dp)
                    )
                    HorizontalDivider(color = DarkDivider, thickness = 0.5.dp)
                    Spacer(Modifier.height(24.dp))
                }

                // Scenes - clean novel content
                items(cws.scenes, key = { "sc_${it.id}" }) { scene ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Scene title (subtle)
                        if (scene.title.isNotBlank()) {
                            Text(
                                text = scene.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                                color = TextHint,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        // Novel content - the main event
                        Text(
                            text = scene.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 17.sp,
                                lineHeight = 30.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Minimal action bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { viewModel.toggleBookmark(scene.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (scene.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    "Đánh dấu",
                                    tint = if (scene.isBookmarked) BlueSky else TextHint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(scene.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (scene.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    "Yêu thích",
                                    tint = if (scene.isFavorite) GoldWarm else TextHint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
