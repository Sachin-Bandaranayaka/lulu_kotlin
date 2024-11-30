package com.example.luluuu.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.adapter.StockHistoryAdapter
import com.example.luluuu.databinding.DialogStockHistoryBinding
import com.example.luluuu.model.Stock
import com.example.luluuu.viewmodel.StockHistoryViewModel
import kotlinx.coroutines.launch

class StockHistoryDialog(private val stock: Stock) : DialogFragment() {
    private var _binding: DialogStockHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StockHistoryViewModel by viewModels()
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
        setupRecyclerView()
        observeHistory()
    }

    private fun setupRecyclerView() {
        adapter = StockHistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StockHistoryDialog.adapter
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getStockHistory(stock.id).collect { history ->
                adapter.submitList(history)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 