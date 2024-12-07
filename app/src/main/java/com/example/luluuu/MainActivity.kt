package com.example.luluuu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.luluuu.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val firebaseRepository = FirebaseRepository()
    private lateinit var stockViewModel: StockViewModel

    private val resolutionForResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            setupApp()
        } else {
            showGooglePlayServicesError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stockViewModel = ViewModelProvider(this)[StockViewModel::class.java]

        setupNavigation()
        setupRefreshLayout()
        initializeGooglePlayServices()
    }

    private fun setupRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        // Customize the refresh indicator colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun refreshData() {
        lifecycleScope.launch {
            try {
                // Refresh data from Firebase
                stockViewModel.refreshStocks()
                
                // Show success message
                Toast.makeText(this@MainActivity, "Data refreshed successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Show error message
                Toast.makeText(this@MainActivity, "Error refreshing data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error refreshing data", e)
            } finally {
                // Hide the refresh indicator
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupNavigation() {
        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavView.setupWithNavController(navController)
    }

    private fun initializeGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d("MainActivity", "Google Play Services is available")
                setupApp()
            }
            else -> {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
                        ?: showGooglePlayServicesError()
                } else {
                    showGooglePlayServicesError()
                }
            }
        }
    }

    private fun showGooglePlayServicesError() {
        Toast.makeText(
            this,
            "This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun setupApp() {
        // Check network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Some features may not work.", Toast.LENGTH_LONG).show()
        }

        // Check and request permissions before connecting to printer
        checkBluetoothPermissions()

        // Test Firebase connection only if network is available
        if (isNetworkAvailable()) {
            lifecycleScope.launch {
                try {
                    firebaseRepository.getAllStocks().collectLatest { stocks: List<Stock> ->
                        Log.d("Firebase", "Connected to Firebase successfully")
                    }
                } catch (e: Exception) {
                    Log.e("Firebase", "Error connecting to Firebase: ${e.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Firebase error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
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

    fun getBluetoothSocket(): BluetoothSocket? {
        return bluetoothSocket
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
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
}