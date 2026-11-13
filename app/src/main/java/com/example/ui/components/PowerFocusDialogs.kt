package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.WeatherRepository
import com.example.model.AppFolder
import com.example.model.AppInfo
import com.example.model.MindfulInsights
import com.example.model.WeatherLocation
import com.example.ui.theme.GeistMonoNumericStyle
import com.example.util.FocusAndSearchUtils
import com.example.util.LauncherHapticEvent
import com.example.util.rememberLauncherHaptics
import kotlinx.coroutines.launch

@Composable
fun FolderEditorDialog(
  existingFolder: AppFolder?,
  allApps: List<AppInfo>,
  onSave: (name: String, selectedApps: List<AppInfo>) -> Unit,
  onDelete: (() -> Unit)?,
  onDismiss: () -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  var folderName by remember(existingFolder) { mutableStateOf(existingFolder?.name ?: "") }
  var filterQuery by remember { mutableStateOf("") }
  var selectedKeys by remember(existingFolder) {
    mutableStateOf(existingFolder?.componentKeys?.toSet() ?: emptySet())
  }
  val filtered =
    remember(allApps, filterQuery) {
      val trimmed = filterQuery.trim()
      if (trimmed.isEmpty()) allApps
      else allApps.filter { it.label.contains(trimmed, ignoreCase = true) }
    }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp).testTag("folder_editor_dialog"),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = if (existingFolder == null) "Create folder" else "Edit folder",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = folderName,
          onValueChange = { folderName = it },
          singleLine = true,
          label = { Text("Folder name") },
          placeholder = { Text("Work, Finance, Tools...") },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("folder_name_input"),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = filterQuery,
          onValueChange = { filterQuery = it },
          singleLine = true,
          placeholder = { Text("Filter apps") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            if (filterQuery.isNotEmpty()) {
              IconButton(onClick = { filterQuery = "" }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear filter")
              }
            }
          },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(filtered, key = { it.componentKey }, contentType = { "folder_app" }) { app ->
            val checked = app.componentKey in selectedKeys
            Surface(
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
                selectedKeys =
                  if (checked) selectedKeys - app.componentKey
                  else selectedKeys + app.componentKey
              },
              shape = RoundedCornerShape(8.dp),
              color =
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent,
              modifier = Modifier.fillMaxWidth().testTag("folder_app_pick_${app.packageName}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                FramedAppIcon(app = app, frameSize = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = app.label,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                if (checked) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          if (onDelete != null) {
            TextButton(
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
                onDelete()
              }
            ) {
              Text("Delete", color = MaterialTheme.colorScheme.error)
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }
          Row {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(
              enabled = folderName.trim().isNotEmpty() && selectedKeys.isNotEmpty(),
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
                onSave(folderName.trim(), allApps.filter { it.componentKey in selectedKeys })
              },
              modifier = Modifier.testTag("folder_save_button"),
            ) {
              Text("Save", color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }
  }
}

@Composable
fun WeatherCityPickerDialog(
  currentLocation: WeatherLocation,
  onSearchCities: suspend (String) -> List<WeatherLocation>,
  onSelectLocation: (WeatherLocation) -> Unit,
  onDismiss: () -> Unit,
  useDeviceLocation: Boolean = false,
  isDetectingLocation: Boolean = false,
  locationStatusMessage: String? = null,
  onUseDeviceLocation: () -> Unit = {},
) {
  val context = LocalContext.current
  val launcherHaptics = rememberLauncherHaptics()
  var query by remember { mutableStateOf("") }
  var isSearching by remember { mutableStateOf(false) }
  var searchResults by remember { mutableStateOf<List<WeatherLocation>>(emptyList()) }
  var deniedHint by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        deniedHint = null
        onUseDeviceLocation()
      } else {
        deniedHint = "Location permission denied — search a city manually below"
      }
    }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp).testTag("weather_city_dialog"),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Weather location",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text =
            buildString {
              append("Current: ${currentLocation.name}")
              if (currentLocation.region.isNotEmpty()) append(" (${currentLocation.region})")
              if (useDeviceLocation) append(" · Device")
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          onClick = {
            launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
            deniedHint = null
            val hasCoarse =
              ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            val hasFine =
              ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (hasCoarse || hasFine) {
              onUseDeviceLocation()
            } else {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
          },
          shape = RoundedCornerShape(10.dp),
          color =
            if (useDeviceLocation) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant,
          border =
            BorderStroke(
              1.dp,
              if (useDeviceLocation) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.fillMaxWidth().testTag("weather_use_device_location_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.MyLocation,
              contentDescription = null,
              tint =
                if (useDeviceLocation) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Use device location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              val subtitle =
                deniedHint
                  ?: locationStatusMessage
                  ?: if (useDeviceLocation) "Auto-synced from GPS & network"
                  else "Detect current city from this device"
              Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (isDetectingLocation) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
              )
            } else if (useDeviceLocation) {
              Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Active",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search city...") },
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              scope.launch {
                isSearching = true
                searchResults = onSearchCities(query)
                isSearching = false
              }
            },
            enabled = query.trim().length >= 2 && !isSearching,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
          ) {
            if (isSearching) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Text("Find")
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val displayed =
          if (searchResults.isNotEmpty()) searchResults else WeatherRepository.PRESET_CITIES
        Text(
          text = if (searchResults.isNotEmpty()) "SEARCH RESULTS" else "QUICK CITIES",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(
            items = displayed,
            key = { "${it.name}_${it.latitude}_${it.longitude}" },
          ) { city ->
            val selected = city.name == currentLocation.name && city.region == currentLocation.region
            Surface(
              onClick = {
                launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
                onSelectLocation(city)
              },
              shape = RoundedCornerShape(8.dp),
              color =
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.LocationOn,
                  contentDescription = null,
                  tint =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = city.name, style = MaterialTheme.typography.titleMedium)
                  if (city.region.isNotEmpty()) {
                    Text(
                      text = city.region,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                if (selected) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}

@Composable
fun FocusScheduleDialog(
  enabled: Boolean,
  startMinutes: Int,
  endMinutes: Int,
  onSave: (enabled: Boolean, startMinutes: Int, endMinutes: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  var scheduleEnabled by remember(enabled) { mutableStateOf(enabled) }
  var startMin by remember(startMinutes) { mutableIntStateOf(startMinutes) }
  var endMin by remember(endMinutes) { mutableIntStateOf(endMinutes) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(text = "Focus schedule", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Automatically enable Focus Mode during your quiet work or study hours.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Scheduled hours",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          Switch(
            checked = scheduleEnabled,
            onCheckedChange = {
              launcherHaptics.trigger(LauncherHapticEvent.TOGGLE_SWITCH)
              scheduleEnabled = it
            },
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TimeAdjustRow(
          label = "Starts",
          minutes = startMin,
          enabled = scheduleEnabled,
          onAdjust = { delta -> startMin = ((startMin + delta) % 1440 + 1440) % 1440 },
        )
        Spacer(modifier = Modifier.height(8.dp))
        TimeAdjustRow(
          label = "Ends",
          minutes = endMin,
          enabled = scheduleEnabled,
          onAdjust = { delta -> endMin = ((endMin + delta) % 1440 + 1440) % 1440 },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
              onSave(scheduleEnabled, startMin, endMin)
            }
          ) {
            Text("Save", color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

@Composable
private fun TimeAdjustRow(
  label: String,
  minutes: Int,
  enabled: Boolean,
  onAdjust: (Int) -> Unit,
) {
  val launcherHaptics = rememberLauncherHaptics()
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    IconButton(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SCRUBBER_TICK)
        onAdjust(-30)
      },
      enabled = enabled,
    ) {
      Icon(Icons.Outlined.Remove, contentDescription = "Decrease $label time")
    }
    Text(
      text = FocusAndSearchUtils.formatMinutesOfDay(minutes),
      style = GeistMonoNumericStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
      modifier = Modifier.padding(horizontal = 8.dp),
    )
    IconButton(
      onClick = {
        launcherHaptics.trigger(LauncherHapticEvent.SCRUBBER_TICK)
        onAdjust(30)
      },
      enabled = enabled,
    ) {
      Icon(Icons.Outlined.Add, contentDescription = "Increase $label time")
    }
  }
}

@Composable
fun MindfulInsightsDialog(
  insights: MindfulInsights,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().testTag("mindful_insights_dialog"),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.SelfImprovement,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = "Today's Mindful Summary", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          InsightStatPill("Launches", insights.totalLaunchesToday.toString(), Modifier.weight(1f))
          InsightStatPill("Stayed present", insights.stayedPresentToday.toString(), Modifier.weight(1f))
          InsightStatPill("Continued", insights.continuedAnywayToday.toString(), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "MOST OPENED TODAY",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (insights.topLaunchedApps.isEmpty()) {
          Text(
            text = "No app launches recorded yet today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          insights.topLaunchedApps.forEach { (app, count) ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FramedAppIcon(app = app, frameSize = 32.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = "${count}×",
                style = GeistMonoNumericStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}

@Composable
private fun InsightStatPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun BackupRestoreDialog(
  exportedJson: String,
  onImportJson: suspend (String) -> Boolean,
  onRestoreSuccess: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val launcherHaptics = rememberLauncherHaptics()
  val scope = rememberCoroutineScope()
  var jsonInput by remember(exportedJson) { mutableStateOf(exportedJson) }
  var copiedFeedback by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp).testTag("backup_restore_dialog"),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Backup & restore",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Copy or share your configuration JSON, or paste a saved backup below.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
              clipboard.setText(AnnotatedString(exportedJson))
              copiedFeedback = true
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("copy_backup_button"),
          ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copiedFeedback) "Copied" else "Copy JSON")
          }
          OutlinedButton(
            onClick = {
              launcherHaptics.trigger(LauncherHapticEvent.SUBTLE_TAP)
              val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "Vesper Backup")
                  putExtra(Intent.EXTRA_TEXT, exportedJson)
                }
              runCatching {
                context.startActivity(Intent.createChooser(shareIntent, "Share launcher backup"))
              }
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share")
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = jsonInput,
          onValueChange = {
            jsonInput = it
            errorMessage = null
          },
          label = { Text("Backup JSON payload") },
          textStyle = GeistMonoNumericStyle.copy(fontSize = 11.sp, lineHeight = 15.sp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().height(180.dp).testTag("backup_json_input"),
        )
        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = {
              jsonInput = ""
              errorMessage = null
            },
          ) {
            Text("Clear")
          }
          Row {
            TextButton(onClick = onDismiss) { Text("Close") }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                scope.launch {
                  val restored = onImportJson(jsonInput)
                  if (restored) {
                    launcherHaptics.trigger(LauncherHapticEvent.CONFIRM_ACTION)
                    onRestoreSuccess()
                  } else {
                    launcherHaptics.trigger(LauncherHapticEvent.ERROR_FEEDBACK)
                    errorMessage = "Invalid backup JSON. Please check formatting."
                  }
                }
              },
              enabled = jsonInput.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("restore_backup_button"),
            ) {
              Text("Restore")
            }
          }
        }
      }
    }
  }
}
