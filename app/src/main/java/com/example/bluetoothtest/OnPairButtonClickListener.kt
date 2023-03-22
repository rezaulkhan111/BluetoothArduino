package com.example.bluetoothtest

import android.bluetooth.BluetoothDevice

interface OnPairButtonClickListener {
    fun onPairButtonClick(mBlueDevice: BluetoothDevice, position: Int)

    fun onConnectDeviceClick(mBlueDevice: BluetoothDevice, position: Int)
}