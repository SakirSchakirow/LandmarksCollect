package org.landmarkscollector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RatesColumn(
    rates: Map<String, Float>
) {
    Column {
        rates.map { (gesture, rate) ->
            Box {
                val rateWidth = 200 * rate
                Box(
                    modifier = Modifier
                        .background(color = Color.Blue)
                        .height(25.dp)
                        .width(rateWidth.dp)
                )
                Row {
                    Text(
                        text = gesture,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Text(
                        text = ": $rate",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
