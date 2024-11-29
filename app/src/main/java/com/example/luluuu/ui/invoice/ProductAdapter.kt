package com.example.luluuu.ui.invoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemProductBinding

class ProductAdapter(
    private val products: List<Product>,
    private val onProductChanged: () -> Unit,
    private val onRemoveProduct: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(product: Product) {
            with(binding) {
                productNameEditText.setText(product.name)
                productPriceEditText.setText(product.price.toString())
                productQuantityEditText.setText(product.quantity.toString())

                productNameEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        product.name = productNameEditText.text.toString()
                        onProductChanged()
                    }
                }

                productPriceEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        product.price = productPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
                        onProductChanged()
                    }
                }

                productQuantityEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        product.quantity = productQuantityEditText.text.toString().toIntOrNull() ?: 1
                        onProductChanged()
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