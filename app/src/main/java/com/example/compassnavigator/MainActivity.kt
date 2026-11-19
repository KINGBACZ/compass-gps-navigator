package com.example.compassnavigator

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.compassnavigator.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: PointStorage
    private lateinit var adapter: PointsAdapter
    private lateinit var gpsTracker: GpsTracker
    private lateinit var compass: CompassSensor

    private val points = mutableListOf<NavPoint>()
    private var selectedPoint: NavPoint? = null
    private var lastLocation: Location? = null
    private var azimuth: Float = 0f

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        startGps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = PointStorage(this)
        points.clear()
        points.addAll(storage.getAll())

        adapter = PointsAdapter(
            points = points,
            onClick = { point -> selectPoint(point) },
            onDelete = { point -> confirmDelete(point) }
        )
        binding.recyclerPoints.layoutManager = LinearLayoutManager(this)
        binding.recyclerPoints.adapter = adapter

        gpsTracker = GpsTracker(
            context = this,
            onLocation = { location ->
                lastLocation = location
                updateUi()
            },
            onStatus = { text ->
                binding.tvGpsStatus.text = text
            }
        )

        compass = CompassSensor(this) { degrees ->
            azimuth = degrees
            binding.ivCompass.rotation = -degrees
            binding.tvDirection.text = "${degrees.toInt()}°"
            updateUi()
        }

        binding.btnAddPoint.setOnClickListener { showAddDialog() }

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        updateUi()
    }

    override fun onResume() {
        super.onResume()
        compass.start()
        startGps()
    }

    override fun onPause() {
        super.onPause()
        compass.stop()
        gpsTracker.stop()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun startGps() {
        if (hasLocationPermission()) {
            gpsTracker.start()
        } else {
            binding.tvGpsStatus.text =
                "Нет разрешения на геолокацию. Включите его в настройках приложения"
        }
    }

    private fun selectPoint(point: NavPoint) {
        selectedPoint = point
        Toast.makeText(this, "Цель: ${point.name}", Toast.LENGTH_SHORT).show()
        updateUi()
    }

    private fun confirmDelete(point: NavPoint) {
        AlertDialog.Builder(this)
            .setTitle("Удалить точку?")
            .setMessage("«${point.name}» будет удалена без возможности восстановления.")
            .setPositiveButton("Удалить") { _, _ ->
                deletePoint(point)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deletePoint(point: NavPoint) {
        storage.delete(point)
        if (selectedPoint == point) {
            selectedPoint = null
        }
        refreshList()
        updateUi()
        Toast.makeText(this, "Точка удалена", Toast.LENGTH_SHORT).show()
    }

    private fun refreshList() {
        points.clear()
        points.addAll(storage.getAll())
        adapter.notifyDataSetChanged()
    }

    private fun updateUi() {
        val me = lastLocation
        val target = selectedPoint

        binding.tvTarget.text = target?.name ?: "Точка не выбрана"

        if (me == null) {
            binding.tvCoords.text = "Ожидание GPS"
            binding.tvDistance.text = "Ожидание сигнала GPS..."
            binding.ivTargetArrow.rotation = 0f
            return
        }

        binding.tvCoords.text = String.format(
            Locale.US, "%.5f, %.5f", me.latitude, me.longitude
        )

        if (target == null) {
            binding.tvDistance.text = "Выберите точку в списке"
            binding.ivTargetArrow.rotation = 0f
            return
        }

        val targetLocation = Location("target").apply {
            latitude = target.latitude
            longitude = target.longitude
        }

        val meters = me.distanceTo(targetLocation)
        binding.tvDistance.text = if (meters < 1000f) {
            "${meters.toInt()} м"
        } else {
            String.format(Locale.US, "%.2f км", meters / 1000f)
        }

        // Стрелка показывает направление на цель относительно верха телефона
        val bearing = me.bearingTo(targetLocation)
        binding.ivTargetArrow.rotation = ((bearing - azimuth) % 360f + 360f) % 360f
    }

    private fun showAddDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val etName = EditText(this).apply {
            hint = "Название"
        }
        val etLat = EditText(this).apply {
            hint = "Широта (например 55.751)"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val etLon = EditText(this).apply {
            hint = "Долгота (например 37.617)"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        container.addView(etName)
        container.addView(etLat)
        container.addView(etLon)

        AlertDialog.Builder(this)
            .setTitle("Новая точка")
            .setView(container)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val lat = etLat.text.toString().replace(',', '.').toDoubleOrNull()
                val lon = etLon.text.toString().replace(',', '.').toDoubleOrNull()

                if (name.isEmpty() || lat == null || lon == null ||
                    lat !in -90.0..90.0 || lon !in -180.0..180.0
                ) {
                    Toast.makeText(this, "Проверьте название и координаты", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val point = NavPoint(name, lat, lon)
                storage.add(point)
                refreshList()
                selectPoint(point)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}