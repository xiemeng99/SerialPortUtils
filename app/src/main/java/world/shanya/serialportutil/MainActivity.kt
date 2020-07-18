package world.shanya.serialportutil

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import world.shanya.serialportutils.SerialPort

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        serialPort.readDataType = SerialPort.READ_DATA_TYPE_HEX

        serialPort.getReceivedData(object :SerialPort.ReadDataCallback{
            override fun readData(data: String) {
                println(data)

            }


        })

        serialPort.sendDataType = SerialPort.SEND_DATA_TYPE_HEX

        button.setOnClickListener {
            serialPort.sendData("A2DFD")
        }

        button2.setOnClickListener {
            serialPort.openSearchPage(this)
        }


    }
}