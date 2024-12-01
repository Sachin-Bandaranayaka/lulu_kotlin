package com.example.luluuu.ui.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.invoice_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_last_invoice -> {
                loadLastPrintedInvoice()
                true
            }
            R.id.action_delete_last_invoice -> {
                deleteLastPrintedInvoice()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        var isValid = true
        
        val customerName = binding.customerNameEditText.text.toString().trim()
        val customerMobile = binding.customerMobileEditText.text.toString().trim()
        
        if (customerName.isEmpty()) {
            binding.customerNameLayout.error = "Please enter customer name"
            isValid = false
        } else {
            binding.customerNameLayout.error = null
        }
        
        if (customerMobile.isEmpty()) {
            binding.customerMobileLayout.error = "Please enter mobile number"
            isValid = false
        } else {
            binding.customerMobileLayout.error = null
        }

        if (products.isEmpty()) {
            Toast.makeText(context, "Please add at least one product", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
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
        val customerName = binding.customerNameEditText.text.toString().trim()
        val customerMobile = binding.customerMobileEditText.text.toString().trim()
        
        if (customerName.isEmpty()) {
            binding.customerNameLayout.error = "Please enter customer name"
            return
        }
        
        if (customerMobile.isEmpty()) {
            binding.customerMobileLayout.error = "Please enter mobile number"
            return
        }

        val invoiceItems = products.map { product ->
            InvoiceItem(
                productName = product.name,
                quantity = product.quantity,
                price = product.price,
                total = product.total()
            )
        }
        
        val currentDate = Date()
        val invoiceNumber = generateInvoiceNumber(currentDate)
        
        val lastInvoice = invoiceViewModel.getLastPrintedInvoice()
        val invoice = if (lastInvoice != null && products == lastInvoice.items.map { 
            Product(it.productName, it.price, it.quantity) 
        }) {
            // This is an update to the last invoice
            lastInvoice.copy(
                customerName = customerName,
                customerMobile = customerMobile,
                items = invoiceItems,
                total = products.sumOf { it.total() }
            )
        } else {
            // This is a new invoice
            Invoice(
                date = currentDate,
                invoiceNumber = invoiceNumber,
                customerName = customerName,
                customerMobile = customerMobile,
                items = invoiceItems,
                total = products.sumOf { it.total() }
            )
        }
        
        if (lastInvoice != null && invoice.firebaseId == lastInvoice.firebaseId) {
            invoiceViewModel.update(invoice)
        } else {
            invoiceViewModel.insert(invoice)
        }
    }

    private fun generateInvoiceNumber(date: Date): String {
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val randomSuffix = (1000..9999).random()
        return "INV-${dateFormat.format(date)}-$randomSuffix"
    }

    private fun buildInvoiceString(): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("si", "LK"))
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val customerName = binding.customerNameEditText.text.toString().trim()
        val customerMobile = binding.customerMobileEditText.text.toString().trim()
        val currentDate = Date()
        val invoiceNumber = generateInvoiceNumber(currentDate)

        return buildString {
            appendLine("================================")
            appendLine("           LULU ENTERPRISES")
            appendLine("================================")
            appendLine("Invoice #: $invoiceNumber")
            appendLine("Date: ${dateFormat.format(currentDate)}")
            appendLine("--------------------------------")
            appendLine("Customer: $customerName")
            appendLine("Mobile: $customerMobile")
            appendLine("--------------------------------")
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

    private fun loadLastPrintedInvoice() {
        val lastInvoice = invoiceViewModel.getLastPrintedInvoice()
        if (lastInvoice != null) {
            // Clear current products
            products.clear()
            
            // Load invoice data
            binding.customerNameEditText.setText(lastInvoice.customerName)
            binding.customerMobileEditText.setText(lastInvoice.customerMobile)
            
            // Convert invoice items to products
            products.addAll(lastInvoice.items.map { item ->
                Product(
                    name = item.productName,
                    price = item.price,
                    quantity = item.quantity
                )
            })
            
            productAdapter.notifyDataSetChanged()
            updateTotalAmount()
        } else {
            Toast.makeText(context, "No recent invoice found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteLastPrintedInvoice() {
        val lastInvoice = invoiceViewModel.getLastPrintedInvoice()
        if (lastInvoice != null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_last_invoice)
                .setMessage("Are you sure you want to delete the last printed invoice?")
                .setPositiveButton("Delete") { _, _ ->
                    invoiceViewModel.delete(lastInvoice)
                    Toast.makeText(context, "Last invoice deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(context, "No recent invoice found", Toast.LENGTH_SHORT).show()
        }
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