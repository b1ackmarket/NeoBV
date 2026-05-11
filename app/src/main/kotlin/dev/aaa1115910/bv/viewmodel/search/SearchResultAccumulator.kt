package dev.aaa1115910.bv.viewmodel.search

class SearchResultAccumulator<T, C>(
    private val itemKey: (T) -> Any,
    initialCursor: C
) {
    private var hasAcceptedPage: Boolean = false

    var cursor: C = initialCursor
        private set

    private val keys = linkedSetOf<Any>()
    private val _items = mutableListOf<T>()
    val items: List<T> get() = _items

    fun append(cursor: C, items: List<T>) {
        if (hasAcceptedPage && cursor == this.cursor) return
        hasAcceptedPage = true
        this.cursor = cursor
        items.forEach { item ->
            if (keys.add(itemKey(item))) {
                _items += item
            }
        }
    }

    fun clear(initialCursor: C) {
        hasAcceptedPage = false
        cursor = initialCursor
        keys.clear()
        _items.clear()
    }
}
