package world.shanya.serialportutil;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import world.shanya.serialportutils.SerialPort;

public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        SerialPort serialPort = SerialPort.Companion.getInstance(this);
        Button button7 = findViewById(R.id.button7);

        serialPort.setReadDataType(SerialPort.READ_DATA_TYPE_STRING);
        serialPort.setSendDataType(SerialPort.SEND_DATA_TYPE_STRING);

        serialPort.getReceivedData(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                System.out.println(s);
                return null;
            }
        });

        serialPort.sendData("sd");

        serialPort.getScanStatus(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                System.out.println(aBoolean);
                return null;
            }
        });

        serialPort.setButtonSendData(R.id.button,"button");

        ArrayAdapter<String> arrayAdapter = serialPort.getPairedDevicesArrayAdapter();
        ArrayAdapter<String> arrayAdapter1 = serialPort.getUnPairedDevicesArrayAdapter();

        serialPort.getConnectionResult(new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                System.out.println(aBoolean);
                return null;
            }
        });

        serialPort.setSwitchOnText(R.id.button6,"on");

        button7.setOnClickListener(serialPort.getSendSwitchListener());

        button7.setOnClickListener(v -> serialPort.openSearchPage(MainActivity2.this));
    }
}