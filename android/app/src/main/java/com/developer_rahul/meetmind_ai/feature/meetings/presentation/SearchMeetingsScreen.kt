package com.developer_rahul.meetmind_ai.feature.meetings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer_rahul.meetmind_ai.core.designsystem.*
import com.developer_rahul.meetmind_ai.core.ui.ViewModelFactory
import com.developer_rahul.meetmind_ai.core.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMeetingsScreen(
    onBack: () -> Unit,
    onMeetingClick: (String) -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = ViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredMeetings = uiState.meetings.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || it.hostName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    MeetMindTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Search by title or host",
                        leadingIcon = Icons.Default.Search,
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            if (searchQuery.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Suggested Searches", style = Typography.labelLarge, color = ElectricIndigo)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(text = "WebRTC", color = TextSecondary)
                            StatusChip(text = "Architecture", color = TextSecondary)
                            StatusChip(text = "Rahul", color = TextSecondary)
                        }
                    }
                }
            } else if (filteredMeetings.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp), tint = TextDisabled)
                        Spacer(Modifier.height(24.dp))
                        Text("No results for \"$searchQuery\"", style = Typography.titleMedium, color = TextSecondary)
                        Text("Check your spelling or try a different keyword.", style = Typography.bodyMedium, color = TextDisabled)
                    }
                }
            }
            
            items(filteredMeetings) { meeting ->
                MeetingItemCard(meeting = meeting, onClick = { onMeetingClick(meeting.id.toString()) })
            }
        }
    }
}
