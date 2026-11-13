package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AppInfo
import kotlinx.coroutines.delay

@Composable
fun MindfulLaunchDialog(
  app: AppInfo,
  onStayPresent: () -> Unit,
  onContinueToApp: () -> Unit,
) {
  var remainingSeconds by remember(app.componentKey) { mutableIntStateOf(3) }

  LaunchedEffect(app.componentKey) {
    remainingSeconds = 3
    while (remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds -= 1
    }
  }

  val breathTransition = rememberInfiniteTransition(label = "mindful_breath")
  val breathScale by
    breathTransition.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.14f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "breath_scale",
    )

  val accentColor = MaterialTheme.colorScheme.primary

  Dialog(onDismissRequest = onStayPresent) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().testTag("mindful_pause_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier.size(88.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier
                .size(76.dp)
                .scale(breathScale)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
          )
          FramedAppIcon(
            app = app,
            frameSize = 48.dp,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Take a moment",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(6.dp))

        val countCopy =
          if (app.dailyLaunchCount == 0) {
            "Are you sure you want to open ${app.label}?"
          } else {
            "You've opened ${app.label} ${app.dailyLaunchCount}× today."
          }
        Text(
          text = countCopy,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onStayPresent,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("mindful_stay_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.SelfImprovement,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Wait & Stay Present")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onContinueToApp,
          enabled = remainingSeconds <= 0,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("mindful_continue_button"),
        ) {
          Text(
            text =
              if (remainingSeconds > 0) "Continue in ${remainingSeconds}s..."
              else "Continue to ${app.label}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun RenameAppDialog(
  app: AppInfo,
  onSave: (String?) -> Unit,
  onDismiss: () -> Unit,
) {
  var nameInput by remember(app.componentKey) { mutableStateOf(app.label) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Rename app",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Original: ${app.originalLabel}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          singleLine = true,
          placeholder = { Text(app.originalLabel) },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("rename_app_input"),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          if (app.label != app.originalLabel) {
            TextButton(onClick = { onSave(null) }) {
              Text("Reset")
            }
          }
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          TextButton(
            onClick = {
              val trimmed = nameInput.trim()
              onSave(if (trimmed.isEmpty() || trimmed == app.originalLabel) null else trimmed)
            },
            modifier = Modifier.testTag("rename_app_save_button"),
          ) {
            Text("Save", color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

@Composable
fun HiddenAppsDialog(
  hiddenApps: List<AppInfo>,
  onUnhide: (AppInfo) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Hidden apps",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (hiddenApps.isEmpty()) {
          Text(
            text = "No apps are hidden. Long-press any app in All apps to hide it from the drawer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 20.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(
              items = hiddenApps,
              key = { it.componentKey },
              contentType = { "hidden_app" },
            ) { app ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                FramedAppIcon(app = app, frameSize = 38.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = app.label,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onUnhide(app) }) {
                  Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Show")
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Done") }
        }
      }
    }
  }
}

@Composable
fun MindfulAppsPickerDialog(
  allApps: List<AppInfo>,
  pausedKeys: Set<String>,
  onSave: (List<AppInfo>) -> Unit,
  onDismiss: () -> Unit,
  title: String = "Mindful pause apps",
  subtitle: String = "Select apps that show a brief breathing pause before opening.",
) {
  var filterQuery by remember { mutableStateOf("") }
  var selectedKeys by remember(pausedKeys) { mutableStateOf(pausedKeys) }
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
      modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = filterQuery,
          onValueChange = { filterQuery = it },
          singleLine = true,
          placeholder = { Text("Find an app") },
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
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(
            items = filtered,
            key = { it.componentKey },
            contentType = { "mindful_app" },
          ) { app ->
            val checked = app.componentKey in selectedKeys
            Surface(
              onClick = {
                selectedKeys =
                  if (checked) selectedKeys - app.componentKey
                  else selectedKeys + app.componentKey
              },
              shape = RoundedCornerShape(8.dp),
              color =
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                FramedAppIcon(app = app, frameSize = 38.dp)
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = {
              onSave(allApps.filter { it.componentKey in selectedKeys })
            }
          ) {
            Text("Save", color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}
