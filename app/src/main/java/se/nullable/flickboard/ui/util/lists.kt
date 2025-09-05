package se.nullable.flickboard.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.R

/**
 * Replace the contents of the collection with a single value
 */
fun <T> MutableCollection<T>.replaceWith(value: T) {
    clear()
    add(value)
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.collapsibleListHeader(
    label: String,
    expanded: Boolean,
    onSetExpanded: (expanded: Boolean) -> Unit,
) {
    stickyHeader(key = label, contentType = "header") {
        Surface(
            Modifier
                .fillMaxWidth()
                .animateItem()
                .toggleable(expanded, onValueChange = onSetExpanded, role = Role.Checkbox),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7F),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, Modifier.padding(8.dp))
                Spacer(Modifier.weight(1F))
                val hideIconAngle = animateFloatAsState(
                    when {
                        expanded -> 180F
                        else -> 0F
                    },
                    label = "hideIconAngle",
                )
                Icon(
                    painterResource(R.drawable.baseline_arrow_drop_down_24),
                    when {
                        expanded -> "Hide"
                        else -> "Show"
                    },
                    Modifier.rotate(hideIconAngle.value),
                )
            }
        }
    }
}
