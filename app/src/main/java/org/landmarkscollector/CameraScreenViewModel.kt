package org.landmarkscollector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.landmarkscollector.data.CsvRow

class CameraScreenViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_GESTURE = "KEY_GESTURE"
    }

    val currentGesture: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_GESTURE, null)
    fun setGestureName(name: String) {
        savedStateHandle[KEY_GESTURE] = name
    }

    private val csvRows: List<CsvRow>? = null
}
