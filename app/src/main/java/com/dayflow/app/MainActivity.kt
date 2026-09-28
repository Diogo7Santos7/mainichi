package com.dayflow.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Pine = Color(0xFF285C50)
private val Cream = Color(0xFFF7F5EF)
private val Lime = Color(0xFFD8EB9C)
private fun clock(minute: Int) = LocalTime.of(minute / 60, minute % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
private fun Section.icon(): ImageVector = when (this) {
    Section.ROUTINE -> Icons.Outlined.WbSunny
    Section.FOCUS -> Icons.Outlined.Checklist
    Section.EVENTS -> Icons.Outlined.Event
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Pine, onPrimary = Color.White, primaryContainer = Lime,
                onPrimaryContainer = Pine, background = Cream, surface = Cream,
                surfaceVariant = Color(0xFFEAEDE5), secondaryContainer = Color(0xFFE1EADD)
            )) { DayflowApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayflowApp() {
    val context = LocalContext.current
    val store = remember { TaskStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var sectionName by rememberSaveable { mutableStateOf(Section.ROUTINE.name) }
    val section = Section.valueOf(sectionName)
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val day = LocalDate.ofEpochDay(selectedEpoch)
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = tasks.find { it.id == editingId }
    var deleting by remember { mutableStateOf<Task?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)

    fun refreshDay() {
        val current = LocalDate.now()
        if (current != today) {
            if (selectedEpoch == today.toEpochDay()) selectedEpoch = current.toEpochDay()
            today = current
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshDay() }
    LaunchedEffect(Unit) { while (true) { delay(30_000); refreshDay() } }
    LaunchedEffect(loadAttempt) {
        loading = true
        try {
            tasks = withContext(Dispatchers.IO) { store.load() }
            loadError = false
        } catch (_: Exception) { loadError = true }
        loading = false
    }

    fun persist(next: List<Task>, after: () -> Unit = {}) {
        if (busy || loading || loadError) return
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(next) }
                tasks = next
                after()
            } catch (_: Exception) {
                snackbar.showSnackbar("Couldn't save changes. Please try again.")
            } finally { busy = false }
        }
    }

    val visible = tasksForDay(tasks, section, day)
    val done = visible.count { it.isDone(day) }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet {
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.app_name), Modifier.padding(horizontal = 28.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Pine)
            Text("A little structure. More life.", Modifier.padding(28.dp, 8.dp, 24.dp, 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Section.entries.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label) }, selected = section == item,
                    icon = { Icon(item.icon(), null) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { sectionName = item.name; scope.launch { drawer.close() } }
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Your day, at your pace.\nSaved on this device.", Modifier.padding(28.dp), style = MaterialTheme.typography.bodySmall)
        }
    }) {
        Scaffold(
            containerColor = Cream,
            topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = Pine) },
                navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Outlined.Menu, "Open navigation drawer") } },
                actions = { TextButton(onClick = { selectedEpoch = today.toEpochDay() }) { Text("Today") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)) },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (!loading && !loadError && !editorOpen) ExtendedFloatingActionButton(
                    onClick = { if (!busy) { editingId = null; editorOpen = true } },
                    icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(if (section == Section.ROUTINE) "Add habit" else if (section == Section.EVENTS) "Add event" else "Add task") },
                    containerColor = Pine, contentColor = Color.White)
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("YOUR DAY, INTENTIONALLY", color = Pine, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(section.label, Modifier.padding(top = 8.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
                    Text(section.subtitle, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { selectedEpoch-- }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Previous day") }
                        TextButton(onClick = {
                            DatePickerDialog(context, { _, y, m, d -> selectedEpoch = LocalDate.of(y, m + 1, d).toEpochDay() }, day.year, day.monthValue - 1, day.dayOfMonth).show()
                        }, modifier = Modifier.weight(1f)) {
                            Text(day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { selectedEpoch++ }) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Next day") }
                    }
                }
                if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                else if (loadError) item {
                    EmptyCard("Couldn't open your planner", "Your saved file has been left untouched.")
                    TextButton(onClick = { loadAttempt++ }) { Text("Try again") }
                } else {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Pine), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(24.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (visible.isNotEmpty() && done == visible.size) "Nicely done." else "One thing at a time.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                                        Text("$done of ${visible.size} completed", Modifier.padding(top = 8.dp), color = Color(0xFFDCE8E0))
                                    }
                                    Text("${if (visible.isEmpty()) 0 else done * 100 / visible.size}%", color = Lime, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(progress = { if (visible.isEmpty()) 0f else done.toFloat() / visible.size }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(6.dp), color = Lime, trackColor = Color(0xFF4B756A))
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Your timeline", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if (section == Section.ROUTINE) "REPEATS DAILY" else "${visible.size} PLANNED", color = Pine, fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                    }
                    if (visible.isEmpty()) item {
                        EmptyCard(if (section == Section.ROUTINE) "Build your daily rhythm" else "A little breathing room", if (section == Section.ROUTINE) "Add a habit to begin. It will appear every day from its start date." else "No plans for this date. Add something when you're ready.")
                        if (tasks.isEmpty()) OutlinedButton(enabled = !busy, onClick = { persist(sampleTasks(today)) }) { Text("Try a sample routine & tasks") }
                    }
                    items(visible, key = { it.id }) { task ->
                        TaskCard(task, day, !busy, onToggle = { persist(tasks.map { if (it.id == task.id) it.toggle(day) else it }) },
                            onEdit = { editingId = task.id; editorOpen = true }, onDelete = { deleting = task })
                    }
                }
            }
        }
    }
    if (editorOpen && !loading && !loadError) TaskEditor(editing, section, day, busy, onDismiss = { if (!busy) editorOpen = false }, onSave = { updated ->
        persist(if (editing == null) tasks + updated else tasks.map { if (it.id == updated.id) updated else it }) { editorOpen = false }
    })
    deleting?.let { task ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("Delete ${if (task.section == Section.ROUTINE) "habit" else "plan"}?") },
            text = { Text(if (task.section == Section.ROUTINE) "“${task.title}” and all its completion history will be removed from every day." else "“${task.title}” will be removed from your planner.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { persist(tasks.filterNot { it.id == task.id }) { deleting = null } }) { Text("Delete") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Icon(Icons.Outlined.Spa, null, tint = Pine, modifier = Modifier.size(32.dp))
            Text(title, Modifier.padding(top = 14.dp), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(body, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskCard(task: Task, day: LocalDate, enabled: Boolean, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val done = task.isDone(day)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(61.dp).padding(top = 20.dp)) {
            Text(clock(task.minute), color = Pine, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${task.duration} min", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Surface(color = if (done) Color(0xFFEBF0E6) else Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.clickable(enabled = enabled, onClick = onEdit).padding(start = 16.dp, top = 10.dp, bottom = 14.dp, end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.category.uppercase(), Modifier.weight(1f), color = Pine, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                    Box {
                        IconButton(enabled = enabled, onClick = { menu = true }) { Icon(Icons.Outlined.MoreHoriz, "Options for ${task.title}") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
                    IconToggleButton(checked = done, enabled = enabled, onCheckedChange = { onToggle() }) {
                        Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, if (done) "Mark ${task.title} incomplete" else "Complete ${task.title}", tint = Pine)
                    }
                }
                if (task.location.isNotBlank()) Text(task.location, color = Pine, fontSize = 12.sp)
                if (task.notes.isNotBlank()) Text(task.notes, Modifier.padding(top = 4.dp, end = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditor(existing: Task?, section: Section, selectedDay: LocalDate, busy: Boolean, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var minute by rememberSaveable { mutableIntStateOf(existing?.minute ?: 540) }
    var duration by rememberSaveable { mutableStateOf((existing?.duration ?: 15).toString()) }
    var dateEpoch by rememberSaveable { mutableLongStateOf((existing?.date ?: selectedDay).toEpochDay()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    val categories = when (section) {
        Section.ROUTINE -> listOf("Wellbeing", "Hygiene", "Fitness", "Personal")
        Section.FOCUS -> listOf("Work", "Study", "Chores", "Personal")
        Section.EVENTS -> listOf("Appointment", "Event", "Social", "Personal")
    }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: categories.first()) }
    val date = LocalDate.ofEpochDay(dateEpoch)
    val validDuration = duration.toIntOrNull()?.let { it in 1..1440 } == true
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Cream) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (existing == null) "Make a little plan" else "Edit your plan", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(section.label, color = Pine)
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { name -> FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }, enabled = !busy) }
                }
                OutlinedButton(enabled = !busy, onClick = {
                    DatePickerDialog(context, { _, y, m, d -> dateEpoch = LocalDate.of(y, m + 1, d).toEpochDay() }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("${if (section == Section.ROUTINE) "Starts" else "Date"}: ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}") }
                OutlinedButton(enabled = !busy, onClick = {
                    TimePickerDialog(context, { _, h, m -> minute = h * 60 + m }, minute / 60, minute % 60, true).show()
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Schedule, null); Spacer(Modifier.width(8.dp)); Text("Time: ${clock(minute)}") }
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4) }, label = { Text("Duration (minutes)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    supportingText = { Text("1–1440 minutes") }, isError = !validDuration, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                if (section == Section.EVENTS) OutlinedTextField(location, { location = it.take(200) }, label = { Text("Location (optional)") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(notes, { notes = it.take(2000) }, label = { Text("Notes (optional)") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                if (section == Section.ROUTINE) Text("Repeats every day. Each day's completion is separate. Edits apply to the entire habit.", style = MaterialTheme.typography.bodySmall, color = Pine)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
                    Button(enabled = title.isNotBlank() && validDuration && !busy, onClick = {
                        val base = existing ?: Task(title = title.trim(), section = section, minute = minute)
                        // Dated plans moved to a new day start incomplete.
                        onSave(base.copy(title = title.trim(), minute = minute, duration = duration.toInt(), date = date,
                            category = category, notes = notes.trim(), location = location.trim(),
                            completedDates = if (section != Section.ROUTINE && base.date != date) emptySet() else base.completedDates))
                    }) { Text(if (busy) "Saving…" else "Save") }
                }
            }
        }
    }
}
