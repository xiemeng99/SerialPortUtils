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

        val serialPort1 = SerialPort.getInstance(this)

        serialPort.getReadData(object :SerialPort.ReadDataCallback{
            override fun readData(data: String) {
                MainScope().launch {
                    Toast.makeText(this@MainActivity,data,Toast.LENGTH_SHORT).show()
                }
            }
        })

        button.setOnClickListener {
            println(serialPort)
            println(serialPort1)
        }

        button2.setOnClickListener {
//            startActivity(Intent(this,SearchActivity::class.java))
            serialPort.openSearchPage(this)
        }
    }
}