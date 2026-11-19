package com.example.compassnavigator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.compassnavigator.databinding.ItemPointBinding
import java.util.Locale

class PointsAdapter(
    private val points: MutableList<NavPoint>,
    private val onClick: (NavPoint) -> Unit,
    private val onDelete: (NavPoint) -> Unit
) : RecyclerView.Adapter<PointsAdapter.PointViewHolder>() {

    inner class PointViewHolder(val binding: ItemPointBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointViewHolder {
        val binding = ItemPointBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PointViewHolder(binding)
    }

    override fun getItemCount(): Int = points.size

    override fun onBindViewHolder(holder: PointViewHolder, position: Int) {
        val point = points[position]
        holder.binding.tvPointName.text = point.name
        holder.binding.tvPointCoords.text =
            String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)
        holder.binding.root.setOnClickListener { onClick(point) }
        holder.binding.btnDeletePoint.setOnClickListener { onDelete(point) }
    }
}