package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.widget.Button
import android.widget.Toast
import android.app.AlertDialog

import android.util.Log

class CasesFragment : Fragment(R.layout.fragment_cases) {
    
    private val viewModel: ViolationViewModel by viewModels { 
        ViolationViewModelFactory(requireActivity().application) 
    }

    private var isSelectionMode = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        
        val btnSelectMode = view.findViewById<Button>(R.id.btnSelectMode)
        val btnSelectAll = view.findViewById<Button>(R.id.btnSelectAll)
        val bottomActionBar = view.findViewById<View>(R.id.bottomActionBar)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val btnClipVideo = view.findViewById<Button>(R.id.btnClipVideo)

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        
        val adapter = CasesAdapter(
            onClick = { caseInfo ->
                if (isSelectionMode) {
                    // Ignore clicks if mode is managed by adapter, but adapter handles clicks too.
                    // Actually, if isSelectionMode is true in Fragment, we might want to do nothing or let adapter handle.
                    // Adapter handles toggleSelection if isSelectionMode is true.
                } else {
                    Log.d("CasesFragment", "Clicked case: ${caseInfo.plateNumber}")
                    val intent = Intent(requireContext(), CaseDetailActivity::class.java).apply {
                        putExtra("plate_number", caseInfo.plateNumber)
                    }
                    startActivity(intent)
                }
            },
            onSelectionChanged = { count ->
                btnDelete.text = "Delete ($count)"
                btnClipVideo.text = "Clip Video ($count)"
                btnDelete.isEnabled = count > 0
                btnClipVideo.isEnabled = count > 0
            },
            onModeChanged = { enabled ->
                isSelectionMode = enabled
                if (enabled) {
                    btnSelectMode.text = "Cancel"
                    btnSelectAll.visibility = View.VISIBLE
                    bottomActionBar.visibility = View.VISIBLE
                } else {
                    btnSelectMode.text = "Select"
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
            
            // UI update handled by onModeChanged callback from adapter
        }
        
        // Select All
        btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }
        
        viewModel.allCases.observe(viewLifecycleOwner) { list ->
            Log.d("CasesFragment", "Cases list updated. Size: ${list.size}")
            list.forEach { Log.d("CasesFragment", "Case: ${it.plateNumber}") }
            adapter.submitList(list)
        }

        // Select Mode Toggle
        btnSelectMode.setOnClickListener {
            isSelectionMode = !isSelectionMode
            adapter.setSelectionMode(isSelectionMode)
            
            if (isSelectionMode) {
                btnSelectMode.text = "Cancel"
                bottomActionBar.visibility = View.VISIBLE
            } else {
                btnSelectMode.text = "Select"
                bottomActionBar.visibility = View.GONE
            }
        }

        // Batch Delete
        btnDelete.setOnClickListener {
            val plates = adapter.getSelectedPlates()
            if (plates.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete")
                    .setMessage("Delete ${plates.size} cases? This will remove all records in them.")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteCases(plates)
                        exitSelectionMode(adapter, btnSelectMode, bottomActionBar)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        // Clip Video
        btnClipVideo.setOnClickListener {
            val plates = adapter.getSelectedPlates()
            if (plates.isNotEmpty()) {
                viewModel.clipVideoForCase(plates)
                Toast.makeText(context, "Clipping video for ${plates.size} cases in background...", Toast.LENGTH_SHORT).show()
                exitSelectionMode(adapter, btnSelectMode, bottomActionBar)
            }
        }
    }

    private fun exitSelectionMode(adapter: CasesAdapter, btnSelectMode: Button, bottomActionBar: View) {
        isSelectionMode = false
        adapter.setSelectionMode(false)
        btnSelectMode.text = "Select"
        bottomActionBar.visibility = View.GONE
    }
}
