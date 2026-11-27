package com.chargingscreen.pro
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
/** Перезапускает мониторинг после загрузки, если он был включён пользователем. */
class BootReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action==Intent.ACTION_BOOT_COMPLETED&&context.getSharedPreferences("charging_settings",0).getBoolean("monitoring",false))
   ContextCompat.startForegroundService(context,Intent(context,ChargingService::class.java))
 }
}