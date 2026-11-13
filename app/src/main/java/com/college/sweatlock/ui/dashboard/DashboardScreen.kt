package com.college.sweatlock.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.college.sweatlock.data.repository.InstalledAppInfo
import com.college.sweatlock.data.repository.SweatLockRepository
import com.college.sweatlock.ui.theme.GlassCard
import com.college.sweatlock.ui.theme.SoftLavender
import com.college.sweatlock.ui.theme.VioletAccent
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    repository: SweatLockRepository,
    onTestWorkoutClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val todayReps by repository.observeTodayReps().collectAsState(initial = 0)
    val streak by repository.observeStreakDays().collectAsState(initial = 0)
    val blockedEntities by repository.blockedAppsFlow.collectAsState(initial = emptyList())

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        installedApps = repository.getLaunchableApps()
    }

    val blockedMap = remember(blockedEntities) {
        blockedEntities.associateBy { it.packageName }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SweatLock", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = onTestWorkoutClick,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Test Camera", fontSize = 12.sp)
                }
            }
        }

        // iOS Control Center Style Activity Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("REPS TODAY", color = SoftLavender, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("$todayReps", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("ACTIVE STREAK", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("$streak days", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps to lock...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
        }

        items(filteredApps, key = { it.packageName }) { app ->
            val dbState = blockedMap[app.packageName]
            val isChecked = dbState?.isBlocked ?: false
            val remainingMin = ((dbState?.unlockUntilMs ?: 0L) - System.currentTimeMillis()) / 60_000L

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bitmap = remember(app.packageName) {
                        app.icon.toBitmap(96, 96, Bitmap.Config.ARGB_8888).asImageBitmap()
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (isChecked) {
                            Text(
                                text = if (remainingMin > 0) "Unlocked • ${remainingMin}m left" else "Locked",
                                color = if (remainingMin > 0) Color(0xFF4ADE80) else SoftLavender,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            scope.launch { repository.toggleAppBlocked(app.packageName, checked) }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = VioletAccent)
                    )
                }
            }
        }
    }
}
