package io.radar.sdk

import android.app.Activity
import android.content.res.AssetManager
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily


@Composable
fun InAppMessageView(fontId: Int) {
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Color(0xffffffff),
                )
        )

        Text(text = "Hello word!")
        Button(onClick = {
            println("clicked")
        }) {
            Text("Dismiss", fontFamily = FontFamily(Font(resId = fontId)))
        }
    }
}

fun getView(context: Activity): View {
    val fontId = context.resources.getIdentifier("papyrus", "font", context.packageName)
    println("FONT IS: ${fontId}")

    val newView = ComposeView(context)
    newView.setContent {
        InAppMessageView(fontId)
    }

    println("SUPPOSEDLY GENERATED VIEW")
    return newView;
}
