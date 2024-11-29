package com.example.luluuu.ui.invoice

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
import com.example.luluuu.MainActivity
import com.example.luluuu.databinding.FragmentInvoiceHistoryBinding
import com.example.luluuu.model.Invoice
import com.example.luluuu.viewmodel.InvoiceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceHistoryFragment : Fragment() {
    private var _binding: FragmentInvoiceHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InvoiceViewModel by viewModels()
    private lateinit var adapter: InvoiceHistoryAdapter

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
        setupRecyclerView()
        observeInvoices()
    }

    private fun setupRecyclerView() {
        adapter = InvoiceHistoryAdapter(onInvoiceClick = { invoice ->
            printInvoice(invoice)
        })
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@InvoiceHistoryFragment.adapter
        }
    }

    private fun observeInvoices() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allInvoices.collectLatest { invoices ->
                adapter.submitList(invoices)
            }
        }
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
                Toast.makeText(context, "Printing failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildInvoiceString(invoice: Invoice): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("si", "LK"))
        
        return buildString {
            appendLine("================================")
            appendLine("           ${getString(R.string.invoice_header)}")
            appendLine("================================")
            appendLine("Date: ${dateFormat.format(invoice.date)}")
            appendLine()
            appendLine(getString(R.string.item_details))
            appendLine("--------------------------------")
            
            invoice.items.forEach { item ->
                val formattedPrice = currencyFormat.format(item.price)
                    .replace("LKR", "Rs.")
                val formattedTotal = currencyFormat.format(item.total)
                    .replace("LKR", "Rs.")
                    
                appendLine(item.productName)
                appendLine("${item.quantity} x $formattedPrice")
                appendLine("Total: $formattedTotal")
                appendLine("--------------------------------")
            }
            
            val grandTotal = currencyFormat.format(invoice.total)
                .replace("LKR", "Rs.")
            appendLine("Grand Total: $grandTotal")
            appendLine()
            appendLine(getString(R.string.thank_you))
            appendLine()
            appendLine()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 