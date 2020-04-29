package it.gcriscione.bletestcontact

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.preference.PreferenceManager
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    private var advData: AdvertiseData? = null
    private var scanSettings: ScanSettings? = null
    private var scanFilters: MutableList<ScanFilter>? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var advSettings: AdvertiseSettings? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null

    var rssiListAdapter: RssiListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set the list adapter
        rssiListAdapter = RssiListAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = rssiListAdapter

        // Retrieve the instance id
        val instanceId = instanceId

        // Show the instanceId
        val textView = findViewById<TextView>(R.id.textView)
        val text = "ID: " + Utils.toHexString(instanceId)
        textView.text = text

        // Copy instanceId into service data
        System.arraycopy(
            instanceId,
            0,
            EDDYSTONE_SERVICE_DATA,
            12,
            6
        )
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Configure scanning
        bleScanner = bluetoothAdapter.bluetoothLeScanner
        scanSettings =
            ScanSettings.Builder().setReportDelay(1).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        scanFilters = ArrayList()
        val filter = ScanFilter.Builder()
            .setServiceUuid(UID_SERVICE)
            .setServiceData(
                UID_SERVICE,
                EDDYSTONE_NAMESPACE_FILTER,
                NAMESPACE_FILTER_MASK
            )
            .build()
        (scanFilters as ArrayList<ScanFilter>).add(filter)

        // Configure advertising
        bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        advSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val pUuid = parcelUuidFromShortUUID(0xFEAA)
        advData = AdvertiseData.Builder()
            .addServiceData(pUuid, EDDYSTONE_SERVICE_DATA)
            .addServiceUuid(pUuid)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
        if (bleScanner == null) {
            textView.text =
                "Bluetooth scan not available.\nPlease activate bluetooth and restart the app."
        } else if (bleAdvertiser == null) {
            textView.text =
                "Bluetooth advertising not available.\nPlease activate bluetooth and restart the app."
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask the permission to the user
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 1
            )
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Check the permission has been granted
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You must accept the permission", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onResume() {
        super.onResume()
        if (bleScanner != null) {
            bleScanner!!.startScan(scanFilters, scanSettings, myScanCallback)
        }
        if (bleAdvertiser != null) {
            bleAdvertiser!!.startAdvertising(advSettings, advData, myAdvertisingCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        if (bleScanner != null) {
            bleScanner!!.stopScan(myScanCallback)
        }
        if (bleAdvertiser != null) {
            bleAdvertiser!!.stopAdvertising(myAdvertisingCallback)
        }
    }

    private fun parcelUuidFromShortUUID(serviceId: Long): ParcelUuid {
        return ParcelUuid(
            UUID(
                (0x1000 or (serviceId shl 32).toInt()).toLong(),
                -0x7fffff7fa064cb05L
            )
        )
    }

    // Generate a random instance id
    private val instanceId: ByteArray
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            var instanceIdAsString = sharedPref.getString("pref_instance_id", null)
            val instanceId: ByteArray
            if (instanceIdAsString == null) {
                // Generate a random instance id
                val random = Random()
                instanceId = ByteArray(6)
                random.nextBytes(instanceId)
                instanceIdAsString = Utils.toHexString(instanceId)
                sharedPref.edit().putString("pref_instance_id", instanceIdAsString).apply()
            } else {
                instanceId = Utils.fromHexString(instanceIdAsString)
            }
            return instanceId
        }

    private var myScanCallback: ScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            Log.d("ScanCallback", results.toString())
            for (result in results) {
                rssiListAdapter?.add(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("onFail", java.lang.String.valueOf(rssiListAdapter?.itemCount))
            Toast.makeText(this@MainActivity, "Scan failed", Toast.LENGTH_SHORT).show()
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            Log.d("onScan", java.lang.String.valueOf(rssiListAdapter?.itemCount))
            Toast.makeText(this@MainActivity, "Scan result", Toast.LENGTH_SHORT).show()
            rssiListAdapter?.add(result)
        }
    }
    private var myAdvertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
        }
    }

    companion object {
        private val UID_SERVICE =
            ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb")
        private val EDDYSTONE_NAMESPACE_FILTER = byteArrayOf(
            0x00,  //Frame type
            0x00,  //TX power
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        private val NAMESPACE_FILTER_MASK = byteArrayOf(
            0xFF.toByte(),
            0x00,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        private val EDDYSTONE_SERVICE_DATA = byteArrayOf(
            0x00,  //Frame type
            -21,  //TX power
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0xaa.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
    }
}
