package dev.aaa1115910.bv.component.controllers.playermenu

import dev.aaa1115910.bv.component.controllers.MenuFocusState

internal fun resolveParentMenuFocusIndex(selectedIndex: Int, itemCount: Int): Int {
    return selectedIndex.takeIf { itemCount > 0 && it in 0 until itemCount } ?: 0
}

internal data class ParentMenuTouchResult<T>(
    val selectedItem: T,
    val focusState: MenuFocusState
)

internal fun <T> resolveParentMenuTouch(
    current: T,
    touched: T
): ParentMenuTouchResult<T> {
    return ParentMenuTouchResult(
        selectedItem = touched,
        focusState = MenuFocusState.Items
    )
}
