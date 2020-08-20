package world.shanya.serialportutils

import android.Manifest
import android.content.Context
import android.content.Intent

internal class PermissionUtil private constructor() {

    companion object{
        private var instance: PermissionUtil ?= null
        fun getInstance(): PermissionUtil{
            val temp = instance
            if (temp != null){
                return temp
            }
            return synchronized(this){
                val temp2 = instance
                if (temp2 != null){
                    temp2
                }else{
                    val created = PermissionUtil()
                    instance = created
                    created
                }
            }
        }
    }

    var permissions :Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    internal fun getPermissions(context: Context){
        context.startActivity(Intent(context,PermissionActivity::class.java))
    }
}