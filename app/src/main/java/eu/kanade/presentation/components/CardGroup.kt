package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight

/**
 * AniTenshi: a few entries that belong together, in one card with their name above it. It is how the first
 * page of the settings and the "More" page are laid out.
 */
@Composable
fun CardGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
        }
    }
}

/**
 * One entry of a [CardGroup]: an icon in a small box, a title, and under it a line that says what the entry
 * is set to or what is inside.
 *
 * @param selected null when the entry is not one of a set to pick from. On tablets, where the page an entry
 * opens is shown beside the list, it is true for the entry of that page.
 * @param trailing what closes the row; the arrow that says "this opens a page" unless told otherwise.
 */
@Composable
fun CardEntry(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = Color.Unspecified,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean? = null,
    trailing: (@Composable () -> Unit)? = { CardEntryArrow() },
) {
    CardEntryRow(
        title = title,
        subtitle = subtitle,
        subtitleColor = subtitleColor,
        icon = icon,
        iconTint = iconTint,
        isSelected = selected == true,
        modifier = modifier,
        interaction = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (selected != null) this.selected = selected },
        trailing = trailing,
    )
}

/**
 * An entry of a [CardGroup] that turns something on and off.
 */
@Composable
fun CardSwitchEntry(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    CardEntryRow(
        title = title,
        subtitle = subtitle,
        subtitleColor = Color.Unspecified,
        icon = icon,
        iconTint = MaterialTheme.colorScheme.primary,
        isSelected = false,
        modifier = modifier,
        interaction = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}

@Composable
fun CardEntryArrow() {
    Icon(
        imageVector = MaterialSymbols.Rounded.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun CardEntryRow(
    title: String,
    subtitle: String?,
    subtitleColor: Color,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    modifier: Modifier,
    interaction: Modifier,
    trailing: (@Composable () -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val titleColor = if (isSelected) colors.onSecondaryContainer else colors.onSurface
    val defaultSubtitleColor = if (isSelected) {
        colors.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        colors.onSurfaceVariant
    }
    val iconBoxColor = if (isSelected) {
        colors.onSecondaryContainer.copy(alpha = 0.12f)
    } else {
        colors.surfaceContainerHigh
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
            .then(interaction)
            .heightIn(min = 60.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBoxColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconTint,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = subtitleColor.takeOrElse { defaultSubtitleColor },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}
