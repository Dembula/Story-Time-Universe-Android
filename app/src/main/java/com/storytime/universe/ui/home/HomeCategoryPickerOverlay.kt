package com.storytime.universe.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.CatalogueTypes
import com.storytime.universe.data.model.HomeBrowseFilter
import com.storytime.universe.ui.theme.StColors

/**
 * Netflix-style full-screen category / genre picker.
 * Only lists media types and genres that currently have catalogue titles.
 */
@Composable
fun HomeCategoryPickerOverlay(
    filter: HomeBrowseFilter = HomeBrowseFilter.All,
    populatedTypeIds: Set<String>,
    populatedGenres: List<String>,
    onSelectType: (HomeBrowseFilter) -> Unit,
    onSelectGenre: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val typeOptions = remember(populatedTypeIds) {
        CatalogueTypes.browseTypeOptions.filter { option ->
            option.id == "ALL" || populatedTypeIds.contains(option.id)
        }
    }
    val genres = remember(populatedGenres) {
        val seen = HashSet<String>()
        populatedGenres
            .map { it.trim() }
            .filter { it.isNotEmpty() && seen.add(it.lowercase()) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                typeOptions.forEach { option ->
                    val selected = when (filter) {
                        HomeBrowseFilter.All -> option.id == "ALL"
                        is HomeBrowseFilter.ContentType -> filter.id == option.id
                        is HomeBrowseFilter.Genre -> false
                    }
                    CategoryButton(
                        title = if (option.id == "ALL") "Home" else option.title,
                        selected = selected,
                        onClick = {
                            if (option.id == "ALL") {
                                onSelectType(HomeBrowseFilter.All)
                            } else {
                                onSelectType(
                                    HomeBrowseFilter.ContentType(
                                        id = option.id,
                                        title = option.title,
                                        typeValues = option.typeValues,
                                    ),
                                )
                            }
                        },
                    )
                }

                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "GENRES",
                        color = StColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    genres.forEach { genre ->
                        val selected = (filter as? HomeBrowseFilter.Genre)
                            ?.name.equals(genre, ignoreCase = true)
                        CategoryButton(
                            title = genre,
                            selected = selected,
                            onClick = { onSelectGenre(genre) },
                        )
                    }
                }
            }

            Box(
                Modifier
                    .padding(bottom = 28.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        title,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
        fontSize = if (selected) 28.sp else 24.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}
