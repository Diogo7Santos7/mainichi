package com.dayflow.app

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** One atomic private file; a failed write preserves the previous version. */
class TaskStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "tasks-v1.json"))

    fun load(): List<Task> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        val array = JSONArray(file.openRead().bufferedReader().use { it.readText() })
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val dates = item.getJSONArray("completedDates")
            Task(
                id = item.getString("id"), title = item.getString("title"),
                section = Section.valueOf(item.getString("section")),
                minute = item.getInt("minute"), duration = item.getInt("duration"),
                date = LocalDate.parse(item.getString("date")), category = item.getString("category"),
                notes = item.getString("notes"), location = item.getString("location"),
                completedDates = (0 until dates.length()).map { LocalDate.parse(dates.getString(it)) }.toSet()
            )
        }
    }

    fun save(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id); put("title", task.title); put("section", task.section.name)
                put("minute", task.minute); put("duration", task.duration); put("date", task.date.toString())
                put("category", task.category); put("notes", task.notes); put("location", task.location)
                put("completedDates", JSONArray(task.completedDates.map { it.toString() }))
            })
        }
        val stream = file.startWrite()
        try {
            stream.write(array.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}
