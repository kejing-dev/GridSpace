package com.kejing.gridspace.data

import com.kejing.gridspace.domain.model.Cell
import com.kejing.gridspace.domain.model.ColorMath
import com.kejing.gridspace.domain.model.Palette
import com.kejing.gridspace.domain.model.Project
import com.kejing.gridspace.domain.model.ProjectSummary
import com.kejing.gridspace.domain.model.VoxelGrid
import org.json.JSONArray
import org.json.JSONObject

/**
 * Project file format (design spec G.3), version 1:
 * {"version":1,"id","name","gridSize","slots":["#rrggbb" x3],"activeSlot":0,
 *  "voxels":[[x,y,z,"#rrggbb"],...],"createdAt","updatedAt"}
 */
object ProjectJson {
    private const val VERSION = 1

    fun encode(project: Project): String {
        val voxels = JSONArray()
        for ((cell, color) in project.voxels.cells) {
            voxels.put(JSONArray().put(cell.x).put(cell.y).put(cell.z).put(ColorMath.toHex(color)))
        }
        val slots = JSONArray()
        project.slots.forEach { slots.put(ColorMath.toHex(it)) }
        return JSONObject()
            .put("version", VERSION)
            .put("id", project.id)
            .put("name", project.name)
            .put("gridSize", project.gridSize)
            .put("slots", slots)
            .put("activeSlot", project.activeSlot)
            .put("voxels", voxels)
            .put("createdAt", project.createdAt)
            .put("updatedAt", project.updatedAt)
            .toString()
    }

    /** Throws on malformed input; the caller turns that into a "file damaged" row. */
    fun decode(text: String): Project {
        val json = JSONObject(text)
        val size = json.getInt("gridSize")
        require(size in VoxelGrid.SIZES) { "Unsupported grid size $size" }
        val cells = HashMap<Cell, Int>()
        val voxels = json.getJSONArray("voxels")
        for (i in 0 until voxels.length()) {
            val v = voxels.getJSONArray(i)
            val cell = Cell(v.getInt(0), v.getInt(1), v.getInt(2))
            if (cell.inBounds(size)) cells[cell] = ColorMath.parseHex(v.getString(3))
        }
        val slotsJson = json.optJSONArray("slots")
        val slots =
            if (slotsJson != null && slotsJson.length() == Palette.SLOT_COUNT) {
                List(Palette.SLOT_COUNT) { ColorMath.parseHex(slotsJson.getString(it)) }
            } else {
                Palette.DEFAULT_SLOTS
            }
        return Project(
            id = json.getString("id"),
            name = json.getString("name"),
            gridSize = size,
            voxels = VoxelGrid(size, cells),
            slots = slots,
            activeSlot = json.optInt("activeSlot", 0).coerceIn(0, Palette.SLOT_COUNT - 1),
            createdAt = json.optLong("createdAt", 0L),
            updatedAt = json.optLong("updatedAt", 0L),
        )
    }

    fun encodeIndex(entries: List<ProjectSummary>): String {
        val array = JSONArray()
        for (e in entries) {
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("gridSize", e.gridSize)
                    .put("voxelCount", e.voxelCount)
                    .put("dominantColor", e.dominantColor?.let { ColorMath.toHex(it) } ?: JSONObject.NULL)
                    .put("updatedAt", e.updatedAt)
                    .put("corrupt", e.corrupt)
            )
        }
        return JSONObject().put("version", VERSION).put("projects", array).toString()
    }

    fun decodeIndex(text: String): List<ProjectSummary> {
        val array = JSONObject(text).getJSONArray("projects")
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            ProjectSummary(
                id = o.getString("id"),
                name = o.getString("name"),
                gridSize = o.getInt("gridSize"),
                voxelCount = o.getInt("voxelCount"),
                dominantColor = if (o.isNull("dominantColor")) null else ColorMath.parseHex(o.getString("dominantColor")),
                updatedAt = o.getLong("updatedAt"),
                corrupt = o.optBoolean("corrupt", false),
            )
        }
    }

    fun summaryOf(project: Project): ProjectSummary =
        ProjectSummary(
            id = project.id,
            name = project.name,
            gridSize = project.gridSize,
            voxelCount = project.voxels.count,
            dominantColor = project.voxels.dominantColor(),
            updatedAt = project.updatedAt,
        )
}
