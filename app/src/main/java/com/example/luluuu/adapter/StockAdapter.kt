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
    private val onDeleteClick: (Stock) -> Unit,
    private val onHistoryClick: (Stock) -> Unit
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

    inner class StockViewHolder(
        private val binding: ItemStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

        init {
            binding.apply {
                editButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onEditClick(getItem(position))
                    }
                }

                deleteButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onDeleteClick(getItem(position))
                    }
                }

                historyButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onHistoryClick(getItem(position))
                    }
                }
            }
        }

        fun bind(stock: Stock) {
            binding.apply {
                stockNameTextView.text = stock.name
                stockPriceTextView.text = currencyFormat.format(stock.price)
                    .replace("LKR", "Rs.")
                stockQuantityTextView.text = "Quantity: ${stock.quantity}"
                stockDescriptionTextView.text = stock.description

                // Change background color if stock is low
                root.setBackgroundResource(
                    if (stock.quantity <= 5) {
                        com.google.android.material.R.color.design_default_color_error
                    } else {
                        android.R.color.transparent
                    }
                )
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