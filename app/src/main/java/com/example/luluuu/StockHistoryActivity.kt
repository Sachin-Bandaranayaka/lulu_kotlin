package com.example.luluuu

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.adapter.StockHistoryAdapter
import com.example.luluuu.databinding.ActivityStockHistoryBinding
import com.example.luluuu.viewmodel.StockHistoryViewModel
import kotlinx.coroutines.launch

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
        lifecycleScope.launch {
            viewModel.getStockHistory(stockId).collect { history ->
                adapter.submitList(history)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 