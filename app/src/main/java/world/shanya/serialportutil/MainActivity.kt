package world.shanya.serialportutil

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import world.shanya.serialportutils.SerialPort

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        serialPort.readDataType = SerialPort.READ_DATA_TYPE_STRING

        serialPort.getReceivedData{
            println(it)
        }

        serialPort.sendDataType = SerialPort.SEND_DATA_TYPE_STRING

        button.setOnClickListener {
            serialPort.sendData("Hello")
        }

        button2.setOnClickListener {
            serialPort.openSearchPage(this)
        }

        serialPort.setButtonSendData(button3.id,"3")
        serialPort.setButtonSendData(button4.id,"4")

        button3.setOnTouchListener(serialPort.sendButtonListener)

        button4.setOnTouchListener(serialPort.sendButtonListener)

    }
}