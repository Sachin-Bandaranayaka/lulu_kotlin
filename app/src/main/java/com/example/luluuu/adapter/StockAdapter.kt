package com.example.luluuu.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemInvoiceProductBinding
import com.example.luluuu.model.InvoiceItem
import com.example.luluuu.model.Stock
import java.text.NumberFormat
import java.util.Locale

class StockAdapter(
    private val items: MutableList<InvoiceItem>,
    private val onItemChanged: () -> Unit,
    private val onRemoveItem: (Int) -> Unit,
    private val onStockClick: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

    var availableStocks: List<Stock> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInvoiceProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemInvoiceProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

        init {
            binding.removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveItem(position)
                }
            }

            binding.quantityEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val currentItem = items[position]
                        val newQuantity = binding.quantityEditText.text.toString().toIntOrNull() ?: 0
                        if (newQuantity != currentItem.quantity) {
                            val stock = availableStocks.find { it.name == currentItem.productName }
                            if (stock != null && stock.quantity >= newQuantity) {
                                // Create a new item with updated values
                                val updatedItem = currentItem.copy(
                                    quantity = newQuantity,
                                    total = currentItem.price * newQuantity
                                )
                                items[position] = updatedItem
                                onItemChanged()
                                notifyItemChanged(position)
                            }
                        }
                    }
                }
            }
        }

        fun bind(item: InvoiceItem) {
            binding.apply {
                productNameTextView.text = item.productName
                quantityEditText.setText(item.quantity.toString())
                priceTextView.text = currencyFormat.format(item.price)
                    .replace("LKR", "Rs.")
                totalTextView.text = currencyFormat.format(item.total)
                    .replace("LKR", "Rs.")

                val stock = availableStocks.find { it.name == item.productName }
                availableQuantityTextView.text = "Available: ${stock?.quantity ?: 0}"
            }
        }
    }
}