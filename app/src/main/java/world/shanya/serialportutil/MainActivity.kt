package world.shanya.serialportutil

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_main.*
import world.shanya.serialportutils.SerialPort

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        serialPort.readDataType = SerialPort.READ_DATA_TYPE_STRING

        serialPort.getReceivedData{it
            println(it)
        }

        serialPort.sendDataType = SerialPort.SEND_DATA_TYPE_STRING

        button.setOnClickListener {
            serialPort.sendData("Hello")
        }

        button2.setOnClickListener {
            serialPort.openSearchPage(this)
        }

        serialPort.getScanStatus {

        }

        val arrayAdapter = serialPort.pairedDevicesArrayAdapter
        val arrayAdapter2 = serialPort.unPairedDevicesArrayAdapter

        serialPort.getConnectionResult {

        }

        serialPort.setButtonSendData(button3.id,"3")
        serialPort.setButtonSendData(button4.id,"4")

        button3.setOnTouchListener(serialPort.sendButtonListener)

        button4.setOnTouchListener(serialPort.sendButtonListener)

        serialPort.setSwitchSendData(button5.id,"5")
        serialPort.setSwitchSendData(button6.id,"6")

        serialPort.setSwitchOnText(button5.id,"on")
        serialPort.setSwitchOnText(button6.id,"on")

        serialPort.setSwitchOffText(button5.id,"off")
        serialPort.setSwitchOffText(button6.id,"off")

        button5.setOnClickListener(serialPort.sendSwitchListener)
        button6.setOnClickListener(serialPort.sendSwitchListener)

        val saad = object :TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                val ss = s.toString()
                println(ss)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        }
    }
}