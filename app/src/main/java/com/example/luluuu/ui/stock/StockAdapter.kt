package com.example.luluuu.ui.stock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemStockBinding
import com.example.luluuu.model.Stock
import java.text.NumberFormat
import java.util.Locale

class StockAdapter(
    private val onStockClick: (Stock) -> Unit,
    private val onStockLongClick: (Stock) -> Boolean,
    private val onEditClick: (Stock) -> Unit,
    private val onDeleteClick: (Stock) -> Unit
) : ListAdapter<Stock, StockAdapter.StockViewHolder>(StockDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StockViewHolder(private val binding: ItemStockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: Stock) {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK")).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }

            binding.apply {
                nameTextView.text = stock.name
                descriptionTextView.text = stock.description
                quantityTextView.text = "Quantity: ${stock.quantity}"
                priceTextView.text = "Price: ${numberFormat.format(stock.price)}"
                categoryTextView.text = "Category: ${stock.category}"

                root.setOnClickListener { onStockClick(stock) }
                root.setOnLongClickListener { onStockLongClick(stock) }
                editButton.setOnClickListener { onEditClick(stock) }
                deleteButton.setOnClickListener { onDeleteClick(stock) }
            }
        }
    }

    private class StockDiffCallback : DiffUtil.ItemCallback<Stock>() {
        override fun areItemsTheSame(oldItem: Stock, newItem: Stock): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Stock, newItem: Stock): Boolean {
            return oldItem == newItem
        }
    }
}
