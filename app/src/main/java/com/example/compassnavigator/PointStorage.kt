package com.example.compassnavigator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PointStorage(context: Context) {

    private val prefs = context.getSharedPreferences("points", Context.MODE_PRIVATE)

    fun getAll(): MutableList<NavPoint> {
        val list = mutableListOf<NavPoint>()
        val arr = JSONArray(prefs.getString("list", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(NavPoint(o.getString("name"), o.getDouble("lat"), o.getDouble("lon")))
        }
        return list
    }

    fun add(point: NavPoint) {
        val list = getAll()
        list.add(point)
        saveAll(list)
    }

    fun delete(point: NavPoint) {
        val list = getAll()
        val index = list.indexOfFirst {
            it.name == point.name &&
                    it.latitude == point.latitude &&
                    it.longitude == point.longitude
        }
        if (index >= 0) {
            list.removeAt(index)
            saveAll(list)
        }
    }

    private fun saveAll(list: List<NavPoint>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("lat", p.latitude)
                    .put("lon", p.longitude)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}