package world.shanya.serialportutils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.Fragment

const val TAG = "SerialPortInvisibleFragment"

/**
 * 一个不可见的Fragment
 */
class InvisibleFragment: Fragment() {

    /**
     * 立即注册所有的广播
     */
    fun registerReceiverNow(broadcastReceiver: BroadcastReceiver) {
        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
    }
}