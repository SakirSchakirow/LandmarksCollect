package org.landmarkscollector

import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.landmarkscollector.motionDisplaying.MotionDisplay

class MotionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        setContent { MotionDisplay() }
    }
}
