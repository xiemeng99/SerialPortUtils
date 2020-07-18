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
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
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
    private var scanStatusCallback:ScanStatusCallback ?= null
    private var connectionCallback:ConnectionCallback ?= null
    private var readDataCallback: ReadDataCallback ?= null

    //SPP服务UUID号
    private val uuid = "00001101-0000-1000-8000-00805F9B34FB"

    //获取蓝牙设配器
    private val bluetoothAdapter:BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket:BluetoothSocket ?= null
    private var inputStream: InputStream ?= null
    val pairedDevicesArrayAdapter:ArrayAdapter<String> = ArrayAdapter(context,R.layout.device_name)
    val unPairedDevicesArrayAdapter:ArrayAdapter<String> = ArrayAdapter(context,R.layout.device_name)

    var readDataType = READ_DATA_TYPE_HEX
    private var readThreadStarted = false

    var switchOnText = ""
    var switchOffText = ""
    private var switchOnFlag = false
    private var switchOffFlag = false
    private var switchOnSendThread:SwitchOnSendThread ?= null
    private var switchOffSendThread:SwitchOffSendThread ?= null
    var sendDataDownString = ""
    private var sendDownFlag = false
    private var sendUpFlag = false
    private var buttonDownSendThread:ButtonDownSendThread ?= null



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
                scanStatusCallback?.scanStatus(false)
                if (unPairedDevicesArrayAdapter.count == 0){
                    //没有搜索到设备
                    unPairedDevicesArrayAdapter.add(context?.getString(R.string.no_available))
                }
            }else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action){
                val device:BluetoothDevice ?= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                bluetoothSocket = null
                connectionCallback?.connectionResult(false)
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
                scanStatusCallback?.scanStatus(false)
            }
            unPairedDevicesArrayAdapter.clear()
            bluetoothAdapter.startDiscovery()
            scanStatusCallback?.scanStatus(true)
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
                connectionCallback?.connectionResult(true)
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
                connectionCallback?.connectionResult(false)
                Toast.makeText(context,context.getString(R.string.connection_failed),Toast.LENGTH_SHORT).show()
                try {
                    bluetoothSocket?.close()
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }

    /**
    * 搜索状态的回调
    * @Author Shanya
    * @Date 2020/7/12 16:32
    * @Version 1.0.0
    */
    interface ScanStatusCallback{
        fun scanStatus(status: Boolean)
    }

    fun getScanStatus(scanStatusCallback: ScanStatusCallback){
        this.scanStatusCallback = scanStatusCallback
    }

    /**
    * 连接状态的回调
    * @Author Shanya
    * @Date 2020/7/12 16:32
    * @Version 1.0.0
    */
    interface ConnectionCallback{
        fun connectionResult(result: Boolean)
    }

    fun getConnectionResult(connectionCallback: ConnectionCallback){
        this.connectionCallback = connectionCallback
    }

    /**
     * 接收数据的回调
     * @Author Shanya
     * @Date 2020/7/12 17:19
     * @Version 1.0.0
     */
    interface ReadDataCallback{
        fun readData(data: String)
    }

    /**
    * 获取数据
    * @Author Shanya
    * @Date 2020/7/18 21:39
    * @Version 1.0.0
    */
    fun getReceivedData(readDataCallback: ReadDataCallback) {
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
                        readDataCallback?.readData(receivedData)
                    }else{
                        val sb = StringBuilder()
                        for (i in buffer){
                            sb.append("${String.format("%2x", i)} ")
                        }
                        readDataCallback?.readData(sb.toString())
                    }
                    flag = false
                }
            }
        }
    }



    /**
    * 发送单组数据，（使用线程发送）
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

    /**
    * 发送数据，对内（直接发送，会造成阻塞）
    * @Author Shanya
    * @Date 2020/7/12 19:01
    * @Version 1.0.0
    */
    private fun send(data: String) {
        var n = 0
        val outputStream = bluetoothSocket?.outputStream
        val bos = data.toByteArray()
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

    /**
    * 开关监听器
    * @Author Shanya
    * @Date 2020/7/13 21:52
    * @Version 1.0.0
    */
    val sendDataSwitchListener = View.OnClickListener {
        if (!switchOnFlag){
            (it as Button).text = switchOffText
            switchOnFlag = true
            switchOffFlag = false
            switchOnSendThread = SwitchOnSendThread()
            switchOnSendThread?.start()
        }else{
            (it as Button).text = switchOnText
            switchOnFlag = false
            switchOffFlag = true
            switchOffSendThread = SwitchOffSendThread()
            switchOffSendThread?.start()
        }
    }

    /**
    * 按键监听器
    * @Author Shanya
    * @Date 2020/7/12 20:21
    * @Version 1.0.0
    */
    val sendDataButtonListener = View.OnTouchListener { v, event ->

        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                if (!sendDownFlag) {
                    switchOnFlag = false
                    switchOffFlag = false
                    buttonDownSendThread = ButtonDownSendThread()
                    sendDownFlag = true
                    sendUpFlag = false
                    buttonDownSendThread?.start()
                }

                return@OnTouchListener false
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!sendUpFlag) {
                    switchOnFlag = false
                    switchOffFlag = false
                    sendDownFlag = false
                    sendUpFlag = true
                    sendData(" ")
                }

                return@OnTouchListener false
            }
            MotionEvent.ACTION_UP -> {
                if (!sendUpFlag) {
                    switchOnFlag = false
                    switchOffFlag = false
                    sendDownFlag = false
                    sendUpFlag = true
                    sendData(" ")
                }

                return@OnTouchListener false
            }
        }
        v.performClick()
        return@OnTouchListener false
    }

    /**
    * 按键按下发数据线程
    * @Author Shanya
    * @Date 2020/7/13 21:12
    * @Version 1.0.0
    */
    inner class ButtonDownSendThread: Thread(){
        override fun run() {
            super.run()
            while (sendDownFlag){
                sleep(100)
                send(sendDataDownString)
            }
        }
    }

    /**
    * 开关打开发数据线程
    * @Author Shanya
    * @Date 2020/7/13 21:40
    * @Version 1.0.0
    */
    inner class SwitchOnSendThread: Thread(){
        override fun run() {
            super.run()
            while (switchOnFlag){
                sleep(100)
                send(switchOnText)
            }
        }
    }

    /**
    * 开关关闭发数据线程
    * @Author Shanya
    * @Date 2020/7/13 22:13
    * @Version 1.0.0
    */
    inner class SwitchOffSendThread: Thread(){
        override fun run() {
            super.run()
            while (switchOffFlag){
                sleep(100)
                send(switchOffText)
            }
        }
    }




}
