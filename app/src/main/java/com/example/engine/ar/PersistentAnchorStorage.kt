package com.example.engine.ar

import android.content.Context
import android.content.SharedPreferences
import com.example.math3d.Vec3
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistent storage for AR Anchors, Cloud Anchor IDs and Geospatial coordinates.
 */
class PersistentAnchorStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ar_persistent_anchors", Context.MODE_PRIVATE)

    fun saveAnchor(anchorData: PersistentARAnchorData) {
        val list = getAllAnchors().toMutableList()
        list.removeAll { it.id == anchorData.id }
        list.add(0, anchorData)
        saveList(list)
    }

    fun removeAnchor(id: String) {
        val list = getAllAnchors().toMutableList()
        list.removeAll { it.id == id }
        saveList(list)
    }

    fun clearAll() {
        prefs.edit().remove("anchors_json").apply()
    }

    fun getAllAnchors(): List<PersistentARAnchorData> {
        val jsonStr = prefs.getString("anchors_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val result = mutableListOf<PersistentARAnchorData>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    PersistentARAnchorData(
                        id = obj.optString("id", "anchor_$i"),
                        modelName = obj.optString("modelName", "Model"),
                        posX = obj.optDouble("posX", 0.0).toFloat(),
                        posY = obj.optDouble("posY", 0.0).toFloat(),
                        posZ = obj.optDouble("posZ", 1.5).toFloat(),
                        rotY = obj.optDouble("rotY", 0.0).toFloat(),
                        scale = obj.optDouble("scale", 1.0).toFloat(),
                        cloudAnchorId = if (obj.has("cloudAnchorId")) obj.getString("cloudAnchorId") else null,
                        latitude = if (obj.has("latitude")) obj.getDouble("latitude") else null,
                        longitude = if (obj.has("longitude")) obj.getDouble("longitude") else null,
                        altitude = if (obj.has("altitude")) obj.getDouble("altitude") else null,
                        hitType = try { ARHitType.valueOf(obj.optString("hitType", "PLANE_POLYGON")) } catch (e: Exception) { ARHitType.PLANE_POLYGON },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveList(list: List<PersistentARAnchorData>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("modelName", item.modelName)
                put("posX", item.posX.toDouble())
                put("posY", item.posY.toDouble())
                put("posZ", item.posZ.toDouble())
                put("rotY", item.rotY.toDouble())
                put("scale", item.scale.toDouble())
                item.cloudAnchorId?.let { put("cloudAnchorId", it) }
                item.latitude?.let { put("latitude", it) }
                item.longitude?.let { put("longitude", it) }
                item.altitude?.let { put("altitude", it) }
                put("hitType", item.hitType.name)
                put("timestamp", item.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("anchors_json", arr.toString()).apply()
    }
}
