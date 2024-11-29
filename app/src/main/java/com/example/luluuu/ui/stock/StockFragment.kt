package com.example.luluuu.ui.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.R
import com.example.luluuu.adapter.StockAdapter
import com.example.luluuu.databinding.FragmentStockBinding
import com.example.luluuu.databinding.DialogStockEditBinding
import com.example.luluuu.model.Stock
import com.example.luluuu.viewmodel.StockViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class StockFragment : Fragment() {
    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StockViewModel by viewModels()
    private lateinit var stockAdapter: StockAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeStocks()
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onEditClick = { stock -> showEditDialog(stock) },
            onDeleteClick = { stock -> showDeleteConfirmation(stock) }
        )

        binding.stockRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stockAdapter
        }
    }

    private fun showEditDialog(stock: Stock? = null) {
        val binding = DialogStockEditBinding.inflate(layoutInflater)
        val isNewStock = stock == null
        
        if (!isNewStock) {
            binding.apply {
                nameEditText.setText(stock?.name)
                priceEditText.setText(stock?.price.toString())
                quantityEditText.setText(stock?.quantity.toString())
                descriptionEditText.setText(stock?.description)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isNewStock) getString(R.string.add_stock) else getString(R.string.edit_stock))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = binding.nameEditText.text.toString()
                val price = binding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0
                val quantity = binding.quantityEditText.text.toString().toIntOrNull() ?: 0
                val description = binding.descriptionEditText.text.toString()

                if (name.isBlank()) {
                    Toast.makeText(context, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isNewStock) {
                    viewModel.insert(Stock(name = name, price = price, quantity = quantity, description = description))
                } else {
                    stock?.apply {
                        this.name = name
                        this.price = price
                        this.quantity = quantity
                        this.description = description
                        viewModel.update(this)
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
                viewModel.delete(stock)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupFab() {
        binding.addStockFab.setOnClickListener {
            showEditDialog()
        }
    }

    private fun observeStocks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allStocks.collect { stocks ->
                stockAdapter.submitList(stocks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 