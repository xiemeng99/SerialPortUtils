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
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets


/**
 * @ClassName SerialPort
 * @Description 蓝牙串口工具类，集成搜索页面Activity，按键监听器，开关型按键监听器，支持字符或16进制的发送与接收
 * @Author Shanya
 * @Date 2020/7/11 20:33
 * @Version 1.0.0
 **/
class SerialPort private constructor(private val context: Context){

    /**
     * 懒汉式SingleTon
     */
    companion object {

        /**
         * 以16进制形式发送数据
         */
        const val SEND_DATA_TYPE_HEX = 0x11

        /**
         * 以字符形式发送数据
         */
        const val SEND_DATA_TYPE_STRING = 0x12

        /**
         * 以16进制形式接收数据
         */
        const val READ_DATA_TYPE_HEX = 0x13

        /**
         * 以字符形式接收数据
         */
        const val READ_DATA_TYPE_STRING = 0x14

        /**
         * 获取SerialPort实例，SingleTon
         */
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

    /**
     * 扫描状态回调
     */
    private var scanStatusCallback : ((status:Boolean) -> Unit) ?= null

    /**
     * 连接状态回调
     */
    private var connectionCallback : ((result:Boolean) -> Unit) ?= null

    /**
     * 接收信息回调
     */
    private var readDataCallback : ((data:String) -> Unit) ?= null

    /**
     * SPP服务UUID号
     */
    private val uuid = "00001101-0000-1000-8000-00805F9B34FB"

    /**
     * 获取蓝牙设配器
     */
    private val bluetoothAdapter:BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    /**
     * 蓝牙接收发送的Socket
     */
    private var bluetoothSocket:BluetoothSocket ?= null

    /**
     * 蓝牙接收inputStream
     */
    private var inputStream: InputStream ?= null

    /**
     * 已配对设备的列表
     */
    val pairedDevicesArrayAdapter:ArrayAdapter<String> = ArrayAdapter(context,R.layout.device_name)

    /**
     * 未配对设备列表
     */
    val unPairedDevicesArrayAdapter:ArrayAdapter<String> = ArrayAdapter(context,R.layout.device_name)

    /**
     * 接收数据类型
     */
    var readDataType = READ_DATA_TYPE_STRING

    /**
     * 发送数据类型
     */
    var sendDataType = SEND_DATA_TYPE_STRING

    /**
     * 接收线程打开标志
     */
    private var readThreadStarted = false

    /**
     * 开关发送标志
     */
    var switchSendFlag = false

    /**
     * 开关运行数量
     */
    private var switchSendTrueCount = 0

    /**
     * 开关发送线程发送数据缓存
     */
    private val switchSendStringBuilder = StringBuilder()

    /**
     * 开关发送线程发送数据
     */
    var switchSendData = ""

    /**
     * 开关发送线程
     */
    private var switchSendThread = SwitchSendThread()

    /**
     * 开关发送内容的存储 HashMap
     */
    private val switchSendDataHashMap by lazy { HashMap<Int,String>() }

    /**
     * 开关按键打开时显示内容
     */
    val switchOnTextHashMap by lazy { HashMap<Int,String>() }

    /**
     * 开关按键关闭时显示内容
     */
    val switchOffTextHashMap by lazy { HashMap<Int,String>() }

    /**
     * 开关按键状态
     */
    private val switchStatusHashMap by lazy { HashMap<Int,Boolean>() }

    /**
     * 按键发送线程标志位
     */
    private var buttonSendFlag = false

    /**
     * 按键发送线程发送的数据缓存
     */
    private var buttonSendData = ""

    /**
     * 按键发送线程
     */
    private var buttonSendThread = ButtonSendThread()

    /**
     * 按键发送内容的存储 HashMap
     */
    private val buttonSendDataHashMap by lazy { HashMap<Int,String>() }

    /**
    * 蓝牙设配器各种状态的广播监听器
    * @Author Shanya
    * @Date 2020/7/12 16:33
    * @Version 1.0.0
    */
    private val searchReceiver by lazy {
        object :BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (BluetoothDevice.ACTION_FOUND == action){ //找到设备
                    /**
                     * 获取设备
                     */
                    val device:BluetoothDevice ?= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null){
                        /**
                         * 如果设备未配对则将其加入未配对设备列表
                         */
                        if (device.bondState != BluetoothDevice.BOND_BONDED) {
                            unPairedDevicesArrayAdapter.add("${device.name}\n${device.address}")
                        }
                    }
                }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action){ //搜索完成
                    /**
                     * 设置搜索状态为 false
                     */
                    scanStatusCallback?.invoke(false)
                    /**
                     * 没有未配对的设备，则在未配对列表加入提示信息
                     */
                    if (unPairedDevicesArrayAdapter.count == 0){
                        unPairedDevicesArrayAdapter.add(context?.getString(R.string.no_available))
                    }
                }else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action){ //设备断开连接
                    /**
                     * 获取设备信息
                     */
                    val device:BluetoothDevice ?= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    /**
                     * 清空蓝牙Socket
                     */
                    bluetoothSocket = null
                    /**
                     * 设置连接状态为 false
                     */
                    connectionCallback?.invoke(false)
                    /**
                     * 弹出Toast提示
                     */
                    Toast.makeText(context,"${device?.name}  ${context?.getString(R.string.disconnect)}",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    init {
        /**
         * 注册找到设备广播
         */
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothDevice.ACTION_FOUND))

        /**
         * 注册搜索完成广播
         */
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        /**
         * 注册搜索开始广播
         */
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))

        /**
         * 注册设备断开连接广播
         */
        context.registerReceiver(searchReceiver,IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))

        /**
         * 获取已配对设备列表，若没有则在已配对设备列表加入提示信息
         */
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
    * 打开搜索页面，弹出SearchActivity（默认 AlertDialog 形式）
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
        /**
         * 检查权限
         */
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            /**
             * 没有权限则弹出 Toast 提示
             */
            Toast.makeText(context,context.getString(R.string.no_permission),Toast.LENGTH_SHORT).show()
        }else{
            /**
             * 蓝牙没打开则弹出 Toast 提示
             */
            if (!bluetoothAdapter.isEnabled){
                Toast.makeText(context,context.getString(R.string.open_bluetooth),Toast.LENGTH_SHORT).show()
                return
            }
            /**
             * 如果当前正在搜索则先取消搜索重新打开
             */
            if (bluetoothAdapter.isDiscovering){
                bluetoothAdapter.cancelDiscovery()
                scanStatusCallback?.invoke(false)
            }
            /**
             * 清空未配对列表方便，重新搜索加入
             */
            unPairedDevicesArrayAdapter.clear()
            /**
             * 开始搜索设备
             */
            bluetoothAdapter.startDiscovery()
            /**
             * 设置搜索状态为 true
             */
            scanStatusCallback?.invoke(true)
        }
    }

    /**
    * 搜索列表项的先择监听
    * @Author Shanya
    * @Date 2020/7/12 16:31
    * @Version 1.0.0
    */
    val devicesClickListener by lazy {
        AdapterView.OnItemClickListener { _, view, _, _ ->
            /**
             * 如蓝牙未打开则弹出 Toast 提示
             */
            if (!bluetoothAdapter.isEnabled){
                Toast.makeText(context,context.getString(R.string.open_bluetooth),Toast.LENGTH_SHORT).show()
                return@OnItemClickListener
            }
            /**
             * 获取点击项的信息
             */
            val info = (view as TextView).text.toString()
            if (info == context.getString(R.string.no_paired) || info == context.getString(R.string.no_available)) {
                return@OnItemClickListener
            }
            /**
             * 取消搜索
             */
            bluetoothAdapter.cancelDiscovery()
            /**
             * 开始连接
             */
            val address = info.substring(info.length - 17)
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            try {
                /**
                 * 建立 Socket 通信
                 */
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString(uuid))
                /**
                 * 连接 Socket
                 */
                bluetoothSocket?.connect()
                /**
                 * 设置连接状态为 false
                 */
                connectionCallback?.invoke(true)
                /**
                 * 弹出连接成功的 Toast 提示
                 */
                Toast.makeText(
                    context,
                    "${bluetoothDevice.name}  ${context.getString(R.string.connect_success)}",
                    Toast.LENGTH_SHORT
                ).show()
                /**
                 * 获取接收 inoutStream
                 */
                inputStream = bluetoothSocket?.inputStream
                /**
                 * 打开接收线程
                 */
                if (!readThreadStarted){
                    readThread.start()
                    readThreadStarted = true
                }
            }catch (e:IOException){ //连接过程的异常处理
                /**
                 * 设置连接状态为 false ，并弹出连接失败的 Toast 提示
                 */
                connectionCallback?.invoke(false)
                Toast.makeText(context,context.getString(R.string.connection_failed),Toast.LENGTH_SHORT).show()
                /**
                 * 关闭蓝牙 Socket
                 */
                try {
                    bluetoothSocket?.close()
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }
    }


    /**
    * 扫描状态的回调
    * @Author Shanya
    * @Date 2020/7/19 22:02
    * @Version 1.0.0
    */
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
    private val readThread by lazy {
        object :Thread(){
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun run() {
                super.run()
                var len: Int
                var receivedData:String
                var buffer = ByteArray(0)
                var flag = false
                while (true){
                    /**
                     * 线程打开判断
                     */
                    if (!readThreadStarted) {
                        return
                    }
                    sleep(100)
                    /**
                     * 获取数据
                     */
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
    }

    /**
    * 发送数据，（使用线程发送）
    * @Author Shanya
    * @Date 2020/7/12 18:46
    * @Version 1.0.0
    */
    fun sendData(data:String){
        /**
         * 若蓝牙未打开则取消发送，并弹出 Toast 提示
         */
        if (!bluetoothAdapter.isEnabled){
            Toast.makeText(context,context.getString(R.string.open_bluetooth),Toast.LENGTH_SHORT).show()
            return
        }
        /**
         * 若设备未连接则取消发送，并弹出 Toast 提示
         */
        if (bluetoothSocket == null) {
            Toast.makeText(context,context.getString(R.string.connect_device),Toast.LENGTH_SHORT).show()
            return
        }

        /**
         * 执行发送线程
         */
        Thread(Runnable {
            send(data)
        }).start()
    }

    /**
    * 字符串转成16进制
    * @Author Shanya
    * @Date 2020/7/19 21:34
    * @Version 1.0.0
    */
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
    * 发送数据，内部使用（直接发送，会造成线程阻塞）
    * @Author Shanya
    * @Date 2020/7/12 19:01
    * @Version 1.0.0
    */
    private fun send(data: String) {
        /**
         * 获取发送 OutputStream
         */
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



    /**
     * 根据按键ID设置每个按键按下对应发送的内容
     * @Author Shanya
     * @Date 2020/7/19 22:46
     * @Version 1.0.0
     */
    fun setButtonSendData(id:Int,data:String){
        buttonSendDataHashMap[id] = data
    }

    /**
    * 发送按键监听器，按住发送，松开取消
    * @Author Shanya
    * @Date 2020/7/19 22:11
    * @Version 1.0.0
    */
    val sendButtonListener by lazy {

         View.OnTouchListener { v, event ->
             /**
              * 若蓝牙未打开则取消发送，并弹出 Toast 提示
              */
             if (!bluetoothAdapter.isEnabled) {
                 Toast.makeText(
                     context,
                     context.getString(R.string.open_bluetooth),
                     Toast.LENGTH_SHORT
                 ).show()
                 return@OnTouchListener false
             }
             /**
              * 若设备未连接则取消发送，并弹出 Toast 提示
              */
             if (bluetoothSocket == null) {
                 Toast.makeText(
                     context,
                     context.getString(R.string.connect_device),
                     Toast.LENGTH_SHORT
                 ).show()
                 return@OnTouchListener false
             }

             var templateData = ""

             /**
              * 获取对应按键按下发出的数据
              */
             if (!buttonSendDataHashMap.contains(v.id)) {
                 buttonSendData = ""
             }else{
                 templateData = if (buttonSendDataHashMap[v.id] != null) {
                     buttonSendDataHashMap[v.id]!!
                 }else{
                     ""
                 }
             }

             /**
              * 根据当前按键的状态进行相应处理
              */
             when (event.action) {
                 /**
                  * 按键按下滑动后抬起
                  */
                 MotionEvent.ACTION_UP -> {
                     buttonSendData = "0"
                     Thread.sleep(10)
                     buttonSendFlag = false
                     return@OnTouchListener false
                 }

                 /**
                  * 按键取消按下
                  */
                 MotionEvent.ACTION_CANCEL -> {
                     buttonSendData = "0"
                     Thread.sleep(10)
                     buttonSendFlag = false
                     return@OnTouchListener false
                 }

                 /**
                  * 按键按下
                  */
                 MotionEvent.ACTION_DOWN -> {
                     buttonSendThread = ButtonSendThread()
                     buttonSendData = templateData
                     buttonSendFlag = true
                     buttonSendThread.start()
                     return@OnTouchListener false
                 }
             }

             v.performClick()
             return@OnTouchListener false
         }
    }

    /**
    * 按键用的发送线程，按住发送，松开取消
    * @Author Shanya
    * @Date 2020/7/19 22:23
    * @Version 1.0.0
    */
    private inner class ButtonSendThread:Thread(){
        override fun run() {
            super.run()
            while (buttonSendFlag){
                sleep(10)
                sendData(buttonSendData)
            }
        }

    }



    /**
     * 根据开关按键的ID设置每个按键按下对应发送的内容
     * @Author Shanya
     * @Date 2020/7/19 22:46
     * @Version 1.0.0
     */
    fun setSwitchSendData(id:Int,data:String){
        switchSendDataHashMap[id] = data
    }

    val sendSwitchListener by lazy {
        View.OnClickListener {

            /**
             * 若蓝牙未打开则取消发送，并弹出 Toast 提示
             */
            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(
                    context,
                    context.getString(R.string.open_bluetooth),
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }
            /**
             * 若设备未连接则取消发送，并弹出 Toast 提示
             */
            if (bluetoothSocket == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.connect_device),
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }

            /**
             * 获取对应开关状态,并将开关上的显示字符更新
             */
            if (!switchStatusHashMap.contains(it.id)) {
                switchStatusHashMap[it.id] = true
                (it as Button).text = switchOnTextHashMap[it.id]
            }else{
                if (switchStatusHashMap[it.id] != null) {
                    if (switchStatusHashMap[it.id]!!){
                        switchStatusHashMap[it.id] = false
                        (it as Button).text = switchOffTextHashMap[it.id]
                    }else{
                        switchStatusHashMap[it.id] = true
                        (it as Button).text = switchOnTextHashMap[it.id]
                    }
                }else{
                    switchStatusHashMap[it.id] = false
                }
            }

            /**
             * 获取对应开关打开发出的数据
             */
            switchSendStringBuilder.clear()
            switchSendTrueCount = 0
            for (data in switchStatusHashMap) {
                if (data.value){
                    switchSendTrueCount++
                    switchSendStringBuilder.append(switchSendDataHashMap[data.key])
                }
            }

            /**
             * 判断当前开关发送线程的状态
             */
            switchSendFlag = if (switchSendTrueCount == 0){
                switchSendStringBuilder.clear()
                switchSendStringBuilder.append("0")
                switchSendData = switchSendStringBuilder.toString()
                Thread.sleep(10)
                false
            }else{
                switchSendThread = SwitchSendThread()
                switchSendData = switchSendStringBuilder.toString()
                switchSendThread.start()
                true
            }

        }
    }

    /**
    * 按键用的发送线程，点击开启发送，再次点击取消
    * @Author Shanya
    * @Date 2020/7/20 20:26
    * @Version 1.0.0
    */
    private inner class SwitchSendThread:Thread(){
        override fun run() {
            super.run()
            while (switchSendFlag) {
                sleep(10)
                sendData(switchSendData)
            }
        }
    }

}

