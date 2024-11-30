package com.example.luluuu.ui.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.R
import com.example.luluuu.databinding.FragmentInvoiceBinding
import com.example.luluuu.MainActivity
import com.example.luluuu.model.Stock
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.InvoiceItem
import com.example.luluuu.viewmodel.StockViewModel
import com.example.luluuu.viewmodel.InvoiceViewModel
import com.example.luluuu.databinding.DialogProductSelectionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvoiceFragment : Fragment() {
    private var _binding: FragmentInvoiceBinding? = null
    private val binding get() = _binding!!
    private val stockViewModel: StockViewModel by viewModels()
    private val invoiceViewModel: InvoiceViewModel by viewModels()
    
    private val products = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter

    private var currentJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeStocks()
        updateTotalAmount()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            products,
            onProductChanged = { 
                updateTotalAmount() 
            },
            onRemoveProduct = { position ->
                products.removeAt(position)
                productAdapter.notifyItemRemoved(position)
                updateTotalAmount()
            }
        )

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }

        // Observe stocks to update available quantities
        viewLifecycleOwner.lifecycleScope.launch {
            stockViewModel.stocks.collect { stocks ->
                productAdapter.availableProducts = stocks
            }
        }
    }

    private fun setupButtons() {
        binding.addProductFab.setOnClickListener {
            showProductSelectionDialog()
        }

        binding.generateInvoiceFab.setOnClickListener {
            if (validateInputs()) {
                generateAndPrintInvoice()
            }
        }

        binding.viewHistoryFab.setOnClickListener {
            findNavController().navigate(R.id.action_invoice_to_history)
        }
    }

    private fun validateInputs(): Boolean {
        if (products.isEmpty()) {
            Toast.makeText(context, "Please add at least one product", Toast.LENGTH_SHORT).show()
            return false
        }

        for (product in products) {
            if (product.name.isBlank() || product.price <= 0) {
                Toast.makeText(context, "Please fill all product details correctly", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun showProductSelectionDialog() {
        val dialogBinding = DialogProductSelectionBinding.inflate(layoutInflater)
        var selectedStock: Stock? = null

        viewLifecycleOwner.lifecycleScope.launch {
            stockViewModel.stocks.collect { stocks ->
                // Only update if we don't have a selection yet
                if (selectedStock == null) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        stocks.map { it.name }
                    )
                    dialogBinding.productSpinner.setAdapter(adapter)
                }

                dialogBinding.productSpinner.setOnItemClickListener { _, _, position, _ ->
                    selectedStock = stocks[position]
                    dialogBinding.quantityEditText.hint = "Available: ${selectedStock?.quantity ?: 0}"
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_product))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                selectedStock?.let { stock ->
                    val quantity = dialogBinding.quantityEditText.text.toString().toIntOrNull() ?: 1

                    if (quantity <= 0 || quantity > stock.quantity) {
                        Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    products.add(Product(
                        name = stock.name,
                        price = stock.price,
                        quantity = quantity
                    ))
                    productAdapter.notifyItemInserted(products.size - 1)
                    updateTotalAmount()
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun generateAndPrintInvoice() {
        val mainActivity = activity as? MainActivity
        val bluetoothSocket = MainActivity.bluetoothSocket
        
        if (bluetoothSocket == null || !bluetoothSocket.isConnected) {
            // Try to reconnect
            mainActivity?.connectToPrinter()
            Toast.makeText(context, "Reconnecting to printer...", Toast.LENGTH_SHORT).show()
            return
        }

        currentJob?.cancel()
        
        currentJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentStocks = withContext(Dispatchers.IO) {
                    stockViewModel.stocks.first()
                }

                // Validate stock quantities
                val invalidProducts = products.filter { product ->
                    val stock = currentStocks.find { it.name == product.name }
                    stock == null || stock.quantity < product.quantity
                }

                if (invalidProducts.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "Some products are out of stock or have insufficient quantity",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                try {
                    withContext(Dispatchers.IO) {
                        mainActivity?.printText(buildInvoiceString())
                    }
                    
                    currentStocks.forEach { stock ->
                        products.find { it.name == stock.name }?.let { product ->
                            val updatedStock = stock.copy(
                                quantity = stock.quantity - product.quantity
                            )
                            stockViewModel.update(updatedStock)
                        }
                    }
                    
                    saveInvoiceHistory()
                    
                    withContext(Dispatchers.Main) {
                        products.clear()
                        productAdapter.notifyDataSetChanged()
                        updateTotalAmount()
                        Toast.makeText(context, "Invoice printed successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Log.e("InvoiceFragment", "Printing failed: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Printing failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    mainActivity?.connectToPrinter()
                }
            } catch (e: Exception) {
                Log.e("InvoiceFragment", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveInvoiceHistory() {
        val invoiceItems = products.map { product ->
            InvoiceItem(
                productName = product.name,
                quantity = product.quantity,
                price = product.price,
                total = product.total()
            )
        }
        
        val invoice = Invoice(
            date = Date(),
            items = invoiceItems,
            total = products.sumOf { it.total() }
        )
        
        invoiceViewModel.insert(invoice)
    }

    private fun buildInvoiceString(): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("si", "LK"))
        return buildString {
            appendLine("================================")
            appendLine("           LULU ENTERPRISES")
            appendLine("================================")
            appendLine("           ${getString(R.string.invoice_header)}")
            appendLine("================================")
            appendLine()
            appendLine(getString(R.string.item_details))
            appendLine("--------------------------------")
            
            products.forEach { product ->
                val formattedPrice = currencyFormat.format(product.price)
                    .replace("LKR", "Rs.")
                val formattedTotal = currencyFormat.format(product.total())
                    .replace("LKR", "Rs.")
                appendLine("${product.name}")
                appendLine("${product.quantity} x $formattedPrice")
                appendLine("Total: $formattedTotal")
                appendLine("--------------------------------")
            }
            
            val grandTotal = currencyFormat.format(products.sumOf { it.total() })
                .replace("LKR", "Rs.")
            appendLine("Grand Total: $grandTotal")
            appendLine()
            appendLine(getString(R.string.thank_you))
            appendLine()
            appendLine()
        }
    }

    private fun updateTotalAmount() {
        val total = products.sumOf { it.total() }
        val formattedTotal = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
            .format(total)
        binding.totalAmountTextView.text = getString(R.string.total_amount, formattedTotal)
    }

    private fun observeStocks() {
        viewLifecycleOwner.lifecycleScope.launch {
            stockViewModel.stocks.collectLatest { stocks ->
                updateAvailableProducts(stocks)
            }
        }
    }

    private fun updateAvailableProducts(stocks: List<Stock>) {
        productAdapter.availableProducts = stocks
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentJob?.cancel()
        _binding = null
    }

    private companion object {
        private const val TAG = "InvoiceFragment"
    }
} 