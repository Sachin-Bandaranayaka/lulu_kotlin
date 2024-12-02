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

class StockListAdapter(
    private val onStockClick: (Stock) -> Unit
) : ListAdapter<Stock, StockListAdapter.StockViewHolder>(StockDiffCallback()) {

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
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStockClick(getItem(position))
                }
            }
        }

        fun bind(stock: Stock) {
            binding.apply {
                nameTextView.text = stock.name
                priceTextView.text = currencyFormat.format(stock.price)
                    .replace("LKR", "Rs.")
                quantityTextView.text = stock.quantity.toString()
                descriptionTextView.text = stock.description
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