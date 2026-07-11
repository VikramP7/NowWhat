package com.example.nowwhat

import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_VERSION = 1

data class BackupData(
    val activities: List<Activity>,
    val entries: List<HourEntry>,
    val schedule: List<ScheduleEntry>
)
fun toJson(data: BackupData): String{
    val root = JSONObject()
    root.put("BACKUP_VERSION", BACKUP_VERSION)
    val activitiesJsonArr = JSONArray()
    val entriesJsonArr = JSONArray()
    val scheduleJsonArr = JSONArray()

    data.activities.forEach { activity ->
        val activityJson = JSONObject()
        activityJson.put("id", activity.id)
        activityJson.put("name", activity.name)
        activityJson.put("colour", activity.colour)

        activitiesJsonArr.put(activityJson)
    }

    data.entries.forEach { entry ->
        val entryJson = JSONObject()
        entryJson.put("id", entry.id)
        entryJson.put("timestamp", entry.timestamp)
        entryJson.put("actualActivityId", entry.actualActivityId ?: JSONObject.NULL)
        entryJson.put("plannedActivityId", entry.plannedActivityId ?: JSONObject.NULL)

        entriesJsonArr.put(entryJson)
    }

    data.schedule.forEach { scheduleEntry ->
        val scheduleEntryJson = JSONObject()
        scheduleEntryJson.put("id", scheduleEntry.id)
        scheduleEntryJson.put("plannedActivityId", scheduleEntry.plannedActivityId?: JSONObject.NULL)
        scheduleEntryJson.put("hourOfDay", scheduleEntry.hourOfDay)
        scheduleEntryJson.put("dayOfWeek", scheduleEntry.dayOfWeek)

        scheduleJsonArr.put(scheduleEntryJson)
    }

    root.put("activities", activitiesJsonArr)
    root.put("entries", entriesJsonArr)
    root.put("schedule", scheduleJsonArr)

    return root.toString()
}

fun fromJson(text: String): BackupData {
    val root = JSONObject(text)
    val importedBackupVersion = root.getInt("BACKUP_VERSION")
    val activitiesJsonArr = root.getJSONArray("activities")
    val entriesJsonArr = root.getJSONArray("entries")
    val scheduleJsonArr = root.getJSONArray("schedule")

    val activities = mutableListOf<Activity>()
    for (i in 0 ..< activitiesJsonArr.length()){
        val activityJsonObj = activitiesJsonArr.getJSONObject(i)
        val activity = Activity(
            name = activityJsonObj.getString("name"),
            colour = activityJsonObj.getInt("colour"),
            id = activityJsonObj.getLong("id"),
        )
        activities.add(activity)
    }

    val entries = mutableListOf<HourEntry>()
    for (i in 0 ..< entriesJsonArr.length()){
        val entryJsonObj = entriesJsonArr.getJSONObject(i)
        val entry = HourEntry(
            id = entryJsonObj.getLong("id"),
            timestamp = entryJsonObj.getLong("timestamp"),
            plannedActivityId = if (entryJsonObj.isNull("plannedActivityId")) null else entryJsonObj.getLong("plannedActivityId"),
            actualActivityId = if (entryJsonObj.isNull("actualActivityId")) null else entryJsonObj.getLong("actualActivityId")
        )
        entries.add(entry)
    }

    val schedule = mutableListOf<ScheduleEntry>()
    for (i in 0 ..< scheduleJsonArr.length()){
        val scheduleEntryJsonObj = scheduleJsonArr.getJSONObject(i)
        val scheduleEntry = ScheduleEntry(
            id = scheduleEntryJsonObj.getLong("id"),
            plannedActivityId = if (scheduleEntryJsonObj.isNull("plannedActivityId")) null else scheduleEntryJsonObj.getLong("plannedActivityId"),
            hourOfDay = scheduleEntryJsonObj.getInt("hourOfDay"),
            dayOfWeek = scheduleEntryJsonObj.getInt("dayOfWeek")
        )
        schedule.add(scheduleEntry)
    }

    return BackupData(activities, entries, schedule)
}