package world.shanya.serialportutils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.text.method.ReplacementTransformationMethod
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets

class SerialPort private constructor(private val context: Context) : SerialPortCallback() {

    val pairedDevicesList = ArrayList<Device>()
    val unPairedDevicesList = ArrayList<Device>()

    private var connectedDevice : Device ?= null

    var readDataType = DataType.READ_STRING
    var sendDataType = DataType.SEND_STRING

    private var connectStatus = false

    fun getConnectStatus():Boolean{
        return connectStatus
    }

    //断开连接
    fun disconnect(){
        readThreadStarted = false
        bluetoothSocket?.close()
        bluetoothSocket = null
        deviceDisconnectCallback?.invoke()
        connectStatus = false
    }

    //打开搜索页面
    fun openSearchPage(context: Context){
        context.startActivity(Intent(context,SearchActivity::class.java))
    }

    //搜索设备
    fun doDiscovery(context: Context) :Boolean{

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            val permissionUtil = PermissionUtil.getInstance()
            permissionUtil.getPermissions(context)

            return false

        }else{
            if (!bluetoothAdapter.isEnabled){
                bluetoothAdapter.enable()
                return false
            }

            //如果当前正在搜索则先取消搜索重新打开
            if (bluetoothAdapter.isDiscovering){
                bluetoothAdapter.cancelDiscovery()
            }

            //清空未配对列表方便，重新搜索加入
            unPairedDevicesList.clear()

            //开始搜索设备
            bluetoothAdapter.startDiscovery()

            //设置搜索状态为 true
            scanStatusCallback?.invoke(true)

            return true
        }
    }

    //十六进制输入框监听
    fun editTextHexLimit(editText: EditText){
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.keyListener = DigitsKeyListener.getInstance("abcdefABCDEF0123456789")
        editText.transformationMethod = object : ReplacementTransformationMethod() {
            override fun getOriginal(): CharArray {
                return charArrayOf('a','b','c','d','e','f')
            }

            override fun getReplacement(): CharArray {
                return charArrayOf('A','B','C','D','E','F')
            }

        }
        editText.addTextChangedListener(object :TextWatcher{
            override fun afterTextChanged(p0: Editable?) {

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 1){
                    if ((s!!.length + 1) % 3 == 0){
                        editText.setText("$s ")
                        editText.setSelection(s.length + 1)
                    }
                }else if (count == 0){
                    if (s!!.isNotEmpty() && s.length % 3 == 0){
                        editText.setText(s.subSequence(0,s.length - 1))
                        editText.setSelection(s.length - 1)
                    }
                }
            }

        })
    }

    //发送数据函数
    fun sendData(data:String){
        if (!bluetoothAdapter.isEnabled){
            bluetoothAdapter.enable()
            return
        }
        if (bluetoothSocket == null) {
            Toast.makeText(context,context.getString(R.string.connect_device),Toast.LENGTH_SHORT).show()
            return
        }

        Thread{
            send(data)
        }.start()
    }

    //连接设备
    internal fun connectDevice(device: Device){

        connectedDevice = device

        Thread{
            val address = device.address
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            connectedDevice = device
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))
                bluetoothSocket?.connect()

                MainScope().launch {
                    connectStatus = true
                    deviceConnectCallback?.invoke()
                    Toast.makeText(
                        context,
                        "${bluetoothDevice.name}  ${context.getString(R.string.connect_success)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                inputStream = bluetoothSocket?.inputStream
                if (!readThreadStarted){
                    readThread.start()
                    readThreadStarted = true
                }

            }catch (e:IOException){
                //设置连接状态为 false ，并弹出连接失败的 Toast 提示

                MainScope().launch {
                    connectStatus = false
                    deviceDisconnectCallback?.invoke()
                    Toast.makeText(context,context.getString(R.string.connection_failed),Toast.LENGTH_SHORT).show()
                }
                //关闭蓝牙 Socket
                try {
                    bluetoothSocket?.close()
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }.start()

    }

    //广播接收器
    private val bluetoothReceiver by lazy {
        object :BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action){
                    //找到设备
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null){
                            if (device.bondState != BluetoothDevice.BOND_BONDED){
                                if (device.name == null){
                                    unPairedDevicesList.add(Device(" ",device.address))
                                    findUnPairedDeviceCallback?.invoke()
                                }else{
                                    unPairedDevicesList.add(Device(device.name,device.address))
                                    findUnPairedDeviceCallback?.invoke()
                                }
                            }
                        }
                    }

                    //搜索完成
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanStatusCallback?.invoke(false)

                    }

                    //设备断开
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        bluetoothSocket = null
                        connectStatus = false
                        deviceDisconnectCallback?.invoke()
                        Toast.makeText(context,"${device?.name}  ${context?.getString(R.string.disconnect)}",
                            Toast.LENGTH_SHORT).show()
                    }
                }

            }

        }
    }

    //接收数据的线程
    private val readThread by lazy {
        object :Thread(){
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun run() {
                super.run()
                var len: Int
                var receivedData:String
                var buffer = ByteArray(0)
                var flag = false
                while (readThreadStarted){
                    len = inputStream?.available()!!
                    while (len != 0){
                        flag = true
                        buffer = ByteArray(len)
                        inputStream?.read(buffer)
                        sleep(10)
                        len = inputStream?.available()!!
                    }
                    /**
                     * 取出读到的数据，并根据所选数据类型进行处理并保存
                     */
                    if (flag){
                        if (readDataType == DataType.READ_STRING){
                            receivedData = String(buffer, StandardCharsets.UTF_8)
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
    }

    //字符串转成16进制
    private fun stringToHex(str: String): ArrayList<Byte>? {
        val chars = "0123456789ABCDEF".toCharArray()
        val stingTemp = str.replace(" ","")
        val bs = stingTemp.toCharArray()
        var bit = 0
        var i = 0
        val intArray = ArrayList<Byte>()
        if (stingTemp.length and 0x01 != 0){
            MainScope().launch {
                Toast.makeText(context,"请输入的十六进制数据保持两位，不足前面补0",Toast.LENGTH_SHORT).show()
            }
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

    //发送数据
    private fun send(data : String){
        try {
            val outputStream = bluetoothSocket?.outputStream
            var n = 0
            val bos: ByteArray = if (sendDataType == DataType.SEND_STRING) {
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
        }catch (e:Exception){

        }
    }



    //初始化
    init {
        context.registerReceiver(bluetoothReceiver,IntentFilter(BluetoothDevice.ACTION_FOUND))
        context.registerReceiver(bluetoothReceiver,IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        context.registerReceiver(bluetoothReceiver,IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        context.registerReceiver(bluetoothReceiver,IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))


        val pairedDevices:Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()){
            for (device in pairedDevices){
                pairedDevicesList.add(Device(device.name,device.address))
            }
        }
    }

    //取消搜索
    internal fun cancelDiscover(){
        bluetoothAdapter.cancelDiscovery()
        scanStatusCallback?.invoke(false)
    }

    companion object{

        //SPP服务UUID号
        private const val UUID = "00001101-0000-1000-8000-00805F9B34FB"
        //蓝牙设配器
        private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        //蓝牙Socket
        private var bluetoothSocket:BluetoothSocket ?= null
        //接收用InputStream
        private var inputStream: InputStream?= null
        //接收线程标志
        private var readThreadStarted = false



        //获取SerialPort实例，SingleTon
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

    enum class DataType {
        SEND_HEX,SEND_STRING,READ_HEX,READ_STRING
    }
}