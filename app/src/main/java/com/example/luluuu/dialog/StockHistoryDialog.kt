package com.example.luluuu.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.R
import com.example.luluuu.adapter.StockHistoryAdapter
import com.example.luluuu.viewmodel.StockHistoryViewModel
import android.app.Application
import androidx.recyclerview.widget.DividerItemDecoration

class StockHistoryDialog(
    context: Context,
    private val stockId: Long,
    private val stockName: String
) : Dialog(context) {

    private val TAG = "StockHistoryDialog"
    private lateinit var viewModel: StockHistoryViewModel
    private lateinit var adapter: StockHistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var titleView: TextView
    private lateinit var progressBar: ProgressBar
    private val fragmentActivity: FragmentActivity? = context as? FragmentActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_stock_history, null)
        setContentView(view)

        // Initialize views
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewStockHistory).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        emptyView = view.findViewById(R.id.textViewNoHistory)
        titleView = view.findViewById(R.id.titleTextView)
        progressBar = view.findViewById(R.id.progressBar)

        // Set dialog title
        titleView.text = context.getString(R.string.stock_history_title, stockName)

        // Show loading state
        showLoading(true)

        // Set up RecyclerView
        adapter = StockHistoryAdapter()
        recyclerView.adapter = adapter

        Log.d(TAG, "Dialog created for stock ID: $stockId")

        // Set up ViewModel
        fragmentActivity?.let { activity ->
            val factory = object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return StockHistoryViewModel(activity.application) as T
                }
            }
            
            try {
                viewModel = ViewModelProvider(activity, factory)[StockHistoryViewModel::class.java]

                // Observe stock history
                viewModel.stockHistory.observe(activity) { historyList ->
                    showLoading(false)
                    Log.d(TAG, "Received ${historyList.size} history entries in dialog")
                    if (historyList.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = context.getString(R.string.no_history_available)
                        Log.d(TAG, "No history entries, showing empty view")
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                        adapter.submitList(historyList)
                        Log.d(TAG, "Showing ${historyList.size} history entries in RecyclerView")
                    }
                }

                // Load history for this stock
                Log.d(TAG, "Loading history for stock ID: $stockId")
                viewModel.loadStockHistory(stockId)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up ViewModel", e)
                showLoading(false)
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.text = context.getString(R.string.error_loading_history)
            }
        } ?: run {
            Log.e(TAG, "Context is not a FragmentActivity")
            showLoading(false)
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyView.text = context.getString(R.string.error_loading_history)
        }

        // Set up dialog properties
        window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        if (::viewModel.isInitialized) {
            viewModel.cleanup()
        }
    }
}
