package world.shanya.serialportutils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.device_cell.view.*

/**
 * 扫描界面的Activity
 */
class SearchActivity : AppCompatActivity() {

    /**
     * SerialPort实例
     */
    private lateinit var serialPort: SerialPort

    /**
     * 连接时显示的进度对话框
     */
    private lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)


        serialPort = SerialPort.getInstance(this)
        dialog = Dialog(this@SearchActivity)
        dialog.setContentView(R.layout.progress_dialog_layout)
        dialog.setCancelable(false)

    }

    override fun onResume() {
        super.onResume()

        if (serialPort.doDiscover()) {
            swipeRedreshLayout.isRefreshing = true
            title = getString(R.string.searching)
        } else {
            swipeRedreshLayout.isRefreshing = false
        }

        serialPort._getScanStatus {
            if (it) {
                swipeRedreshLayout.isRefreshing = true
            } else {
                swipeRedreshLayout.isRefreshing = false
                title = getString(R.string.select_device_to_connect)
            }
        }

        swipeRedreshLayout.setOnRefreshListener {
            serialPort.doDiscover()
            title = getString(R.string.searching)
        }

        serialPort._getConnectedStatus {
            finish()
        }

        val pairedDevicesAdapter = PairedDevicesAdapter(this)
        pairedDeviceRecyclerView.apply {
            adapter = pairedDevicesAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
            addItemDecoration(DividerItemDecoration(this@SearchActivity,DividerItemDecoration.VERTICAL))
        }


        val unPairedDevicesAdapter = UnPairedDevicesAdapter(this)
        unPairedDeviceRecyclerView.apply {
            adapter = unPairedDevicesAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
            addItemDecoration(DividerItemDecoration(this@SearchActivity,DividerItemDecoration.VERTICAL))
        }

        serialPort.findUnPairedDevice {
            unPairedDevicesAdapter.setUnPairedDevice(serialPort.unPairedDevicesList)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serialPort.cancelDiscover()
        dialog.dismiss()
    }

    inner class PairedDevicesAdapter internal constructor(context: Context): RecyclerView.Adapter<PairedDevicesAdapter.PairedDevicesViewHolder>(){

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private var pairedDevices = ArrayList<Device>()

        inner class PairedDevicesViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
            val textViewDeviceName: TextView = itemView.findViewById(R.id.textViewDeviceName)
            val textViewDeviceAddress: TextView = itemView.findViewById(R.id.textViewDeviceAddress)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PairedDevicesViewHolder {
            val holder = PairedDevicesViewHolder(inflater.inflate(R.layout.device_cell,parent,false))

            holder.itemView.setOnClickListener {
                dialog.show()
                serialPort.cancelDiscover()
                serialPort.connectDevice(
                    Device(
                        it.textViewDeviceName.text.toString(),
                        it.textViewDeviceAddress.text.toString()
                    )
                )
            }

            return holder
        }

        override fun getItemCount(): Int {
            return serialPort.pairedDevicesList.size
        }

        override fun onBindViewHolder(holder: PairedDevicesViewHolder, position: Int) {
            val current = serialPort.pairedDevicesList[position]
            holder.textViewDeviceName.text = current.name
            holder.textViewDeviceAddress.text = current.address
        }

        internal fun setPairedDevice(pairedDevices: ArrayList<Device>){
            this.pairedDevices = pairedDevices
            notifyDataSetChanged()
        }
    }

    inner class UnPairedDevicesAdapter internal constructor(context: Context):RecyclerView.Adapter<UnPairedDevicesAdapter.UnPairedDevicesViewHolder>(){

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private var unPairedDevices = ArrayList<Device>()
        inner class UnPairedDevicesViewHolder(itemView:View) :RecyclerView.ViewHolder(itemView){
            val textViewDeviceName: TextView = itemView.findViewById(R.id.textViewDeviceName)
            val textViewDeviceAddress: TextView = itemView.findViewById(R.id.textViewDeviceAddress)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): UnPairedDevicesViewHolder {
            val holder = UnPairedDevicesViewHolder(inflater.inflate(R.layout.device_cell,parent,false))

            holder.itemView.setOnClickListener {
                dialog.show()
                serialPort.cancelDiscover()
                serialPort.connectDevice(
                    Device(
                        it.textViewDeviceName.text.toString(),
                        it.textViewDeviceAddress.text.toString()
                    )
                )
            }
            return holder
        }

        override fun getItemCount(): Int {
            return unPairedDevices.size
        }

        override fun onBindViewHolder(holder: UnPairedDevicesViewHolder, position: Int) {
            val current = unPairedDevices[position]
            holder.textViewDeviceName.text = current.name
            holder.textViewDeviceAddress.text = current.address
        }

        internal fun setUnPairedDevice(unPairedDevices: ArrayList<Device>){
            this.unPairedDevices = unPairedDevices
            notifyDataSetChanged()
        }
    }

}