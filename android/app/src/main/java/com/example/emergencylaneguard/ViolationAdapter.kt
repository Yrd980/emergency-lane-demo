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
import com.bumptech.glide.Glide
import com.example.emergencylaneguard.database.ViolationRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.widget.CheckBox

import android.net.Uri

class ViolationAdapter(
    private val onClick: (ViolationRecord) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {},
    private val onModeChanged: (Boolean) -> Unit = {} // New callback
) : ListAdapter<ViolationRecord, ViolationAdapter.ViewHolder>(DiffCallback) {

    private var isSelectionMode = false
    private val selectedIds = HashSet<Long>()

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            if (!enabled) selectedIds.clear()
            notifyDataSetChanged()
            onSelectionChanged(selectedIds.size)
            onModeChanged(enabled) // Notify fragment
        }
    }

    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        notifyDataSetChanged() // Or notifyItemChanged for better performance
        onSelectionChanged(selectedIds.size)
    }

    fun selectAll() {
        currentList.forEach { selectedIds.add(it.id) }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun getSelectedIds(): List<Long> = selectedIds.toList()

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
        holder.tvFileName.text = item.plateNumber.ifEmpty { "Auto Detected" }
        holder.tvFileSize.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(item.timestamp))
        
        // Selection Mode Logic
        if (isSelectionMode) {
            holder.cbSelect.visibility = View.VISIBLE
            holder.cbSelect.isChecked = selectedIds.contains(item.id)
            holder.cbSelect.setOnClickListener { toggleSelection(item.id) }
        } else {
            holder.cbSelect.visibility = View.GONE
        }

        // Load image thumbnail (prefer imagePath, fallback to videoPath)
        val thumbPath = if (item.imagePath.isNotEmpty()) item.imagePath else item.videoPath
        
        if (thumbPath.startsWith("content://")) {
            Glide.with(holder.itemView)
                .load(Uri.parse(thumbPath))
                .centerCrop()
                .into(holder.ivThumb)
        } else {
            val thumbFile = File(thumbPath)
            if (thumbFile.exists()) {
                Glide.with(holder.itemView)
                    .load(thumbFile)
                    .centerCrop()
                    .into(holder.ivThumb)
            } else {
                // Set placeholder if file doesn't exist
                holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
            
        holder.itemView.setOnClickListener { 
            if (isSelectionMode) {
                toggleSelection(item.id)
            } else {
                onClick(item) 
            }
        }
        
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                setSelectionMode(true)
                toggleSelection(item.id)
                true
            } else {
                false
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ViolationRecord>() {
        override fun areItemsTheSame(oldItem: ViolationRecord, newItem: ViolationRecord) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ViolationRecord, newItem: ViolationRecord) = oldItem == newItem
    }
}
