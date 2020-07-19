package world.shanya.serialportutils

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val serialPort = SerialPort.getInstance(this)

        serialPort.getScanStatus{
                if (it){
                    MainScope().launch {
                        progressBarScan.visibility = View.VISIBLE
                    }
                }else{
                    MainScope().launch {
                        progressBarScan.visibility = View.GONE
                    }
                }
            }

        serialPort.getConnectionResult(){
            finish()
        }

        listViewPairedDevices.adapter = serialPort.pairedDevicesArrayAdapter
        listViewUnpairedDevices.adapter = serialPort.unPairedDevicesArrayAdapter
        listViewPairedDevices.onItemClickListener = serialPort.devicesClickListener
        listViewUnpairedDevices.onItemClickListener = serialPort.devicesClickListener
        buttonScan.setOnClickListener {
            serialPort.doDiscovery(this)
        }
    }
}