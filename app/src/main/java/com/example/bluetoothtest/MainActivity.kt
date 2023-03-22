package com.example.bluetoothtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.*


class MainActivity : AppCompatActivity(), OnPairButtonClickListener {

    lateinit var rvBluetoothDevice: RecyclerView

    private lateinit var mBlueAdapter: BluetoothAdapter
    private var mDevice: BluetoothDevice? = null
    var mHandler: Handler? = null

    private var mDeviceList = ArrayList<BluetoothDevice>()
    private lateinit var mDeviceAdapter: DeviceListAdapter
    private lateinit var mProgressDlg: ProgressDialog

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnOnOff: Button = findViewById(R.id.btn_on_off)
        val btnScanDevice: Button = findViewById(R.id.btn_scan_device)
        val btnPairDevice: Button = findViewById(R.id.btn_pair_device)


        val btnConnect: Button = findViewById(R.id.btn_connect)
        val btnReceived: Button = findViewById(R.id.btn_received)
        rvBluetoothDevice = findViewById(R.id.rv_bluetooth_device)
        mDeviceAdapter = DeviceListAdapter(this)
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter()

        mProgressDlg = ProgressDialog(this)

        mProgressDlg.setMessage("Scanning...")
        mProgressDlg.setCancelable(false)
        mProgressDlg.setButton(
            DialogInterface.BUTTON_NEGATIVE, "Cancel"
        ) { dialog, _ ->
            dialog.dismiss()

            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mBlueAdapter.cancelDiscovery()
            }
        }

        btnOnOff.setOnClickListener {
            if (!mBlueAdapter.isEnabled) {
                btnOnOff.text = "Off"
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    startActivityForResult(enableIntent, 1000)
                }
                btnScanDevice.isEnabled = true
                btnPairDevice.isEnabled = true
            } else {
                btnOnOff.text = "On"
                mBlueAdapter.disable()

                btnScanDevice.isEnabled = false
                btnPairDevice.isEnabled = false
            }
        }

        btnScanDevice.setOnClickListener {
            if (btnScanDevice.text == "Scan") {
                btnScanDevice.text = "Stop"
                val filter = IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                registerReceiver(mSearchDeviceBR, filter)
                var permissionCheck = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionCheck += checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (permissionCheck != 0) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ), 1001
                    )
                } else {
                    mBlueAdapter.startDiscovery()
                }
            } else {
                btnScanDevice.text = "Scan"
            }
        }

        btnPairDevice.setOnClickListener {
            val pairedDevices: Set<BluetoothDevice> = mBlueAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                mDeviceList.clear()
                mDeviceList.addAll(pairedDevices)
                rvBluetoothDevice.apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    mDeviceAdapter.setDeviceList(mDeviceList)
                    mDeviceAdapter.notifyDataSetChanged()
                    adapter = mDeviceAdapter
                }
            }
        }

        mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                val writeBuf = msg.obj as ByteArray
                val begin = msg.arg1
                val end = msg.arg2
                when (msg.what) {
                    1 -> {
                        var writeMessage = String(writeBuf)
                        writeMessage = writeMessage.substring(begin, end)

                        Log.e("MA", "handleMessage: $writeMessage")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(mSearchDeviceBR)

        unregisterReceiver(mRequestPairDeviceBR)
        super.onDestroy()
    }

    inner class ConnectThread(mmDevice: BluetoothDevice?) : Thread() {
        private var mmSocket: BluetoothSocket? = null

        //        private val MYUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private val uuid = UUID.randomUUID()

        init {
            var blueSocket: BluetoothSocket? = null
            try {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    blueSocket = mmDevice?.createRfcommSocketToServiceRecord(uuid)
                }
            } catch (ex: IOException) {
                Log.e("MA", "init: catch $ex")
            }
            mmSocket = blueSocket
        }

        override fun run() {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mBlueAdapter.cancelDiscovery()

                try {
                    mmSocket?.connect()
                } catch (connectException: IOException) {
                    Log.e("MA", "run: connectException $connectException")
                    try {
                        mmSocket?.close()
                    } catch (closeEx: IOException) {
                        Log.e("MA", "run: closeEx $closeEx")
                    }
                    return
                }
            }

            Log.e("MA", "run: mmSocket " + mmSocket.toString())
            val mConnectedThread = ConnectedThread(mmSocket);
            mConnectedThread.start()
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (_: IOException) {
            }
        }
    }

    //data read and write
    inner class ConnectedThread(private val mmSocket: BluetoothSocket?) : Thread() {
        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream? = null

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = mmSocket?.inputStream
                tmpOut = mmSocket?.outputStream
            } catch (_: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut

            Log.e("MA", "ConnectedThread init: mmInStream " + mmInStream.toString())
            Log.e("MA", "ConnectedThread init: mmOutStream " + mmOutStream.toString())
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var begin = 0
            var bytes = 0
            Log.e("MA", "ConnectedThread run " + mmInStream.toString())
            while (true) {
                try {
                    bytes += mmInStream!!.read(buffer, bytes, buffer.size - bytes)
                    for (i in begin until bytes) {
                        if (buffer[i] == "#".toByteArray()[0]) {
                            mHandler?.obtainMessage(1, begin, i, buffer)?.sendToTarget()
                            begin = i + 1
                            if (i == bytes - 1) {
                                bytes = 0
                                begin = 0
                            }
                        }
                    }
                    write(buffer)
                } catch (e: IOException) {
                    Log.e("MA", "ConnectedThread catch $e")

                    break
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPairButtonClick(mBlueDevice: BluetoothDevice, position: Int) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (mBlueDevice.bondState == BluetoothDevice.BOND_BONDED) {
//                unpairDevice(mBlueDevice)
            } else {
                showToast("Pairing...")
                pairDevice(mBlueDevice)
            }
        }

        registerReceiver(
            mRequestPairDeviceBR, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }

    override fun onConnectDeviceClick(mBlueDevice: BluetoothDevice, position: Int) {
        val mConnectThread = ConnectThread(mBlueDevice)
        mConnectThread.start()
    }

    private fun pairDevice(device: BluetoothDevice) {
        try {
            val method: Method = device.javaClass.getMethod("createBond", null as Class<*>?)
            method.invoke(device, null as Array<Any?>?)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* private fun unpairDevice(device: BluetoothDevice) {
         try {
             val method: Method =
                 device.javaClass.getMethod("removeBond", null as Class<*>?)
             method.invoke(device, null as Array<Any?>?)
         } catch (e: Exception) {
             e.printStackTrace()
         }
     }*/

    private val mSearchDeviceBR: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    mDeviceList = ArrayList<BluetoothDevice>()
                    mProgressDlg.show()
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    mDeviceList.add(device!!)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    mProgressDlg.dismiss()
                    rvBluetoothDevice.apply {
                        layoutManager = LinearLayoutManager(
                            this@MainActivity, LinearLayoutManager.VERTICAL, false
                        )
                        mDeviceAdapter.setDeviceList(mDeviceList)
                        mDeviceAdapter.notifyDataSetChanged()
                        adapter = mDeviceAdapter
                    }
                }
            }
        }
    }

    private val mRequestPairDeviceBR: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val state =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                val prevState = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR
                )
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    showToast("Paired")
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    showToast("Unpaired")
                }
                mDeviceAdapter.notifyDataSetChanged()
            }
        }
    }
}