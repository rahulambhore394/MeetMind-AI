package com.developer_rahul.meetmind_ai.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer_rahul.meetmind_ai.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    onDismiss: () -> Unit,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf("English (US)", "English (UK)", "Hindi (HI)", "Marathi (MR)", "Spanish (ES)", "French (FR)")
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Select Language",
                style = Typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            LazyColumn {
                items(languages) { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(lang); onDismiss() }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lang,
                            style = Typography.bodyLarge,
                            color = if (lang == selectedLanguage) ElectricIndigo else TextPrimary,
                            fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                        )
                        if (lang == selectedLanguage) {
                            Icon(Icons.Default.Check, null, tint = ElectricIndigo)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMeetingsSheet(
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextDisabled) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Filter Meetings", style = Typography.titleLarge, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Status", style = Typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                FilterChip(selected = false, onClick = {}, label = { Text("Upcoming") })
                FilterChip(selected = false, onClick = {}, label = { Text("Completed") })
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Date Range", style = Typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("Anytime") })
                FilterChip(selected = false, onClick = {}, label = { Text("Today") })
                FilterChip(selected = false, onClick = {}, label = { Text("This Week") })
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            MeetMindButton(
                text = "Apply Filters",
                onClick = onApply
            )
        }
    }
}
