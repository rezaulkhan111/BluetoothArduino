package com.example.bluetoothtest

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView


class DeviceListAdapter(private val callbackListener: OnPairButtonClickListener) :
    RecyclerView.Adapter<DeviceListAdapter.DeviceListVH>() {
    private var listDevice: MutableList<BluetoothDevice> = mutableListOf()

    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListVH {
        return DeviceListVH(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_device, parent, false)
        )
    }

    fun setDeviceList(devices: MutableList<BluetoothDevice>) {
        this.listDevice = devices
    }

    override fun getItemCount(): Int {
        return listDevice.size
    }

    override fun onBindViewHolder(holder: DeviceListVH, position: Int) {
        holder.onBind(position)

        holder.btnPair.setOnClickListener {
            callbackListener.onPairButtonClick(listDevice[position], position)
        }

        holder.itemView.setOnClickListener {
            callbackListener.onConnectDeviceClick(listDevice[position], position)
        }
    }

    inner class DeviceListVH(viewItem: View) : CommonVH(viewItem) {

        val tvName: TextView = viewItem.findViewById(R.id.tv_name)
        val tvAddress: TextView = viewItem.findViewById(R.id.tv_address)
        val btnPair: Button = viewItem.findViewById(R.id.btn_pair)

        @SuppressLint("SetTextI18n")
        override fun onBind(position: Int) {
            super.onBind(position)
            val mDevice = listDevice[position]

            if (ActivityCompat.checkSelfPermission(
                    itemView.context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                tvName.text = mDevice.name
                tvAddress.text = mDevice.address.toString()

                if (mDevice.bondState == BluetoothDevice.BOND_BONDED) {
                    btnPair.text = "Paired"
                } else {
                    btnPair.text = "Unpaired"
                }
            }
        }
    }
}