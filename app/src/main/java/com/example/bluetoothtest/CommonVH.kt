package com.example.bluetoothtest

import android.view.View
import androidx.recyclerview.widget.RecyclerView

open class CommonVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var mCurrentPosition = 0
    open fun onBind(position: Int) {
        mCurrentPosition = position
    }
}