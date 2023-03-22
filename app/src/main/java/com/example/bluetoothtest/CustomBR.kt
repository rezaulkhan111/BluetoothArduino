package com.example.bluetoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

open class CustomBR : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.e("MA", "onReceive: ${action.toString()}")

        when (action) {
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
//                mDeviceList = ArrayList<BluetoothDevice>()
            }
            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                mDeviceList.add(device!!)
                Log.e("MA", "onReceive: " + device)
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                Log.e("MA", "onReceive: " + mDeviceList.size)
//                mDeviceAdapter.setDeviceList(mDeviceList)
            }
        }
    }
}