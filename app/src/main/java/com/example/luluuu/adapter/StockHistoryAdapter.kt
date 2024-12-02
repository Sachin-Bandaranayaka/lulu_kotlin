package com.example.luluuu.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemStockHistoryBinding
import com.example.luluuu.model.StockHistory
import java.text.SimpleDateFormat
import java.util.Locale

class StockHistoryAdapter : ListAdapter<StockHistory, StockHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = getItem(position)
        holder.bind(history)
    }

    inner class ViewHolder(private val binding: ItemStockHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(history: StockHistory) {
            binding.apply {
                textViewDate.text = dateFormat.format(history.date)
                textViewAction.text = history.action
                
                when (history.action) {
                    "CREATE" -> {
                        textViewQuantityChange.text = "Initial quantity: ${history.newQuantity}"
                        textViewPriceChange.text = "Initial price: $${String.format("%.2f", history.newPrice)}"
                    }
                    "UPDATE" -> {
                        val quantityDiff = history.newQuantity - history.oldQuantity
                        val quantitySign = if (quantityDiff >= 0) "+" else ""
                        textViewQuantityChange.text = "Quantity: ${history.oldQuantity} → ${history.newQuantity} ($quantitySign$quantityDiff)"

                        val priceDiff = history.newPrice - history.oldPrice
                        val priceSign = if (priceDiff >= 0) "+" else ""
                        textViewPriceChange.text = "Price: $${String.format("%.2f", history.oldPrice)} → $${String.format("%.2f", history.newPrice)} ($priceSign$${String.format("%.2f", priceDiff)})"
                    }
                    "DELETE" -> {
                        textViewQuantityChange.text = "Final quantity: ${history.oldQuantity}"
                        textViewPriceChange.text = "Final price: $${String.format("%.2f", history.oldPrice)}"
                    }
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StockHistory>() {
        override fun areItemsTheSame(oldItem: StockHistory, newItem: StockHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockHistory, newItem: StockHistory): Boolean {
            return oldItem == newItem
        }
    }
}