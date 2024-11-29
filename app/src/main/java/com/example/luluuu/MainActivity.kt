package com.example.luluuu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.luluuu.databinding.ActivityMainBinding
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    companion object {
        private const val PRINTER_MAC_ADDRESS = "60:6E:41:76:9B:A0"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
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

        // Connect to the printer
        connectToPrinter()
    }

    private fun connectToPrinter() {
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
        bluetoothSocket?.let { socket ->
            if (socket.isConnected) {
                try {
                    val outputStream = socket.outputStream
                    
                    // Initialize printer
                    outputStream.write(byteArrayOf(0x1B, 0x40))
                    outputStream.write(byteArrayOf(0x1B, 0x21, 0x00))
                    
                    // Write text
                    outputStream.write(text.toByteArray())
                    
                    // Feed and cut
                    outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00))
                    outputStream.flush()
                } catch (e: IOException) {
                    throw e
                }
            } else {
                throw IOException("Printer not connected")
            }
        } ?: throw IOException("Printer not connected")
    }
} 