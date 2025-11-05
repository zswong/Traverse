package coolio.zoewong.traverse.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraverseLandingScreen(
    locationText: String?,
    onRequestLocation: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    val scroll = rememberScrollState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("TRAVERSE") }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card { Column(Modifier.padding(20.dp)) {
                Text("TRAVERSE", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text("Rediscover Memories", style = MaterialTheme.typography.titleMedium)
            } }


            Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("IMMERSE IN MEMORIES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Traverse Through Time and Place", style = MaterialTheme.typography.bodyLarge)
                Text("Handle Memories Like Brain: Hippocampus maps location & time; stories > endless photo streams.",
                    style = MaterialTheme.typography.bodyMedium)
            } }


            Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TAILOR YOUR STORY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Strict privacy control • Tailor story scope • Avoid embarrassment", style = MaterialTheme.typography.bodyMedium)
            } }


            Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PLAN AHEAD", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("All-in-one Planner, Journal, and Story Maker app", style = MaterialTheme.typography.bodyMedium)
            } }


            Card {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Current Location (one-shot)", style = MaterialTheme.typography.titleMedium)
                    Text(locationText ?: "--", textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onRequestLocation) { Text("获取定位权限") }
                        OutlinedButton(onClick = onRefreshLocation) { Text("刷新位置") }
                    }
                }
            }

            Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KEEP MAKING MEMORIES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("References: Eichenbaum (2017); Austin (2021); McKinsey (2024); AMEX (2025)...",
                    style = MaterialTheme.typography.bodySmall)
            } }
        }
    }
}
