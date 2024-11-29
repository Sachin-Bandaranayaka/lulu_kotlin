package com.example.luluuu

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.databinding.ActivityInvoiceBinding
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

class InvoiceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInvoiceBinding
    private val products = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        updateTotalAmount()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            products,
            onProductChanged = { updateTotalAmount() },
            onRemoveProduct = { position ->
                products.removeAt(position)
                productAdapter.notifyItemRemoved(position)
                updateTotalAmount()
            }
        )

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@InvoiceActivity)
            adapter = productAdapter
        }
    }

    private fun setupButtons() {
        binding.addProductFab.setOnClickListener {
            products.add(Product())
            productAdapter.notifyItemInserted(products.size - 1)
            updateTotalAmount()
        }

        binding.generateInvoiceFab.setOnClickListener {
            if (validateInputs()) {
                generateAndPrintInvoice()
            }
        }
    }

    private fun updateTotalAmount() {
        val total = products.sumOf { it.total() }
        val formattedTotal = NumberFormat.getCurrencyInstance(Locale("si", "LK"))
            .format(total)
            .replace("LKR", "Rs.")
        binding.totalAmountTextView.text = getString(R.string.total_amount, formattedTotal)
    }

    private fun validateInputs(): Boolean {
        if (binding.customerNameEditText.text.toString().isBlank()) {
            Toast.makeText(this, "Please enter customer name", Toast.LENGTH_SHORT).show()
            return false
        }

        if (products.isEmpty()) {
            Toast.makeText(this, "Please add at least one product", Toast.LENGTH_SHORT).show()
            return false
        }

        for (product in products) {
            if (product.name.isBlank() || product.price <= 0) {
                Toast.makeText(this, "Please fill all product details correctly", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun generateAndPrintInvoice() {
        val bluetoothSocket = MainActivity.bluetoothSocket
        if (bluetoothSocket != null) {
            if (bluetoothSocket.isConnected()) {
                // Bluetooth is connected, proceed with printing
                val outputStream = bluetoothSocket.outputStream
                val customerName = binding.customerNameEditText.text.toString()
                val invoiceString = buildInvoiceString(customerName)

                try {
                    outputStream.write(invoiceString.toByteArray())
                    outputStream.write(byteArrayOf(0x0A)) // Line feed
                    outputStream.flush()
                    Toast.makeText(this, "Invoice printed", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity after successful print
                } catch (e: IOException) {
                    Toast.makeText(this, "Printing failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    MainActivity.bluetoothSocket = null
                    MainActivity.outputStream = null
                }
            } else {
                // Bluetooth is not connected
                Toast.makeText(this, "Bluetooth is not connected", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Bluetooth socket is null
            Toast.makeText(this, "Bluetooth socket is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildInvoiceString(customerName: String): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("si", "LK"))
        return buildString {
            appendLine("================================")
            appendLine("           ${getString(R.string.invoice_header)}")
            appendLine("================================")
            appendLine()
            appendLine("${getString(R.string.customer)}: $customerName")
            appendLine()
            appendLine(getString(R.string.item_details))
            appendLine("--------------------------------")
            
            products.forEach { product ->
                val formattedPrice = currencyFormat.format(product.price)
                    .replace("LKR", "Rs.")
                val formattedTotal = currencyFormat.format(product.total())
                    .replace("LKR", "Rs.")
                appendLine("${product.name}")
                appendLine("${product.quantity} x $formattedPrice")
                appendLine("Total: $formattedTotal")
                appendLine("--------------------------------")
            }
            
            val grandTotal = currencyFormat.format(products.sumOf { it.total() })
                .replace("LKR", "Rs.")
            appendLine("Grand Total: $grandTotal")
            appendLine()
            appendLine(getString(R.string.thank_you))
            appendLine()
            appendLine()
        }
    }
} 