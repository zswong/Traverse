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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun LoadingSplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .blockAllPointerInputs()
            .background(Color(0xFF2D69FF)) // Blue color
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TRAVERSE",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
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
    LoadingSplashScreen()
}
