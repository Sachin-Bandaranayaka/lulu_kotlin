package com.example.luluuu.ui.stock

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.adapter.StockHistoryAdapter
import com.example.luluuu.databinding.DialogStockHistoryBinding
import com.example.luluuu.viewmodel.StockHistoryViewModel

class StockHistoryDialog(private val stockId: Long) : DialogFragment() {
    private var _binding: DialogStockHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StockHistoryViewModel
    private lateinit var adapter: StockHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStockHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("StockHistoryDialog", "Dialog created for stock ID: $stockId")

        setupRecyclerView()
        setupViewModel()
        observeViewModel()

        // Load stock history
        viewModel.loadStockHistory(stockId)
        
        // Set dialog title
        binding.titleTextView.text = "History: $stockId"
    }

    private fun setupRecyclerView() {
        adapter = StockHistoryAdapter()
        binding.recyclerViewStockHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StockHistoryDialog.adapter
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[StockHistoryViewModel::class.java]
    }

    private fun observeViewModel() {
        viewModel.stockHistory.observe(viewLifecycleOwner) { historyList ->
            Log.d("StockHistoryDialog", "Received ${historyList.size} history entries")
            adapter.submitList(historyList)
            
            // Show/hide empty state
            if (historyList.isEmpty()) {
                binding.textViewNoHistory.visibility = View.VISIBLE
                binding.recyclerViewStockHistory.visibility = View.GONE
            } else {
                binding.textViewNoHistory.visibility = View.GONE
                binding.recyclerViewStockHistory.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}