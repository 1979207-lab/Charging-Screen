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
   handler.postDelayed(closeTask!!,1500)
  }
 }
 private fun showCharging(){
  hidden=false
  // Короткое пробуждение дисплея; экран зарядки использует showWhenLocked/turnScreenOn.
  try {
   val pm=getSystemService(POWER_SERVICE) as PowerManager
   @Suppress("DEPRECATION")
   val wakeLock=pm.newWakeLock(
    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
    "ChargingScreenPRO:PowerConnected"
   )
   wakeLock.acquire(8000L)
   startActivity(Intent(this,ChargingActivity::class.java).addFlags(
    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
   ))
  } catch (_:Exception) { }
  getSystemService(NotificationManager::class.java).notify(101,makeNotification(true))
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  when(intent?.action){
   ACTION_POWER_CONNECTED->{charging=true;hidden=false;showCharging()}
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
 private fun makeNotification(isCharging:Boolean=charging):Notification{
  val open=PendingIntent.getActivity(this,1,Intent(this,ChargingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val stop=PendingIntent.getService(this,2,Intent(this,ChargingService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val text=if(isCharging) "Зарядка подключена · нажмите, чтобы открыть экран" else "Мониторинг включён · ждём зарядку"
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_charging)
   .setContentTitle("Charging Screen PRO").setContentText(text)
   .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_STATUS)
   .setContentIntent(open).setOngoing(true).addAction(0,"Открыть экран",open).addAction(0,"Остановить",stop).build()
 }
 override fun onDestroy(){closeTask?.let(handler::removeCallbacks);try{unregisterReceiver(batteryReceiver)}catch(_:Exception){};super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
 companion object{
  const val CHANNEL="charging_screen";const val ACTION_UPDATE="com.chargingscreen.pro.UPDATE"
  const val ACTION_CLOSE="com.chargingscreen.pro.CLOSE";const val ACTION_HIDE="com.chargingscreen.pro.HIDE"
  const val ACTION_SHOW="com.chargingscreen.pro.SHOW";const val ACTION_STOP="com.chargingscreen.pro.STOP"
  const val ACTION_POWER_CONNECTED="com.chargingscreen.pro.POWER_CONNECTED"
 }
}
