package com.example.luluuu.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemStockBinding
import com.example.luluuu.model.Stock
import java.text.NumberFormat
import java.util.Locale

class StockAdapter(
    private val onStockClick: (Stock) -> Unit,
    private val onEditClick: (Stock) -> Unit,
    private val onDeleteClick: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    private var stocks = listOf<Stock>()

    fun submitList(newStocks: List<Stock>) {
        val oldList = stocks
        stocks = newStocks
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newStocks.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = 
                oldList[oldPos].id == newStocks[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = 
                oldList[oldPos] == newStocks[newPos]
        }).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(stocks[position])
    }

    override fun getItemCount(): Int = stocks.size

    inner class StockViewHolder(
        private val binding: ItemStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

        init {
            binding.apply {
                editButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onEditClick(stocks[position])
                    }
                }

                deleteButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onDeleteClick(stocks[position])
                    }
                }

                historyButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onStockClick(stocks[position])
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
} 