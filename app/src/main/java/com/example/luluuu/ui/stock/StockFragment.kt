package com.example.luluuu.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.R
import com.example.luluuu.databinding.DialogStockEditBinding
import com.example.luluuu.databinding.FragmentStockBinding
import com.example.luluuu.model.ProductCategory
import com.example.luluuu.model.Stock
import com.example.luluuu.ui.stock.StockAdapter
import com.example.luluuu.viewmodel.StockViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StockFragment : Fragment() {
    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StockViewModel by viewModels()
    private lateinit var stockAdapter: StockAdapter
    private var searchJob: Job? = null
    private lateinit var searchView: androidx.appcompat.widget.SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        searchView = binding.root.findViewById(R.id.searchView)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupFab()
        observeStocks()
        observeLowStockItems()
        observeTotalValue()
        
        // Add menu items without using the Fragment Menu APIs
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_stock_sort, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.sort_name -> {
                        viewModel.setSortOrder(StockViewModel.SortOrder.NAME_ASC)
                        true
                    }
                    R.id.sort_price -> {
                        viewModel.setSortOrder(StockViewModel.SortOrder.PRICE_ASC)
                        true
                    }
                    R.id.sort_quantity -> {
                        viewModel.setSortOrder(StockViewModel.SortOrder.QUANTITY_ASC)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onStockClick = { stock ->
                showEditDialog(stock)
            },
            onStockLongClick = { stock ->
                showStockHistory(stock)
                true
            },
            onEditClick = { stock ->
                showEditDialog(stock)
            },
            onDeleteClick = { stock ->
                showDeleteConfirmation(stock)
            }
        )

        binding.stockRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stockAdapter
        }

        // Observe stocks
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stocks.collect { stocks ->
                stockAdapter.submitList(stocks)
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce typing
                    viewModel.setSearchQuery(newText ?: "")
                }
                return true
            }
        })
    }

    private fun setupFab() {
        binding.addStockFab.setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun observeStocks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stocks.collect { stocks ->
                stockAdapter.submitList(stocks)
            }
        }
    }

    private fun observeLowStockItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lowStockItems.collectLatest { lowStockItems ->
                if (lowStockItems.isNotEmpty()) {
                    showLowStockAlert(lowStockItems)
                }
            }
        }
    }

    private fun observeTotalValue() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalStockValue.collectLatest { total ->
                val formatted = NumberFormat
                    .getCurrencyInstance(Locale("en", "LK"))
                    .format(total)
                    .replace("LKR", "Rs.")
                binding.totalValueTextView.text = "Total Stock Value: $formatted"
            }
        }
    }

    private fun showEditDialog(stock: Stock? = null) {
        val binding = DialogStockEditBinding.inflate(layoutInflater)
        val isNewStock = stock == null

        // Setup category spinner
        val categories = ProductCategory.values().map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.categorySpinner.setAdapter(adapter)

        if (!isNewStock) {
            binding.apply {
                nameEditText.setText(stock?.name)
                priceEditText.setText(stock?.price.toString())
                quantityEditText.setText(stock?.quantity.toString())
                descriptionEditText.setText(stock?.description)
                categorySpinner.setText(stock?.category.toString(), false)
            }
        } else {
            binding.categorySpinner.setText(categories[0], false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isNewStock) getString(R.string.add_stock) else getString(R.string.edit_stock))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = binding.nameEditText.text.toString()
                val price = binding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0
                val quantity = binding.quantityEditText.text.toString().toIntOrNull() ?: 0
                val description = binding.descriptionEditText.text.toString()
                val category = ProductCategory.fromString(binding.categorySpinner.text.toString())

                if (name.isBlank()) {
                    Toast.makeText(context, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isNewStock) {
                    viewModel.insert(Stock(
                        name = name,
                        price = price,
                        quantity = quantity,
                        description = description,
                        category = category
                    ))
                } else {
                    stock?.let { existingStock ->
                        val updatedStock = existingStock.copy(
                            name = name,
                            price = price,
                            quantity = quantity,
                            description = description,
                            category = category,
                            firebaseId = existingStock.firebaseId  // Preserve Firebase ID
                        )
                        viewModel.update(updatedStock)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmation(stock: Stock) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_confirmation))
            .setMessage(getString(R.string.delete_confirmation_message, stock.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.delete(stock)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showLowStockAlert(lowStockItems: List<Stock>) {
        val message = buildString {
            appendLine(getString(R.string.low_stock_alert))
            lowStockItems.forEach { stock ->
                appendLine("• ${stock.name}: ${stock.quantity} left")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.low_stock_warning)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showStockHistory(stock: Stock) {
        StockHistoryDialog(stock.id).show(
            childFragmentManager,
            "StockHistoryDialog"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}