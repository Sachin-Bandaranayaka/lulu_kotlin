package com.example.luluuu.ui.invoice

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.R
import com.example.luluuu.MainActivity
import com.example.luluuu.databinding.FragmentInvoiceHistoryBinding
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.InvoiceItem
import com.example.luluuu.viewmodel.InvoiceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceHistoryFragment : Fragment() {
    private var _binding: FragmentInvoiceHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: InvoiceHistoryAdapter
    private val viewModel: InvoiceViewModel by viewModels()
    private var currentCustomerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvoiceHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("InvoiceHistory", "Fragment created")
        
        setupRecyclerView()
        setupCustomerSearch()
        setupFilterButton()
        observeInvoices()
    }

    private fun setupCustomerSearch() {
        binding.customerSearchEditText.setOnClickListener {
            showCustomerSearchDialog()
        }
    }

    private fun showCustomerSearchDialog() {
        CustomerSearchDialog { customer ->
            currentCustomerId = customer.id
            binding.customerSearchEditText.setText(customer.name)
            observeInvoices()
        }.show(childFragmentManager, "CustomerSearchDialog")
    }

    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val filterDialog = InvoiceFilterDialog.newInstance()
        filterDialog.setOnFilterAppliedListener { customer, fromDate, toDate ->
            viewModel.applyFilter(customer, fromDate, toDate)
        }
        filterDialog.show(parentFragmentManager, "InvoiceFilterDialog")
    }

    private fun observeInvoices() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val invoicesFlow = if (currentCustomerId != null) {
                        viewModel.getInvoicesForCustomer(currentCustomerId!!)
                    } else {
                        viewModel.allInvoices
                    }

                    invoicesFlow.collectLatest { invoices ->
                        adapter.submitList(invoices)
                        updateEmptyState(invoices.isEmpty())
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("InvoiceHistory", "Error loading invoices", e)
                    Toast.makeText(context, "Failed to load invoices", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.invoiceRecyclerView.visibility = View.GONE
            showEmptyMessage()
        } else {
            binding.invoiceRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmptyMessage() {
        val message = if (currentCustomerId != null) {
            "No invoices found for this customer"
        } else {
            "No invoices found"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        adapter = InvoiceHistoryAdapter(
            onInvoiceClick = { invoice ->
                printInvoice(invoice)
            },
            onDeleteClick = { invoice ->
                showDeleteConfirmationDialog(invoice)
            }
        )
        
        binding.invoiceRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@InvoiceHistoryFragment.adapter
        }
    }

    private fun showDeleteConfirmationDialog(invoice: Invoice) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_invoice)
            .setMessage("Are you sure you want to delete this invoice?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(invoice)
                Toast.makeText(context, "Invoice deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun printInvoice(invoice: Invoice) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val invoiceString = buildInvoiceString(invoice)
                (activity as? MainActivity)?.let { mainActivity ->
                    mainActivity.printText(invoiceString)
                    Toast.makeText(context, "Invoice reprinted successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("InvoiceHistory", "Error printing invoice", e)
                Toast.makeText(context, "Printing failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildInvoiceString(invoice: Invoice): String = buildString {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        appendLine("================================")
        appendLine("Date: ${dateFormat.format(invoice.date)}")
        appendLine()
        appendLine(getString(R.string.item_details))
        appendLine("--------------------------------")
        
        invoice.items.forEach { item ->
            val formattedPrice = String.format("Rs. %.2f", item.price)
            val formattedTotal = String.format("Rs. %.2f", item.total)
                
            appendLine(item.productName)
            appendLine("${item.quantity} x $formattedPrice")
            appendLine("Total: $formattedTotal")
            appendLine("--------------------------------")
        }
        
        val grandTotal = String.format("Rs. %.2f", invoice.total)
        appendLine("Grand Total: $grandTotal")
        appendLine()
        appendLine(getString(R.string.thank_you))
        appendLine()
        appendLine()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}