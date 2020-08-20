package world.shanya.serialportutils

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

class PermissionActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION = 0x565;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        val permissionUtil = PermissionUtil.getInstance()
        Log.d("hello","Permission --> $permissionUtil")

        val builder = AlertDialog.Builder(this)
            .setTitle("权限申请")
            .setMessage("我们将要获取您的定位权限用于蓝牙搜索，否则蓝牙搜索功能将不可用。")
            .setPositiveButton("我已知晓"){_,_ ->
                ActivityCompat.requestPermissions(this,permissionUtil.permissions,REQUEST_PERMISSION)
            }
            .setOnCancelListener{
                ActivityCompat.requestPermissions(this,permissionUtil.permissions,REQUEST_PERMISSION)
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"已申请权限",Toast.LENGTH_SHORT).show()
                    finish()
                }else{
                    Toast.makeText(this,"申请权限失败",Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}