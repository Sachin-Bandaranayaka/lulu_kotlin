package com.example.luluuu.ui.invoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemInvoiceHistoryBinding
import com.example.luluuu.model.Invoice
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceHistoryAdapter(
    private val onInvoiceClick: (Invoice) -> Unit
) : ListAdapter<Invoice, InvoiceHistoryAdapter.ViewHolder>(InvoiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemInvoiceHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemInvoiceHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("si", "LK"))

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onInvoiceClick(getItem(position))
                }
            }
        }

        fun bind(invoice: Invoice) {
            binding.apply {
                dateTextView.text = dateFormat.format(invoice.date)
                itemCountTextView.text = "${invoice.items.size} items"
                totalTextView.text = currencyFormat.format(invoice.total)
                    .replace("LKR", "Rs.")
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