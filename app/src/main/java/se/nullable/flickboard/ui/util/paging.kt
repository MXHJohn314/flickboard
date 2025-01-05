package se.nullable.flickboard.ui.util

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey

fun <T : Any> LazyListScope.items(
    items: LazyPagingItems<T>,
    key: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T?) -> Unit,
) {
    items(items.itemCount, key = items.itemKey(key)) { index ->
        itemContent(items[index])
    }
}