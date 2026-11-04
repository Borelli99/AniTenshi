package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.tachiyomi.ui.more.OnboardingReposViewModel
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: add an anime repo and, if wanted, a manga repo, so the extensions are there right away.
 * Both are optional: the step can be skipped.
 */
internal class ReposStep : OnboardingStep {

    /** True once at least one repo exists; the screen then shows "Next" instead of "Skip for now". */
    var hasRepos by mutableStateOf(false)
        private set

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val viewModel = metroViewModel<OnboardingReposViewModel>()
        val animeStores by viewModel.animeStores.collectAsStateWithLifecycle()
        val mangaStores by viewModel.mangaStores.collectAsStateWithLifecycle()
        val animeState by viewModel.anime.collectAsStateWithLifecycle()
        val mangaState by viewModel.manga.collectAsStateWithLifecycle()
        SideEffect {
            hasRepos = animeStores.isNotEmpty() || mangaStores.isNotEmpty()
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OnboardingHeader(
                title = stringResource(TLMR.strings.anitenshi_onboarding_repos_title),
                // AniTenshi: repos from a backup (or already added) are ticked; more can still be added
                subtitle = stringResource(
                    if (animeStores.isNotEmpty() || mangaStores.isNotEmpty()) {
                        TLMR.strings.anitenshi_onboarding_repos_body_existing
                    } else {
                        TLMR.strings.anitenshi_onboarding_repos_body
                    },
                ),
            ) {
                OnboardingIcon(Icons.Outlined.Layers)
            }
            RepoField(
                label = stringResource(TLMR.strings.anitenshi_onboarding_repo_anime),
                stores = animeStores,
                state = animeState,
                onAdd = viewModel::addAnimeRepo,
            )
            RepoField(
                label = stringResource(TLMR.strings.anitenshi_onboarding_repo_manga),
                stores = mangaStores,
                state = mangaState,
                onAdd = viewModel::addMangaRepo,
            )
            Text(
                text = stringResource(TLMR.strings.anitenshi_onboarding_repos_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RepoField(
    label: String,
    stores: List<ExtensionStore>,
    state: OnboardingReposViewModel.AddState,
    onAdd: (String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    val error = state.error
    val canAdd = url.isNotBlank() && !state.processing
    val submit = {
        if (canAdd) {
            onAdd(url)
            url = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        stores.forEach { store -> AddedRepo(store) }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text(text = "https://…/index.min.json", maxLines = 1) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            isError = error != null,
            supportingText = if (error != null) {
                { Text(text = error) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                if (state.processing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp),
                    )
                } else {
                    TextButton(
                        onClick = submit,
                        enabled = canAdd,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text(text = stringResource(TLMR.strings.anitenshi_onboarding_repo_add))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AddedRepo(store: ExtensionStore) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(TLMR.strings.anitenshi_onboarding_repo_added),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = store.name.ifBlank { store.indexUrl },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
