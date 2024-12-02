package com.example.luluuu.ui.invoice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.R
import com.example.luluuu.adapter.StockAdapter
import com.example.luluuu.model.Customer
import com.example.luluuu.model.Stock
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.InvoiceItem
import com.example.luluuu.model.StockHistory
import com.example.luluuu.viewmodel.StockViewModel
import com.example.luluuu.viewmodel.InvoiceViewModel
import com.example.luluuu.viewmodel.CustomerViewModel
import com.example.luluuu.databinding.FragmentInvoiceBinding
import com.example.luluuu.databinding.DialogProductSelectionBinding
import com.example.luluuu.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.widget.AutoCompleteTextView

class InvoiceFragment : Fragment() {
    private var _binding: FragmentInvoiceBinding? = null
    private val binding get() = _binding!!
    private val stockViewModel: StockViewModel by viewModels()
    private val invoiceViewModel: InvoiceViewModel by viewModels()
    private val customerViewModel: CustomerViewModel by viewModels()
    private lateinit var stockAdapter: StockAdapter
    private var selectedCustomer: Customer? = null

    private var selectedItems = mutableListOf<InvoiceItem>()
    private var currentJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        
        setupCustomerAutocomplete()
        setupButtons()
        setupRecyclerView()
        setupDiscountListener()
        observeStocks()
        updateTotalAmount()

        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.invoice_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_last_invoice -> {
                        loadLastPrintedInvoice()
                        true
                    }
                    R.id.action_delete_last_invoice -> {
                        deleteLastPrintedInvoice()
                        true
                    }
                    R.id.action_view_history -> {
                        findNavController().navigate(R.id.action_invoice_to_history)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupCustomerAutocomplete() {
        viewLifecycleOwner.lifecycleScope.launch {
            customerViewModel.getAllCustomers().collect { customers ->
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    customers.map { it.name }
                )
                (binding.customerNameEditText as AutoCompleteTextView).setAdapter(adapter)
            }
        }

        binding.customerNameEditText.setOnItemClickListener { _, _, position, _ ->
            val selectedName = (binding.customerNameEditText as AutoCompleteTextView).adapter.getItem(position) as String
            viewLifecycleOwner.lifecycleScope.launch {
                customerViewModel.getCustomerByName(selectedName)?.let { customer ->
                    selectedCustomer = customer
                    binding.customerMobileEditText.setText(customer.phoneNumber)
                }
            }
        }

        binding.customerNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.toString()?.isEmpty() == true) {
                    selectedCustomer = null
                    binding.customerMobileEditText.text?.clear()
                }
            }
        })
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
            try {
                findNavController().navigate(R.id.action_invoice_to_history)
            } catch (e: Exception) {
                Log.e("InvoiceFragment", "Navigation failed: ${e.message}")
                Toast.makeText(context, "Failed to open invoice history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            items = selectedItems,
            onItemChanged = {
                updateTotalAmount()
            },
            onRemoveItem = { position ->
                removeItem(position)
            },
            onStockClick = { stock ->
                // Handle stock click if needed
            }
        )

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stockAdapter
        }

        // Observe stocks to update available quantities
        viewLifecycleOwner.lifecycleScope.launch {
            stockViewModel.stocks.collect { stocks ->
                stockAdapter.availableStocks = stocks
            }
        }
    }

    private fun setupDiscountListener() {
        binding.discountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalAmount()
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        val customerName = binding.customerNameEditText.text.toString().trim()
        val customerMobile = binding.customerMobileEditText.text.toString().trim()
        
        if (customerName.isEmpty()) {
            binding.customerNameEditText.error = "Please enter customer name"
            isValid = false
        } else {
            binding.customerNameEditText.error = null
        }
        
        if (customerMobile.isEmpty()) {
            binding.customerMobileEditText.error = "Please enter mobile number"
            isValid = false
        } else {
            binding.customerMobileEditText.error = null
        }

        if (selectedItems.isEmpty()) {
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
                    val freeQuantity = dialogBinding.freeItemsEditText.text.toString().toIntOrNull() ?: 0
                    val totalQuantity = quantity + freeQuantity

                    if (totalQuantity <= 0 || totalQuantity > stock.quantity) {
                        Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Add regular items if quantity > 0
                    if (quantity > 0) {
                        selectedItems.add(InvoiceItem(
                            stockId = stock.id,
                            productName = stock.name,
                            quantity = quantity,
                            price = stock.price,
                            total = stock.price * quantity,
                            isFree = false
                        ))
                    }

                    // Add free items if freeQuantity > 0
                    if (freeQuantity > 0) {
                        selectedItems.add(InvoiceItem(
                            stockId = stock.id,
                            productName = stock.name,
                            quantity = freeQuantity,
                            price = stock.price,
                            total = 0.0, // Free items have 0 total
                            isFree = true
                        ))
                    }

                    stockAdapter.notifyDataSetChanged()
                    updateTotalAmount()
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun generateAndPrintInvoice() {
        val mainActivity = requireActivity() as MainActivity
        val socket = mainActivity.getBluetoothSocket()
        
        if (socket == null || !socket.isConnected) {
            // Try to reconnect
            mainActivity.connectToPrinter()
            Toast.makeText(context, "Reconnecting to printer...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!validateInputs()) {
            return
        }

        val customerName = binding.customerNameEditText.text.toString().trim()
        val customerPhone = binding.customerMobileEditText.text.toString().trim()

        if (selectedItems.isEmpty()) {
            Toast.makeText(context, "Please select at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        currentJob?.cancel()
        currentJob = lifecycleScope.launch {
            try {
                // Get current stocks for validation
                val currentStocks = selectedItems.mapNotNull { item ->
                    stockViewModel.getStockById(item.stockId)
                }

                // Group items by stockId to combine regular and free quantities
                val itemsByStock = selectedItems.groupBy { it.stockId }

                // Validate total quantities (regular + free)
                for (stock in currentStocks) {
                    val items = itemsByStock[stock.id] ?: continue
                    val totalQuantity = items.sumOf { it.quantity }
                    if (totalQuantity > stock.quantity) {
                        throw IllegalStateException("Not enough stock for ${stock.name}")
                    }
                }

                try {
                    withContext(Dispatchers.IO) {
                        mainActivity.printText(buildInvoiceString())
                    }
                    
                    // Save invoice first to get the invoice number
                    val invoice = saveInvoice()
                    
                    // Update stocks and record history
                    for (stock in currentStocks) {
                        val items = itemsByStock[stock.id] ?: continue
                        val regularItem = items.find { !it.isFree }
                        val freeItem = items.find { it.isFree }
                        
                        val regularQuantity = regularItem?.quantity ?: 0
                        val freeQuantity = freeItem?.quantity ?: 0
                        val totalQuantity = regularQuantity + freeQuantity
                        
                        // Update stock quantity
                        val updatedStock = stock.copy(
                            quantity = stock.quantity - totalQuantity
                        )
                        stockViewModel.update(updatedStock)
                        
                        // Record stock history
                        val stockHistory = StockHistory(
                            stockId = stock.id,
                            date = Date(),
                            oldQuantity = stock.quantity,
                            newQuantity = updatedStock.quantity,
                            oldPrice = stock.price,
                            newPrice = stock.price,
                            action = "SALE",
                            invoiceNumber = invoice.invoiceNumber,
                            regularQuantity = regularQuantity,
                            freeQuantity = freeQuantity,
                            description = if (freeQuantity > 0) "Includes $freeQuantity free items" else ""
                        )
                        stockViewModel.addStockHistory(stockHistory)
                        
                        // Update Firestore
                        stockViewModel.updateFirestore(updatedStock, stockHistory)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Invoice printed successfully", Toast.LENGTH_SHORT).show()
                        navigateBack()
                    }
                } catch (e: IOException) {
                    Log.e("InvoiceFragment", "Printing failed: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Printing failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    mainActivity.connectToPrinter()
                }
            } catch (e: Exception) {
                Log.e("InvoiceFragment", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun saveInvoice(): Invoice {
        val returnAmount = binding.returnAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val returnDescription = binding.returnDescriptionEditText.text.toString().trim()
        val discount = binding.discountEditText.text.toString().toDoubleOrNull() ?: 0.0
        
        val currentDate = Date()
        val invoiceNumber = generateInvoiceNumber(currentDate)
        val subtotal = selectedItems.sumOf { it.quantity * it.price }
        val total = subtotal - discount
        
        // Create new invoice with customer info
        val invoice = Invoice(
            date = currentDate,
            invoiceNumber = invoiceNumber,
            customerId = selectedCustomer?.id,
            customerName = binding.customerNameEditText.text.toString().trim(),
            customerPhone = binding.customerMobileEditText.text.toString().trim(),
            items = selectedItems,
            total = total,
            discount = discount,
            returnAmount = returnAmount,
            returnDescription = returnDescription
        )
        
        invoiceViewModel.insert(invoice)
        return invoice
    }

    private fun generateInvoiceNumber(date: Date): Int {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val randomSuffix = (1000..9999).random()
        return dateFormat.format(date).toInt()
    }

    private fun buildInvoiceString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val customerName = binding.customerNameEditText.text.toString().trim()
        val customerPhone = binding.customerMobileEditText.text.toString().trim()
        val currentDate = Date()
        
        val subtotal = selectedItems.sumOf { it.quantity.toDouble() * it.price }
        val discount = binding.discountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val returnAmount = binding.returnAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val returnDescription = binding.returnDescriptionEditText.text.toString().trim()
        val total = subtotal - discount - returnAmount
        
        return buildString {
            appendLine("LULU ENTERPRISES")
            appendLine("-------------------")
            appendLine("Date: ${dateFormat.format(currentDate)}")
            appendLine("Invoice #: ${generateInvoiceNumber(currentDate)}")
            appendLine("")
            appendLine("Customer: $customerName")
            appendLine("Phone: $customerPhone")
            appendLine("")
            appendLine("Items:")
            appendLine("-------------------")
            
            // Regular items
            val regularItems = selectedItems.filterNot { it.isFree }
            regularItems.forEach { item ->
                appendLine("${item.productName}")
                appendLine("${item.quantity} x Rs. %.2f = Rs. %.2f".format(item.price, item.quantity * item.price))
            }
            
            // Free items
            val freeItems = selectedItems.filter { it.isFree }
            if (freeItems.isNotEmpty()) {
                appendLine("")
                appendLine("Free Items:")
                appendLine("-------------------")
                freeItems.forEach { item ->
                    appendLine("${item.productName}")
                    appendLine("Quantity: ${item.quantity} (Free)")
                }
            }
            
            appendLine("-------------------")
            appendLine("Subtotal: Rs. %.2f".format(subtotal))
            
            if (discount > 0) {
                appendLine("Discount: Rs. %.2f".format(discount))
            }
            
            if (returnAmount > 0) {
                appendLine("Return Amount: Rs. %.2f".format(returnAmount))
                if (returnDescription.isNotEmpty()) {
                    appendLine("Return Description: $returnDescription")
                }
            }
            
            appendLine("Final Total: Rs. %.2f".format(total))
            appendLine("")
            appendLine("Thank you for your business!")
        }
    }

    private fun updateTotalAmount() {
        val subtotal = selectedItems.sumOf { it.quantity.toDouble() * it.price }
        val discount = binding.discountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val returnAmount = binding.returnAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val total = subtotal - discount - returnAmount
        
        binding.totalAmountTextView.text = String.format("Subtotal: Rs. %.2f", subtotal)
        
        val deductions = mutableListOf<String>()
        if (discount > 0) deductions.add("Rs. %.2f discount".format(discount))
        if (returnAmount > 0) deductions.add("Rs. %.2f return".format(returnAmount))
        
        binding.finalAmountTextView.text = if (deductions.isNotEmpty()) {
            String.format("Total (after %s): Rs. %.2f", deductions.joinToString(" and "), total)
        } else {
            String.format("Total: Rs. %.2f", total)
        }
    }

    private fun observeStocks() {
        viewLifecycleOwner.lifecycleScope.launch {
            stockViewModel.stocks.collectLatest { stocks ->
                updateAvailableStocks(stocks)
            }
        }
    }

    private fun updateAvailableStocks(stocks: List<Stock>) {
        stockAdapter.availableStocks = stocks
    }

    private fun loadLastPrintedInvoice() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val lastInvoice = invoiceViewModel.getLastInvoice()
                lastInvoice?.let { invoice ->
                    // Clear existing items
                    selectedItems.clear()
                    selectedItems.addAll(invoice.items)
                    
                    // Load invoice data
                    binding.customerNameEditText.setText(invoice.customerName)
                    binding.customerMobileEditText.setText(invoice.customerPhone)
                    binding.returnAmountEditText.setText(invoice.returnAmount.toString())
                    binding.returnDescriptionEditText.setText(invoice.returnDescription)
                    
                    // Update UI
                    stockAdapter.notifyDataSetChanged()
                    updateTotalAmount()
                } ?: run {
                    Toast.makeText(context, "No recent invoice found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading last invoice: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteLastPrintedInvoice() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val lastInvoice = invoiceViewModel.getLastInvoice()
                if (lastInvoice != null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_last_invoice)
                        .setMessage("Are you sure you want to delete the last printed invoice?")
                        .setPositiveButton("Delete") { _, _ ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                invoiceViewModel.delete(lastInvoice)
                                Toast.makeText(context, "Last invoice deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    Toast.makeText(context, "No recent invoice found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting last invoice: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateBack() {
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentJob?.cancel()
        _binding = null
    }

    private fun removeItem(position: Int) {
        selectedItems.removeAt(position)
        stockAdapter.notifyItemRemoved(position)
        updateTotalAmount()
    }

    private companion object {
        private const val TAG = "InvoiceFragment"
    }
}