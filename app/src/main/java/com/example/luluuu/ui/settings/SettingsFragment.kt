package com.example.luluuu.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatDelegate
import com.example.luluuu.MainActivity
import com.example.luluuu.R
import com.example.luluuu.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothManager by lazy {
        requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val BLUETOOTH_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    private val BLUETOOTH_PERMISSION_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        bluetoothAdapter = bluetoothManager.adapter
        setupBluetoothUI()
        setupPrinterSettings()

        binding.darkModeSwitch.apply {
            isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            setOnCheckedChangeListener { _, isChecked ->
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    private fun setupBluetoothUI() {
        binding.apply {
            searchPrintersButton.setOnClickListener {
                if (hasBluetoothPermissions()) {
                    startPrinterDiscovery()
                } else {
                    requestBluetoothPermissions()
                }
            }

            // Update UI based on current connection status
            updatePrinterInfo()
        }
    }

    private fun updatePrinterInfo() {
        binding.apply {
            val connectedSocket = MainActivity.bluetoothSocket
            if (connectedSocket?.isConnected == true) {
                val device = connectedSocket.remoteDevice
                printerInfoCard.visibility = View.VISIBLE
                printerNameText.text = device.name ?: "Unknown Printer"
                printerAddressText.text = device.address
                connectionStatusText.text = "Connected"
                connectionStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                disconnectButton.visibility = View.VISIBLE
                disconnectButton.setOnClickListener {
                    disconnectPrinter()
                }
            } else {
                printerInfoCard.visibility = View.GONE
                disconnectButton.visibility = View.GONE
            }
        }
    }

    private fun startPrinterDiscovery() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices.toList()
        showPrinterSelectionDialog(pairedDevices)
    }

    private fun showPrinterSelectionDialog(devices: List<BluetoothDevice>) {
        val deviceNames = devices.map { it.name ?: "Unknown Device" }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Printer")
            .setItems(deviceNames) { _, position ->
                connectToPrinter(devices[position])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun connectToPrinter(device: BluetoothDevice) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPort service UUID
                )
                socket.connect()
                MainActivity.bluetoothSocket = socket
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                    updatePrinterInfo()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun disconnectPrinter() {
        try {
            MainActivity.bluetoothSocket?.close()
            MainActivity.bluetoothSocket = null
            updatePrinterInfo()
            Toast.makeText(context, "Disconnected from printer", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(context, "Error disconnecting: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return BLUETOOTH_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        requestPermissions(BLUETOOTH_PERMISSIONS, BLUETOOTH_PERMISSION_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startPrinterDiscovery()
            } else {
                Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPrinterSettings() {
        binding.testPrintButton.setOnClickListener {
            val bluetoothSocket = MainActivity.bluetoothSocket
            if (bluetoothSocket?.isConnected == true) {
                try {
                    val outputStream = bluetoothSocket.outputStream
                    val testString = buildString {
                        appendLine("================================")
                        appendLine("           TEST PRINT")
                        appendLine("================================")
                        appendLine()
                        appendLine("Printer is working correctly!")
                        appendLine()
                        appendLine()
                        appendLine()
                    }
                    outputStream.write(testString.toByteArray())
                    outputStream.flush()
                    Toast.makeText(context, "Test print successful", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(context, "Printing failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    MainActivity.bluetoothSocket = null
                }
            } else {
                Toast.makeText(context, "Printer not connected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 