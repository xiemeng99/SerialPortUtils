package world.shanya.serialportutil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import world.shanya.serialportutils.SearchActivity
import world.shanya.serialportutils.SerialPort

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serialPort = SerialPort.getInstance(this)

        val serialPort1 = SerialPort.getInstance(this)

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