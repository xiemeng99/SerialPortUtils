package world.shanya.serialportutils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets


/**
 * @ClassName SerialPort
 * @Description 蓝牙串口工具类
 * @Author Shanya
 * @Date 2020/7/11 20:33
 * @Version 1.0.0
 **/
class SerialPort private constructor(private val context: Context){

    //懒汉式SingleTon
    companion object {

        const val SEND_DATA_TYPE_HEX = 0x11
        const val SEND_DATA_TYPE_STRING = 0x12
        const val READ_DATA_TYPE_HEX = 0x13
        const val READ_DATA_TYPE_STRING = 0x14

        private var instance: SerialPort ?= null
        fun getInstance(context: Context): SerialPort{
            val temp = instance
            if (temp != null){
                return temp
            }
            return synchronized(this) {
                val temp2 = instance
                if (temp2 != null) {
                    temp2
                } else {
                    val created = SerialPort(context.applicationContext)
                    instance = created
                    created
                }
            }
        }
    }

    //各种状态回调
    private var scanStatusCallback : ((status:Boolean) -> Unit) ?= null
    private var connectionCallback : ((result:Boolean) -> Unit) ?= null
    private var readDataCallback : ((data:String) -> Unit) ?= null

    //SPP服务UUID号
    private val uuid = "00001101-0000-1000-8000-00805F9B34FB"

    //获取蓝牙设配器
    private val bluetoothAdapter:BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket:BluetoothSocket ?= null
    private var inputStream: InputStream ?= null
    val pairedDevicesArrayAdapter:ArrayAdapter<String> = ArrayAdapter(context,R.layout.device_name)
    val unPairedDevicesArrayAdapter:ArrayAdapter<String> = ArrayAdapter(context,R.layout.device_name)

    var readDataType = READ_DATA_TYPE_STRING
    var sendDataType = SEND_DATA_TYPE_STRING

    //接收线程打开标志
    private var readThreadStarted = false


    /**
    * 蓝牙设配器各种状态的广播监听器
    * @Author Shanya
    * @Date 2020/7/12 16:33
    * @Version 1.0.0
    */
    private val searchReceiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action){
                val device:BluetoothDevice ?= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null){
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        unPairedDevicesArrayAdapter.add("${device.name}\n${device.address}")

                    }
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action){
                //搜索完成
                scanStatusCallback?.invoke(false)
                if (unPairedDevicesArrayAdapter.count == 0){
                    //没有搜索到设备
                    unPairedDevicesArrayAdapter.add(context?.getString(R.string.no_available))
                }
            }else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action){
                val device:BluetoothDevice ?= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                bluetoothSocket = null
                connectionCallback?.invoke(false)
                Toast.makeText(context,"${device?.name}  ${context?.getString(R.string.disconnect)}",Toast.LENGTH_SHORT).show()
            }
        }
    }

    init {
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothDevice.ACTION_FOUND))
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
        val pairedDevices:Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()){
            for (device in pairedDevices){
                pairedDevicesArrayAdapter.add("${device.name}\n${device.address}")
            }
        }else {
            pairedDevicesArrayAdapter.add(context.getString(R.string.no_paired))
        }
    }

    /**
    * 打开搜索页面
    * @Author Shanya
    * @Date 2020/7/12 16:30
    * @Version 1.0.0
    */
    fun openSearchPage(context: Context){
        context.startActivity(Intent(context,SearchActivity::class.java))
    }

    /**
    * 开始搜索可用设备
    * @Author Shanya
    * @Date 2020/7/12 16:31
    * @Version 1.0.0
    */
    fun doDiscovery(context: Context){
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context,context.getString(R.string.no_permission),Toast.LENGTH_SHORT).show()
        }else{
            if (!bluetoothAdapter.isEnabled){
                Toast.makeText(context,context.getString(R.string.open_bluetooth),Toast.LENGTH_SHORT).show()
                return
            }
            if (bluetoothAdapter.isDiscovering){
                bluetoothAdapter.cancelDiscovery()
                scanStatusCallback?.invoke(false)
            }
            unPairedDevicesArrayAdapter.clear()
            bluetoothAdapter.startDiscovery()
            scanStatusCallback?.invoke(true)
        }
    }

    /**
    * 搜索列表项的先择监听
    * @Author Shanya
    * @Date 2020/7/12 16:31
    * @Version 1.0.0
    */
    val devicesClickListener =
        AdapterView.OnItemClickListener { _, view, _, _ ->
            if (!bluetoothAdapter.isEnabled){
                Toast.makeText(context,context.getString(R.string.open_bluetooth),Toast.LENGTH_SHORT).show()
                return@OnItemClickListener
            }
            val info = (view as TextView).text.toString()
            if (info == context.getString(R.string.no_paired) || info == context.getString(R.string.no_available)) {
                return@OnItemClickListener
            }
            bluetoothAdapter.cancelDiscovery()
            val address = info.substring(info.length - 17)
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString(uuid))
                bluetoothSocket?.connect()
                connectionCallback?.invoke(true)
                Toast.makeText(
                    context,
                    "${bluetoothDevice.name}  ${context.getString(R.string.connect_success)}",
                    Toast.LENGTH_SHORT
                ).show()
                inputStream = bluetoothSocket?.inputStream
                //打开接收线程
                if (!readThreadStarted){
                    readThread.start()
                    readThreadStarted = true
                }
            }catch (e:IOException){
                connectionCallback?.invoke(false)
                Toast.makeText(context,context.getString(R.string.connection_failed),Toast.LENGTH_SHORT).show()
                try {
                    bluetoothSocket?.close()
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }


    fun getScanStatus(scanStatusCallback: (status:Boolean) -> Unit){
        this.scanStatusCallback = scanStatusCallback
    }
    
    /**
    * 连接状态回调
    * @Author Shanya
    * @Date 2020/7/19 21:11
    * @Version 1.0.0
    */
    fun getConnectionResult(connectionCallback: (result:Boolean) -> Unit){
        this.connectionCallback = connectionCallback
    }


    /**
    * 获取数据
    * @Author Shanya
    * @Date 2020/7/18 21:39
    * @Version 1.0.0
    */
    fun getReceivedData(readDataCallback: (data:String) -> Unit) {
        this.readDataCallback = readDataCallback
    }

    /**
    * 接收数据的线程
    * @Author Shanya
    * @Date 2020/7/12 17:18
    * @Version 1.0.0
    */
    private val readThread = object :Thread(){
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun run() {
            super.run()
            var len: Int
            var receivedData:String
            var buffer = ByteArray(0)
            var flag = false
            while (true){
                if (!readThreadStarted) {
                    return
                }
                sleep(100)
                len = inputStream?.available()!!
                while (len != 0){
                    flag = true
                    buffer = ByteArray(len)
                    inputStream?.read(buffer)
                    sleep(10)
                    len = inputStream?.available()!!
                }
                if (flag){
                    if (readDataType == READ_DATA_TYPE_STRING){
                        receivedData = String(buffer,StandardCharsets.UTF_8)
                        readDataCallback?.invoke(receivedData)
                    }else{
                        val sb = StringBuilder()
                        for (i in buffer){
                            sb.append("${String.format("%2X", i)} ")
                        }
                        readDataCallback?.invoke(sb.toString())
                    }
                    flag = false
                }
            }
        }
    }

    /**
    * 发送数据，（使用线程发送）
    * @Author Shanya
    * @Date 2020/7/12 18:46
    * @Version 1.0.0
    */
    fun sendData(data:String){
        if (!bluetoothAdapter.isEnabled){
            Toast.makeText(context,context.getString(R.string.open_bluetooth),Toast.LENGTH_SHORT).show()
            return
        }
        if (bluetoothSocket == null) {
            Toast.makeText(context,context.getString(R.string.connect_device),Toast.LENGTH_SHORT).show()
            return
        }

        Thread(Runnable {
            send(data)
        }).start()
    }

    private fun stringToHex(str: String): ArrayList<Byte>? {
        val chars = "0123456789ABCDEF".toCharArray()
        val bs = str.toCharArray()
        var bit = 0
        var i = 0
        val intArray = ArrayList<Byte>()
        if (str.length and 0x01 != 0){
            throw  RuntimeException("字符个数不是偶数")
        }
        while (i < bs.size) {
            for (j in chars.indices) {
                if (bs[i] == chars[j]) {
                    bit += (j * 16)
                }
                if (bs[i + 1] == chars[j]) {
                    bit += j
                }
            }
            intArray.add(bit.toByte())
            i += 2
            bit = 0
        }
        return intArray
    }


    /**
    * 发送数据，对内（直接发送，会造成阻塞）
    * @Author Shanya
    * @Date 2020/7/12 19:01
    * @Version 1.0.0
    */
    private fun send(data: String) {
        val outputStream = bluetoothSocket?.outputStream
        var n = 0
        val bos: ByteArray = if (sendDataType == SEND_DATA_TYPE_STRING) {
            data.toByteArray()
        }else{
            stringToHex(data)?.toList()!!.toByteArray()
        }

        for (bo in bos) {
            if (bo.toInt() == 0x0a) {
                n++
            }
        }
        val bosNew = ByteArray(bos.size + n)
        n = 0
        for (bo in bos) {
            if (bo.toInt() == 0x0a) {
                bosNew[n++] = 0x0d
                bosNew[n] = 0x0a
            } else {
                bosNew[n] = bo
            }
            n++
        }
        outputStream?.write(bosNew)
    }





}
