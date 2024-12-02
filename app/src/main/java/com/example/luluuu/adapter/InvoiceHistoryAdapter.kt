package com.example.luluuu.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemInvoiceHistoryBinding
import com.example.luluuu.model.Invoice
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class InvoiceHistoryAdapter(
    private val onInvoiceClick: (Invoice) -> Unit,
    private val onDeleteClick: (Invoice) -> Unit
) : ListAdapter<Invoice, InvoiceHistoryAdapter.InvoiceViewHolder>(InvoiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val binding = ItemInvoiceHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InvoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvoiceViewHolder(
        private val binding: ItemInvoiceHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(invoice: Invoice) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val numberFormat = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance("IDR")
            }

            binding.apply {
                tvInvoiceNumber.text = invoice.invoiceNumber.toString()
                tvCustomerName.text = invoice.customerName
                tvDate.text = dateFormat.format(invoice.date)
                tvTotal.text = numberFormat.format(invoice.total.toString())

                root.setOnClickListener { onInvoiceClick(invoice) }
                binding.btnDelete.setOnClickListener { onDeleteClick(invoice) }
            }
        }
    }

    private class InvoiceDiffCallback : DiffUtil.ItemCallback<Invoice>() {
        override fun areItemsTheSame(oldItem: Invoice, newItem: Invoice): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Invoice, newItem: Invoice): Boolean {
            return oldItem == newItem
        }
    }
}
