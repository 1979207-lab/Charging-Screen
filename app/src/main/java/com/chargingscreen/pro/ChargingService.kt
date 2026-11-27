package com.chargingscreen.pro
import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
class ChargingService:Service(){
 private lateinit var batteryReceiver:BatteryReceiver
 private var charging=false
 private var hidden=false
 private val handler=Handler(Looper.getMainLooper())
 private var closeTask:Runnable?=null
 override fun onCreate(){
  super.onCreate();createChannel();startForeground(101,makeNotification())
  batteryReceiver=BatteryReceiver{plugged,level,temp->handleBattery(plugged,level,temp)}
  val f=IntentFilter(Intent.ACTION_BATTERY_CHANGED)
  if(Build.VERSION.SDK_INT>=33)registerReceiver(batteryReceiver,f,Context.RECEIVER_NOT_EXPORTED) else registerReceiver(batteryReceiver,f)
 }
 private fun handleBattery(plugged:Boolean,level:Int,temp:Int){
  sendBroadcast(Intent(ACTION_UPDATE).setPackage(packageName).putExtra("charging",plugged).putExtra("level",level).putExtra("temperature",temp))
  if(plugged){
   closeTask?.let(handler::removeCallbacks);closeTask=null
   if(!charging){charging=true;if(!hidden)showCharging()}
  }else{
   charging=false;hidden=false
   closeTask?.let(handler::removeCallbacks)
   closeTask=Runnable{sendBroadcast(Intent(ACTION_CLOSE).setPackage(packageName))}
   handler.postDelayed(closeTask!!,3000)
  }
 }
 private fun showCharging(){
  hidden=false
  // Background activity launches are restricted on modern Android; notification remains the fallback.
  try{startActivity(Intent(this,ChargingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))}catch(_:Exception){}
  getSystemService(NotificationManager::class.java).notify(101,makeNotification())
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  when(intent?.action){
   ACTION_HIDE->hidden=true
   ACTION_SHOW->showCharging()
   ACTION_STOP->{getSharedPreferences("charging_settings",0).edit().putBoolean("monitoring",false).apply();stopSelf()}
  }
  return START_STICKY
 }
 private fun createChannel(){
  if(Build.VERSION.SDK_INT>=26){
   val ch=NotificationChannel(CHANNEL,"Мониторинг зарядки",NotificationManager.IMPORTANCE_HIGH)
   ch.lockscreenVisibility=Notification.VISIBILITY_PUBLIC
   getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
  }
 }
 private fun makeNotification():Notification{
  val open=PendingIntent.getActivity(this,1,Intent(this,ChargingActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val stop=PendingIntent.getService(this,2,Intent(this,ChargingService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_charging)
   .setContentTitle("Charging Screen PRO").setContentText("Мониторинг включён · нажмите для открытия")
   .setContentIntent(open).setOngoing(true).addAction(0,"Остановить",stop).build()
 }
 override fun onDestroy(){closeTask?.let(handler::removeCallbacks);try{unregisterReceiver(batteryReceiver)}catch(_:Exception){};super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
 companion object{
  const val CHANNEL="charging_screen";const val ACTION_UPDATE="com.chargingscreen.pro.UPDATE"
  const val ACTION_CLOSE="com.chargingscreen.pro.CLOSE";const val ACTION_HIDE="com.chargingscreen.pro.HIDE"
  const val ACTION_SHOW="com.chargingscreen.pro.SHOW";const val ACTION_STOP="com.chargingscreen.pro.STOP"
 }
}