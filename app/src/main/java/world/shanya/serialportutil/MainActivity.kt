package world.shanya.serialportutil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import world.shanya.serialportutils.SearchActivity
import world.shanya.serialportutils.SerialPort

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        serialPort.dataType = SerialPort.DATA_HEX

        serialPort.getReadData(object :SerialPort.ReadDataCallback{
            override fun readData(data: String) {
                MainScope().launch {

                    println(data)

                }
            }
        })

        button.setOnClickListener {
            serialPort.sendData("asd\r\n")
        }

        button2.setOnClickListener {
            serialPort.openSearchPage(this)
        }
    }
}