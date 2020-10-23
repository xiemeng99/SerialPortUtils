package world.shanya.serialportutils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class SerialPortService : LifecycleService() {

    /**
     * 接收到的数据
     */
    val receiveLiveData = MutableLiveData("")

    inner class SerialPortServiceBinder : Binder() {
        val serialPortService = this@SerialPortService
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        lifecycleScope.launch {
            var len: Int
            var receivedData:String
            var buffer = ByteArray(0)
            var flag = false

            while (true) {
                delay(100)
                len = SerialPort.inputStream?.available()!!
                while (len != 0){
                    flag = true
                    buffer = ByteArray(len)
                    SerialPort.inputStream?.read(buffer)
                    delay(10)
                    len = SerialPort.inputStream?.available()!!
                }
                if (flag){
                    if (SerialPort.readDataType == SerialPort.READ_STRING){
                        receivedData = String(buffer, StandardCharsets.UTF_8)
                        receiveLiveData.value = receivedData
                    }else{
                        val sb = StringBuilder()
                        for (i in buffer){
                            sb.append("${String.format("%2X", i)} ")
                        }
                        receiveLiveData.value = sb.toString()
                    }
                    flag = false
                }
            }
        }
        return SerialPortServiceBinder()
    }


    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

            SerialPort.bluetoothSocket?.remoteDevice?.connectGatt(
                this,
                false,
                object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        super.onConnectionStateChange(gatt, status, newState)
                        if (status == BluetoothGatt.STATE_DISCONNECTED) {
                            gatt?.close()
                        }
                    }
                }
            )?.disconnect()
            SerialPort.bluetoothSocket?.close()

        }
    }
}
