package com.chargingscreen.pro
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
/** Читает состояние батареи из sticky broadcast. */
class BatteryReceiver(private val callback:(Boolean,Int,Int)->Unit):BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val scale=intent.getIntExtra("scale",100).coerceAtLeast(1)
  val level=(intent.getIntExtra("level",0)*100/scale).coerceIn(0,100)
  val status=intent.getIntExtra("status",-1)
  val plugged=intent.getIntExtra("plugged",0)
  callback(plugged!=0&&(status==2||status==5),level,intent.getIntExtra("temperature",-1))
 }
}