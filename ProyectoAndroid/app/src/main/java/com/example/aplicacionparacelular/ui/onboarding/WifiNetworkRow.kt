package com.example.aplicacionparacelular.ui.onboarding

import androidx.core.view.isVisible
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.ItemWifiNetworkBinding

object WifiNetworkRow {
    fun bind(item: ItemWifiNetworkBinding, ssid: String, secure: Boolean, selected: Boolean) {
        item.txtNetworkSsid.text = ssid
        item.imgLock.isVisible = secure
        item.txtNetworkOpen.isVisible = !secure
        item.radioNetwork.isChecked = selected
        item.root.setBackgroundResource(if (selected) R.drawable.teo_network_selected else 0)
        item.root.contentDescription = item.root.context.getString(
            if (selected) R.string.wifi_network_selected else R.string.wifi_network_available,
            ssid,
        )
    }
}
