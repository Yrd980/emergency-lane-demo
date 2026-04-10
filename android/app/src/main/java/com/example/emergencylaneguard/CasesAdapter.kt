package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.emergencylaneguard.database.CaseInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.widget.CheckBox

import android.util.Log

class CasesAdapter(
    private val onClick: (CaseInfo) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {},
    private val onModeChanged: (Boolean) -> Unit = {}
) : ListAdapter<CaseInfo, CasesAdapter.ViewHolder>(DiffCallback) {

    private var isSelectionMode = false
    private val selectedPlates = HashSet<String>()

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            if (!enabled) selectedPlates.clear()
            notifyDataSetChanged()
            onSelectionChanged(selectedPlates.size)
            onModeChanged(enabled)
        }
    }

    fun toggleSelection(plate: String) {
        if (selectedPlates.contains(plate)) {
            selectedPlates.remove(plate)
        } else {
            selectedPlates.add(plate)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedPlates.size)
    }

    fun selectAll() {
        currentList.forEach { selectedPlates.add(it.plateNumber) }
        notifyDataSetChanged()
        onSelectionChanged(selectedPlates.size)
    }

    fun getSelectedPlates(): List<String> = selectedPlates.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val ivThumb: ImageView = view.findViewById(R.id.ivThumb)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileSize: TextView = view.findViewById(R.id.tvFileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvFileName.text = item.plateNumber
        holder.tvFileSize.text = "Last updated: " + SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(item.updatedAt))
        
        holder.ivThumb.setImageResource(android.R.drawable.ic_menu_view)
        
        if (isSelectionMode) {
            holder.cbSelect.visibility = View.VISIBLE
            holder.cbSelect.isChecked = selectedPlates.contains(item.plateNumber)
            holder.cbSelect.setOnClickListener { toggleSelection(item.plateNumber) }
        } else {
            holder.cbSelect.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { 
            Log.d("CasesAdapter", "Item clicked: ${item.plateNumber}, SelectionMode: $isSelectionMode")
            if (isSelectionMode) {
                toggleSelection(item.plateNumber)
            } else {
                onClick(item) 
            }
        }
        
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                setSelectionMode(true)
                toggleSelection(item.plateNumber)
                true
            } else {
                false
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CaseInfo>() {
        override fun areItemsTheSame(oldItem: CaseInfo, newItem: CaseInfo) = oldItem.plateNumber == newItem.plateNumber
        override fun areContentsTheSame(oldItem: CaseInfo, newItem: CaseInfo) = oldItem == newItem
    }
}
