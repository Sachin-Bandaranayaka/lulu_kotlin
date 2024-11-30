package com.example.luluuu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.adapter.StockAdapter
import com.example.luluuu.databinding.ActivityStockBinding
import com.example.luluuu.databinding.DialogStockEditBinding
import com.example.luluuu.model.Stock
import com.example.luluuu.viewmodel.StockViewModel
import kotlinx.coroutines.launch

class StockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStockBinding
    private val viewModel: StockViewModel by viewModels()
    private lateinit var stockAdapter: StockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFab()
        observeStocks()
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onEditClick = { stock -> showStockDialog(stock) },
            onDeleteClick = { stock ->
                showDeleteConfirmationDialog(stock)
            },
            onHistoryClick = { stock -> 
                // Handle history click here
                // For example:
                startHistoryActivity(stock)
            }
        )

        binding.stockRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@StockActivity)
            adapter = stockAdapter
        }
    }

    private fun setupFab() {
        binding.addStockFab.setOnClickListener {
            showStockDialog(null)
        }
    }

    private fun observeStocks() {
        lifecycleScope.launch {
            viewModel.allStocks.collect { stocks ->
                stockAdapter.submitList(stocks)
            }
        }
    }

    private fun showStockDialog(stock: Stock?) {
        val dialogBinding = DialogStockEditBinding.inflate(LayoutInflater.from(this))
        
        // Pre-fill the dialog if editing
        stock?.let { existingStock ->
            dialogBinding.nameEditText.setText(existingStock.name)
            dialogBinding.priceEditText.setText(existingStock.price.toString())
            dialogBinding.quantityEditText.setText(existingStock.quantity.toString())
            dialogBinding.descriptionEditText.setText(existingStock.description)
        }

        AlertDialog.Builder(this)
            .setTitle(if (stock == null) getString(R.string.add_stock) else getString(R.string.edit_stock))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = dialogBinding.nameEditText.text.toString()
                val price = dialogBinding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0
                val quantity = dialogBinding.quantityEditText.text.toString().toIntOrNull() ?: 0
                val description = dialogBinding.descriptionEditText.text.toString()

                if (name.isBlank() || price <= 0 || quantity < 0) {
                    Toast.makeText(this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (stock == null) {
                    viewModel.insert(Stock(name = name, price = price, quantity = quantity, description = description))
                } else {
                    viewModel.update(stock.copy(name = name, price = price, quantity = quantity, description = description))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmationDialog(stock: Stock) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirmation))
            .setMessage(getString(R.string.delete_confirmation_message, stock.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.delete(stock)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun startHistoryActivity(stock: Stock) {
        val intent = Intent(this, StockHistoryActivity::class.java).apply {
            putExtra("STOCK_ID", stock.id)
            putExtra("STOCK_NAME", stock.name)
        }
        startActivity(intent)
    }
} 