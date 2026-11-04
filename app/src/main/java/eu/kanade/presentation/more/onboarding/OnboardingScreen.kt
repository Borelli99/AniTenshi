package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AniTenshi: first launch in six steps: welcome and theme, storage folder, permissions, sync with Google Drive
 * or a backup, extension repos, then a short "all set" with how to move around.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: (uri: String) -> Unit,
) {
    val slideDistance = rememberSlideDistance()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            WelcomeStep(),
            StorageStep(),
            PermissionStep(),
            SyncStep(onRestoreBackup = onRestoreBackup),
            ReposStep(),
            DoneStep(),
        )
    }
    val step = steps[currentStep]
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0, onBack = { currentStep-- })

    Scaffold(
        bottomBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = {
                        if (isLastStep) {
                            onComplete()
                        } else {
                            currentStep++
                        }
                    },
                    enabled = step.isComplete,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (isLastStep) TLMR.strings.anitenshi_onboarding_start else TLMR.strings.anitenshi_onboarding_next,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (step is ReposStep && !step.hasRepos) {
                    TextButton(onClick = { currentStep++ }) {
                        Text(text = stringResource(TLMR.strings.anitenshi_onboarding_skip))
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            OnboardingDots(
                count = steps.size,
                current = currentStep,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = stringResource(TLMR.strings.anitenshi_onboarding_step, currentStep + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                label = "stepContent",
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(modifier = Modifier.widthIn(max = 560.dp)) {
                        // The steps kept from Mihon get a header in the same style as the new ones
                        when (steps[index]) {
                            is StorageStep -> OnboardingHeader(
                                title = stringResource(TLMR.strings.anitenshi_onboarding_storage_title),
                                subtitle = null,
                            ) { OnboardingIcon(Icons.Outlined.Folder) }

                            is PermissionStep -> OnboardingHeader(
                                title = stringResource(TLMR.strings.anitenshi_onboarding_permissions_title),
                                subtitle = null,
                            ) { OnboardingIcon(Icons.Outlined.VerifiedUser) }

                            else -> {}
                        }
                        steps[index].Content()
                    }
                }
            }
        }
    }
}
