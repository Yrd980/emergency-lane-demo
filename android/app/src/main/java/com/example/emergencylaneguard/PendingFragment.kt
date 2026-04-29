package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.widget.Button
import android.widget.Toast
import android.app.AlertDialog

class PendingFragment : Fragment(R.layout.fragment_pending) {
    
    private val viewModel: ViolationViewModel by viewModels { 
        ViolationViewModelFactory(requireActivity().application) 
    }

    private var isSelectionMode = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PendingFragment", "onViewCreated called")
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        
        val btnSelectMode = view.findViewById<Button>(R.id.btnSelectMode)
        val btnSelectAll = view.findViewById<Button>(R.id.btnSelectAll)
        val bottomActionBar = view.findViewById<View>(R.id.bottomActionBar)
        val btnBatchDelete = view.findViewById<Button>(R.id.btnBatchDelete)
        val btnBatchProcess = view.findViewById<Button>(R.id.btnBatchProcess)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        val adapter = ViolationAdapter(
            onClick = { record ->
                if (isSelectionMode) {
                    // Ignore clicks in selection mode (adapter handles it, but just in case)
                } else {
                    if (record.videoPath.isEmpty()) {
                        // Show Image
                        if (record.imagePath.isNotEmpty()) {
                            val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                                putExtra("video_path", "")
                                putExtra("image_path", record.imagePath)
                                putExtra("record_id", record.id)
                                putExtra("status", record.status)
                                putExtra("plate", record.plateNumber)
                                putExtra("timestamp", record.timestamp)
                            }
                            startActivity(intent)
                        }
                    } else {
                        // Show Video
                        val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                            putExtra("video_path", record.videoPath)
                            putExtra("image_path", record.imagePath)
                            putExtra("record_id", record.id)
                            putExtra("status", record.status)
                            putExtra("plate", record.plateNumber)
                            putExtra("timestamp", record.timestamp)
                        }
                        startActivity(intent)
                    }
                }
            },
            onSelectionChanged = { count ->
                btnBatchDelete.text = "删除 ($count)"
                btnBatchProcess.text = "AI 检测 ($count)"
                btnBatchDelete.isEnabled = count > 0
                btnBatchProcess.isEnabled = count > 0
            },
            onModeChanged = { enabled ->
                isSelectionMode = enabled
                if (enabled) {
                    btnSelectMode.text = "取消"
                    btnSelectAll.visibility = View.VISIBLE
                    bottomActionBar.visibility = View.VISIBLE
                } else {
                    btnSelectMode.text = "选择"
                    btnSelectAll.visibility = View.GONE
                    bottomActionBar.visibility = View.GONE
                }
            }
        )
        recyclerView.adapter = adapter
        
        // Select Mode Toggle
        btnSelectMode.setOnClickListener {
            isSelectionMode = !isSelectionMode
            adapter.setSelectionMode(isSelectionMode)
            
            if (isSelectionMode) {
                btnSelectMode.text = "取消"
                btnSelectAll.visibility = View.VISIBLE
                bottomActionBar.visibility = View.VISIBLE
            } else {
                btnSelectMode.text = "选择"
                btnSelectAll.visibility = View.GONE
                bottomActionBar.visibility = View.GONE
            }
        }
        
        // Select All
        btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        // Batch Delete
        btnBatchDelete.setOnClickListener {
            val ids = adapter.getSelectedIds()
            if (ids.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("删除素材")
                    .setMessage("确认删除 ${ids.size} 个素材？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteViolations(ids)
                        exitSelectionMode(adapter, btnSelectMode, bottomActionBar)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        // Batch Process
        btnBatchProcess.setOnClickListener {
            val ids = adapter.getSelectedIds()
            if (ids.isNotEmpty()) {
                val allRecords = viewModel.pendingViolations.value ?: emptyList()
                val selectedRecords = allRecords.filter { it.id in ids }
                
                viewModel.processViolations(selectedRecords)
                Toast.makeText(context, "已提交 ${ids.size} 张图片进行 AI 检测", Toast.LENGTH_LONG).show()
                
                exitSelectionMode(adapter, btnSelectMode, bottomActionBar)
            }
        }

        viewModel.pendingViolations.observe(viewLifecycleOwner) { list ->
            // ... (rest of the code)
            Log.d("PendingFragment", "pendingViolations updated: size=${list.size}")
            list.forEach { record ->
                Log.d("PendingFragment", "  Record: id=${record.id}, plate=${record.plateNumber}, image=${record.imagePath}")
            }
            
            adapter.submitList(list)
            
            // Show/hide empty message
            if (list.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("PendingFragment", "onResume called")
    }

    private fun exitSelectionMode(adapter: ViolationAdapter, btnSelectMode: Button, bottomActionBar: View) {
        isSelectionMode = false
        adapter.setSelectionMode(false)
        btnSelectMode.text = "选择"
        bottomActionBar.visibility = View.GONE
    }
}
