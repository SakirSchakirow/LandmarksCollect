package org.landmarkscollector

import  android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.landmarkscollector.elm.Effect
import org.landmarkscollector.elm.ElmCameraScreen
import org.landmarkscollector.elm.Event
import org.landmarkscollector.elm.State
import org.landmarkscollector.elm.storeFactory
import org.landmarkscollector.ui.theme.LandmarksCollectorTheme
import vivid.money.elmslie.android.renderer.ElmRenderer
import vivid.money.elmslie.android.renderer.ElmRendererDelegate
import vivid.money.elmslie.core.store.Store

class MainActivity : ComponentActivity(), ElmRendererDelegate<Effect, State> {

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
                    ElmCameraScreen(
                        state = state,
                        onDirectoryChosen = { store.accept(Event.Ui.OnDirectoryChosen(it)) },
                        onGestureNameChanged = { store.accept(Event.Ui.OnGestureNameChanged(it)) },
                        onStartRecordingPressed = { store.accept(Event.Ui.OnStartRecordingPressed) },
                        onHandResults = { store.accept(Event.Ui.OnHandResults(it)) },
                        onFacePoseResults = { imageProxy, result ->
                            store.accept(Event.Ui.OnFacePoseResults(imageProxy, result))
                        }
                    )
                }
            }
        }
    }
}
