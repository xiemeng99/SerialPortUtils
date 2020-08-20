package world.shanya.serialportutils

open class SerialPortCallback {

    //扫描状态回调
    internal var scanStatusCallback : ((status:Boolean) -> Unit) ?= null

    //扫描状态的回调
    fun getScanStatus(scanStatusCallback: (status:Boolean) -> Unit){
        this.scanStatusCallback = scanStatusCallback
    }

    //设备连接成功回调
    protected var deviceConnectCallback: (() -> Unit) ?= null

    //设备连接成功回调函数
    internal fun deviceConnect(deviceConnectCallback: () -> Unit){
        this.deviceConnectCallback = deviceConnectCallback
    }

    //设备断开回调
    protected var deviceDisconnectCallback: (() -> Unit) ?= null

    //设备断开回调函数
    fun deviceDisconnect(deviceDisconnectCallback: () -> Unit){
        this.deviceDisconnectCallback = deviceDisconnectCallback
    }

    //接收消息回调
    protected var readDataCallback : ((data:String) -> Unit) ?= null

    //接收消息回调
    fun getReadData(readDataCallback: (result:String) -> Unit) {
        this.readDataCallback = readDataCallback
    }

    //发现新未配对设备回调
    protected var findUnPairedDeviceCallback : (() -> Unit) ?= null

    internal fun findUnPairedDevice(findDeviceCallback: () -> Unit) {
        this.findUnPairedDeviceCallback = findDeviceCallback
    }

    //发现以配对配对设备回调
    protected var findPairedDeviceCallback : (() -> Unit) ?= null

    internal fun findPairedDevice(findDeviceCallback: () -> Unit) {
        this.findUnPairedDeviceCallback = findDeviceCallback
    }

}