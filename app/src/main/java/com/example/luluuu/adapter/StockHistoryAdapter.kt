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

class StockHistoryAdapter : ListAdapter<StockHistory, StockHistoryAdapter.ViewHolder>(StockHistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStockHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun bind(history: StockHistory) {
            binding.apply {
                dateTextView.text = dateFormat.format(history.date)
                actionTextView.text = history.action
                quantityTextView.text = "Quantity: ${history.oldQuantity} → ${history.newQuantity}"
                priceTextView.text = "Price: ${history.oldPrice} → ${history.newPrice}"
            }
        }
    }

    private class StockHistoryDiffCallback : DiffUtil.ItemCallback<StockHistory>() {
        override fun areItemsTheSame(oldItem: StockHistory, newItem: StockHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockHistory, newItem: StockHistory): Boolean {
            return oldItem == newItem
        }
    }
} 