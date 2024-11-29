package com.example.luluuu

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemProductBinding

class ProductAdapter(
    private val products: MutableList<Product>,
    private val onProductChanged: () -> Unit,
    private val onRemoveProduct: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productNameEditText.setText(product.name)
            binding.productPriceEditText.setText(product.price.toString())
            binding.productQuantityEditText.setText(product.quantity.toString())

            binding.productNameEditText.addTextChangedListener(createTextWatcher { text ->
                product.name = text
                onProductChanged()
            })

            binding.productPriceEditText.addTextChangedListener(createTextWatcher { text ->
                product.price = text.toDoubleOrNull() ?: 0.0
                onProductChanged()
            })

            binding.productQuantityEditText.addTextChangedListener(createTextWatcher { text ->
                product.quantity = text.toIntOrNull() ?: 1
                onProductChanged()
            })

            binding.removeProductButton.setOnClickListener {
                onRemoveProduct(adapterPosition)
            }
        }

        private fun createTextWatcher(onTextChanged: (String) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    onTextChanged(s?.toString() ?: "")
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