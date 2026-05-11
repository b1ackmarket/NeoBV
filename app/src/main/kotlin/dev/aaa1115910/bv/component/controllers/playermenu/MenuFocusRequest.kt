package dev.aaa1115910.bv.component.controllers.playermenu

internal fun resolveParentMenuFocusIndex(selectedIndex: Int, itemCount: Int): Int {
    return selectedIndex.takeIf { itemCount > 0 && it in 0 until itemCount } ?: 0
}
