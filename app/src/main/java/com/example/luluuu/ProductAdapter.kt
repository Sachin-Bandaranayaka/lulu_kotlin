package com.example.luluuu

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemProductBinding
import com.example.luluuu.model.Stock

class ProductAdapter(
    private val products: List<Product>,
    private val onProductChanged: () -> Unit,
    private val onRemoveProduct: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    var availableProducts: List<Stock> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.apply {
                // Find the corresponding stock for this product
                val stock = availableProducts.find { it.name == product.name }
                
                productNameTextView.text = product.name
                productPriceEditText.setText(product.price.toString())
                productQuantityEditText.setText(product.quantity.toString())

                // Show available quantity
                productAvailableQuantity.text = "Available: ${stock?.quantity ?: 0}"

                productPriceEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        product.price = productPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
                        onProductChanged()
                    }
                }

                productQuantityEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val newQuantity = productQuantityEditText.text.toString().toIntOrNull() ?: 1
                        // Check if quantity is valid
                        if (stock != null && newQuantity <= stock.quantity) {
                            product.quantity = newQuantity
                            onProductChanged()
                        } else {
                            // Reset to previous quantity if invalid
                            productQuantityEditText.setText(product.quantity.toString())
                            Toast.makeText(
                                binding.root.context,
                                "Insufficient stock available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                removeProductButton.setOnClickListener {
                    onRemoveProduct(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
} 