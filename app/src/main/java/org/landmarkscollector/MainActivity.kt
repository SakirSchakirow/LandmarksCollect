package org.landmarkscollector

import  android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.landmarkscollector.motionRecording.Effect
import org.landmarkscollector.motionRecording.CameraScreen
import org.landmarkscollector.motionRecording.Event
import org.landmarkscollector.motionRecording.State
import org.landmarkscollector.motionRecording.storeFactory
import org.landmarkscollector.ui.theme.LandmarksCollectorTheme
import vivid.money.elmslie.android.renderer.ElmRenderer
import vivid.money.elmslie.android.renderer.ElmRendererDelegate
import vivid.money.elmslie.core.store.Store

internal class MainActivity : ComponentActivity(), ElmRendererDelegate<Effect, State> {

    init {
        ElmRenderer(this, lifecycle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
    }

    override val store: Store<Event, Effect, State> = storeFactory(this)

    override fun render(state: State) {
        setContent {
            LandmarksCollectorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraScreen(
                        state = state,
                        onCameraInfoReceived = { store.accept(Event.Ui.OnCamerasInfoReceived(it)) },
                        onCameraToggle = { store.accept(Event.Ui.OnToggleCamera) },
                        onDirectoryChosen = { store.accept(Event.Ui.OnDirectoryChosen(it)) },
                        onGestureNameChanged = { store.accept(Event.Ui.OnGestureNameChanged(it)) },
                        onStartRecordingPressed = { store.accept(Event.Ui.OnStartRecordingPressed) },
                        onHandResults = { store.accept(Event.Ui.OnHandResults(it)) },
                        onPauseRecordingPressed = { store.accept(Event.Ui.OnPauseRecording) },
                        onResumeRecordingPressed = { store.accept(Event.Ui.OnResumeRecording) },
                        onStopRecordingPressed = { store.accept(Event.Ui.OnStopRecording) },
                        onFacePoseResults = { store.accept(Event.Ui.OnFacePoseResults(it)) }
                    )
                }
            }
        }
    }
}
