package com.example.luluuu.ui.invoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemInvoiceHistoryBinding
import com.example.luluuu.model.Invoice
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceHistoryAdapter(
    private val onInvoiceClick: (Invoice) -> Unit,
    private val onDeleteClick: (Invoice) -> Unit
) : ListAdapter<Invoice, InvoiceHistoryAdapter.ViewHolder>(InvoiceDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

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

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onInvoiceClick(getItem(position))
                }
            }

            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(invoice: Invoice) {
            binding.apply {
                tvInvoiceNumber.text = invoice.invoiceNumber.toString()
                tvCustomerName.text = invoice.customerName
                tvDate.text = dateFormat.format(invoice.date)
                tvTotal.text = String.format("Rs. %.2f", invoice.total)
            }
        }
    }

    class InvoiceDiffCallback : DiffUtil.ItemCallback<Invoice>() {
        override fun areItemsTheSame(oldItem: Invoice, newItem: Invoice): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Invoice, newItem: Invoice): Boolean {
            return oldItem == newItem
        }
    }
}