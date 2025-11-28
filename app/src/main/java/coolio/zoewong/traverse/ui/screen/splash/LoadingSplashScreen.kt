package coolio.zoewong.traverse.ui.screen.splash

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import coolio.zoewong.traverse.R

@Composable
fun LoadingSplashScreen(
    iconDescription: String = "App Icon",
    icon: Drawable? = null,
    background: Color? = null
) {
    val context = LocalContext.current
    val drawable = remember(context) { icon ?: getIcon(context) }

    Layout(
        content = {
            AppIcon(drawable, iconDescription)
            CircularProgressIndicator()
        },

        measurePolicy = { measurables, constraints ->
            val icon = measurables[0].measure(
                Constraints(
                    maxWidth = (constraints.maxWidth * 0.35f).toInt(),
                    maxHeight = (constraints.maxWidth * 0.35f).toInt(),
                )
            )
            val indicator = measurables[1].measure(
                Constraints(
                    maxWidth = 32.dp.roundToPx(),
                    maxHeight = 32.dp.roundToPx(),
                )
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                val iconTop = constraints.maxHeight / 2 - icon.height / 2
                val iconBottom = iconTop + icon.height

                // Bottom 2/3rds, or 24dp after the icon.
                var indicatorTop = (constraints.maxHeight / 3 * 2) - indicator.height / 2
                if (indicatorTop < (iconBottom + 24.dp.roundToPx())) {
                    indicatorTop = iconBottom + 24.dp.roundToPx()
                }

                icon.place(
                    x = constraints.maxWidth / 2 - icon.width / 2,
                    y = iconTop,
                )
                indicator.place(
                    x = constraints.maxWidth / 2 - indicator.width / 2,
                    y = indicatorTop,
                )
            }
        },

        modifier = Modifier
            .blockAllPointerInputs()
            .background(background ?: MaterialTheme.colorScheme.background)
    )
}

@Composable
fun AppIcon(
    drawable: Drawable,
    contentDescription: String,
) {
    Image(
        painter = rememberDrawablePainter(drawable),
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
    )
}

fun getIcon(context: Context): Drawable {
    val pm = context.packageManager
    if (context is Activity) {
        return pm.getActivityIcon(context.componentName)
    }

    return pm.getApplicationIcon(context.packageName)
}

/**
 * Consumes all pointer input events, preventing them from propagating to other composables.
 */
fun Modifier.blockAllPointerInputs(): Modifier {
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
                currentEvent.changes.forEach { it.consume() }
            }
        }
    }
}

@Preview(widthDp = 411, heightDp = 917)
@Composable
fun LoadingSplashScreenPreview() {
    val context = LocalContext.current
    val theme = context.theme

    LoadingSplashScreen(
        icon = context.resources.getDrawable(
            R.mipmap.ic_launcher_round,
            theme,
        )
    )
}