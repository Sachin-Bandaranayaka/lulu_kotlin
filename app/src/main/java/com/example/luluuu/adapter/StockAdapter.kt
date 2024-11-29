package com.example.luluuu.adapter

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
    private val onEditClick: (Stock) -> Unit,
    private val onDeleteClick: (Stock) -> Unit
) : ListAdapter<Stock, StockAdapter.StockViewHolder>(StockDiffCallback()) {

    inner class StockViewHolder(
        private val binding: ItemStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: Stock) {
            with(binding) {
                // Set the stock name
                stockNameTextView.text = stock.name

                // Format and set the price
                val formattedPrice = String.format("Rs. %.2f", stock.price)
                stockPriceTextView.text = "Price: $formattedPrice"

                // Set the quantity
                stockQuantityTextView.text = "In Stock: ${stock.quantity}"

                // Set click listeners for edit and delete buttons
                editButton.setOnClickListener { onEditClick(stock) }
                deleteButton.setOnClickListener { onDeleteClick(stock) }

                // Set description if available
                if (stock.description.isNotBlank()) {
                    stockNameTextView.text = "${stock.name} - ${stock.description}"
                }

                // Change text color if stock is low (less than 5 items)
                if (stock.quantity < 5) {
                    stockQuantityTextView.setTextColor(
                        root.context.getColor(android.R.color.holo_red_dark)
                    )
                } else {
                    stockQuantityTextView.setTextColor(
                        root.context.getColor(android.R.color.black)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        return StockViewHolder(
            ItemStockBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
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