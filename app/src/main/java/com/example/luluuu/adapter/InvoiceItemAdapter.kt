package com.example.luluuu.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.R
import com.example.luluuu.databinding.ItemInvoiceItemBinding
import com.example.luluuu.model.InvoiceItem
import java.text.NumberFormat

class InvoiceItemAdapter : ListAdapter<InvoiceItem, InvoiceItemAdapter.ViewHolder>(DiffCallback()) {
    
    inner class ViewHolder(private val binding: ItemInvoiceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InvoiceItem) {
            binding.apply {
                productNameTextView.text = item.productName
                quantityTextView.text = "Quantity: ${item.quantity}"
                
                // Update the amount display
                val amount = if (item.isFree) {
                    "FREE"
                } else {
                    NumberFormat.getCurrencyInstance().format(item.quantity * item.price)
                }
                amountTextView.text = amount
                
                // Style free items differently
                amountTextView.setTextColor(
                    if (item.isFree) {
                        ContextCompat.getColor(itemView.context, R.color.free_item_color)
                    } else {
                        ContextCompat.getColor(itemView.context, R.color.default_text_color)
                    }
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInvoiceItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<InvoiceItem>() {
        override fun areItemsTheSame(oldItem: InvoiceItem, newItem: InvoiceItem): Boolean {
            return oldItem.stockId == newItem.stockId
        }

        override fun areContentsTheSame(oldItem: InvoiceItem, newItem: InvoiceItem): Boolean {
            return oldItem == newItem
        }
    }
} 