package dev.aaa1115910.bv.viewmodel.player

class TempSpeedHoldState(
    val temporarySpeed: Float = 2.0f
) {
    var originalSpeed: Float = 1f
        private set

    var isHoldingSpeed: Boolean = false
        private set

    fun onLongPressTriggered(currentSpeed: Float) {
        originalSpeed = currentSpeed
        isHoldingSpeed = true
    }

    fun onKeyReleased(): Float {
        val restored = originalSpeed
        isHoldingSpeed = false
        return restored
    }
}
