package world.shanya.serialportutil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import world.shanya.serialportutils.PermissionActivity
import world.shanya.serialportutils.SerialPort
import world.shanya.serialportutils.SerialPortOld


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        button.setOnClickListener {
            serialPort.openSearchPage(this)
        }

        button2.setOnClickListener {
            serialPort.disconnect()
        }

        serialPort.getReadData {
            println(it)
        }


    }
}