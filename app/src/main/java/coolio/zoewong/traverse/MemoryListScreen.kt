package coolio.zoewong.traverse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

@Composable
fun MemoryListScreen() {

    //holds the text
    var newMemory by remember { mutableStateOf("") }

    //holds the list of memories
    var memoryList by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0F7FA)) //lightblue
            .padding(16.dp)
    )  {

        //text box
        OutlinedTextField(
            value = newMemory,
            onValueChange = { newMemory = it },
            label = { Text("Add Memory") },
            modifier = Modifier.fillMaxWidth()
        )

        //button to add the memory
        Button(
            onClick = {
                if (newMemory.isNotBlank()) {
                    memoryList = memoryList + newMemory
                    newMemory = "" // clear the text box
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Add Memory")
        }

        Spacer(modifier = Modifier.height(16.dp))

        //show the list of memories
        LazyColumn(modifier = Modifier.fillMaxSize())
        {
            items(memoryList) { memory ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Text(
                        text = memory,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
