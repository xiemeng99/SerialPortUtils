package world.shanya.serialportutil

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import world.shanya.serialportutils.SerialPort


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        button.setOnClickListener {
            serialPort.openSearchPage(this)
        }

        serialPort.editTextHexLimit(editTextTextPersonName2)

        serialPort.readDataType = SerialPort.DataType.READ_HEX
        serialPort.sendDataType = SerialPort.DataType.SEND_HEX

        button2.setOnClickListener {
            serialPort.sendData(editTextTextPersonName2.text.toString())
        }

        serialPort.getReadData {
            println(it)
        }


    }
}