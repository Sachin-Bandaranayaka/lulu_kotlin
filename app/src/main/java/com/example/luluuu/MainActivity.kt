package com.example.luluuu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.luluuu.databinding.ActivityMainBinding
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.luluuu.repository.FirebaseRepository
import com.example.luluuu.model.Stock
import com.example.luluuu.viewmodel.StockViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val firebaseRepository = FirebaseRepository()
    
    companion object {
        private const val PRINTER_MAC_ADDRESS = "60:6E:41:76:9B:A0"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
        
        private var _bluetoothSocket: BluetoothSocket? = null
        private var _outputStream: OutputStream? = null

        var bluetoothSocket: BluetoothSocket?
            get() = _bluetoothSocket
            set(value) {
                try {
                    _bluetoothSocket?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                _bluetoothSocket = value
                _outputStream = value?.outputStream
            }

        var outputStream: OutputStream?
            get() = _outputStream
            set(value) {
                try {
                    _outputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                _outputStream = value
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation
        binding.bottomNavView.setupWithNavController(navController)

        // Check and request permissions before connecting to printer
        checkBluetoothPermissions()

        // Test Firebase connection
        testFirebaseConnection()
        testStockSync()
        testFirebaseOperations()
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED -> {
                    // Permissions are already granted, proceed with connection
                    connectToPrinter()
                }
                else -> {
                    // Request permissions
                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN
                        ),
                        BLUETOOTH_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            // For Android 11 and below
            connectToPrinter()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permissions granted, proceed with connection
                    connectToPrinter()
                } else {
                    // Permissions denied
                    Toast.makeText(
                        this,
                        "Bluetooth permissions are required for printing",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun connectToPrinter() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
                return
            }

            // Get the printer device
            val printer = bluetoothAdapter.getRemoteDevice(PRINTER_MAC_ADDRESS)
            
            // Create a thread for connection to avoid blocking UI
            Thread {
                try {
                    // Create socket
                    val socket = printer.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
                    
                    // Connect
                    socket.connect()
                    
                    // Set the socket
                    bluetoothSocket = socket
                    
                    // Update UI on main thread
                    runOnUiThread {
                        Toast.makeText(this, "Printer connected successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this, "Failed to connect: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
            _outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun printText(text: String) {
        try {
            bluetoothSocket?.let { socket ->
                if (socket.isConnected) {
                    val outputStream = socket.outputStream
                    try {
                        // Initialize printer
                        outputStream.write(byteArrayOf(0x1B, 0x40))  // Initialize printer
                        outputStream.write(byteArrayOf(0x1B, 0x21, 0x00))  // Normal text
                        
                        // Set text alignment to center
                        outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))
                        
                        // Convert text to bytes with proper encoding
                        val textBytes = text.toByteArray(Charsets.UTF_8)
                        outputStream.write(textBytes)
                        
                        // Reset alignment to left
                        outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))
                        
                        // Feed paper
                        outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                        
                        // Cut paper (if supported)
                        try {
                            outputStream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))
                        } catch (e: Exception) {
                            Log.w("MainActivity", "Paper cut not supported: ${e.message}")
                        }
                        
                        outputStream.flush()
                        
                        // Log success
                        Log.d("MainActivity", "Print successful")
                        
                    } catch (e: IOException) {
                        Log.e("MainActivity", "Error during printing: ${e.message}")
                        e.printStackTrace()
                        throw IOException("Failed to print: ${e.message}")
                    }
                } else {
                    Log.e("MainActivity", "Printer not connected")
                    throw IOException("Printer not connected")
                }
            } ?: run {
                Log.e("MainActivity", "Bluetooth socket is null")
                throw IOException("Printer not connected (null socket)")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Printing failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                firebaseRepository.getAllStocks().collectLatest { stocks: List<Stock> ->
                    Log.d("Firebase", "Stocks: $stocks")
                }
            } catch (e: Exception) {
                Log.e("Firebase", "Error fetching stocks: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Firebase error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun testStockSync() {
        try {
            // Test adding a stock through Firebase directly
            val db = Firebase.firestore
            val testStock = hashMapOf(
                "name" to "Test Product",
                "price" to 99.99,
                "quantity" to 10,
                "description" to "Test Description"
            )

            db.collection("stocks")
                .add(testStock)
                .addOnSuccessListener { documentReference ->
                    Log.d("Firebase", "Stock added with ID: ${documentReference.id}")
                    Toast.makeText(
                        this@MainActivity,
                        "Test stock added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error adding stock", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to add test stock: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        } catch (e: Exception) {
            Log.e("Firebase", "Error in testStockSync", e)
            Toast.makeText(
                this@MainActivity,
                "Test sync failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun testFirebaseOperations() {
        lifecycleScope.launch {
            try {
                // Test cleanup
                firebaseRepository.cleanupTestData()
                Toast.makeText(this@MainActivity, "Cleaned up test data", Toast.LENGTH_SHORT).show()

                // Test search
                firebaseRepository.searchStocksByName("Test").collectLatest { stocks ->
                    Log.d("Firebase", "Search results: $stocks")
                }

                // Test low stock query
                firebaseRepository.getLowStockItems(5).collectLatest { stocks ->
                    Log.d("Firebase", "Low stock items: $stocks")
                }

            } catch (e: Exception) {
                Log.e("Firebase", "Error in Firebase operations: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Firebase operations failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
} 