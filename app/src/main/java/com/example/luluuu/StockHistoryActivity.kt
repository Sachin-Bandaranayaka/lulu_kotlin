package com.example.luluuu

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.adapter.StockHistoryAdapter
import com.example.luluuu.databinding.ActivityStockHistoryBinding
import com.example.luluuu.viewmodel.StockHistoryViewModel

class StockHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStockHistoryBinding
    private val viewModel: StockHistoryViewModel by viewModels()
    private lateinit var adapter: StockHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stockId = intent.getLongExtra("STOCK_ID", -1)
        val stockName = intent.getStringExtra("STOCK_NAME") ?: ""

        supportActionBar?.apply {
            title = "$stockName History"
            setDisplayHomeAsUpEnabled(true)
        }

        setupRecyclerView()
        observeStockHistory(stockId)
    }

    private fun setupRecyclerView() {
        adapter = StockHistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@StockHistoryActivity)
            adapter = this@StockHistoryActivity.adapter
        }
    }

    private fun observeStockHistory(stockId: Long) {
        // Load stock history
        viewModel.loadStockHistory(stockId)

        // Observe stock history changes
        viewModel.stockHistory.observe(this) { historyList ->
            if (historyList.isEmpty()) {
                binding.textViewNoHistory?.visibility = android.view.View.VISIBLE
                binding.historyRecyclerView.visibility = android.view.View.GONE
            } else {
                binding.textViewNoHistory?.visibility = android.view.View.GONE
                binding.historyRecyclerView.visibility = android.view.View.VISIBLE
                adapter.submitList(historyList)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}