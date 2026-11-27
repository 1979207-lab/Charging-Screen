package com.chargingscreen.pro
import android.content.*
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
class ChargingActivity:AppCompatActivity(){
 private val prefs by lazy{getSharedPreferences("charging_settings",MODE_PRIVATE)}
 private val handler=Handler(Looper.getMainLooper())
 private lateinit var clock:TextView
 private lateinit var date:TextView
 private lateinit var info:TextView
 private lateinit var remaining:TextView
 private lateinit var ring:ChargingView
 private var downY=0f
 private var fullSoundPlayed=false
 private val tone by lazy{ToneGenerator(AudioManager.STREAM_NOTIFICATION,75)}
 private val receiver=object:BroadcastReceiver(){
  override fun onReceive(context:Context?,intent:Intent?){intent?:return
   when(intent.action){
    ChargingService.ACTION_UPDATE->render(intent.getBooleanExtra("charging",false),intent.getIntExtra("level",0),intent.getIntExtra("temperature",-1))
    ChargingService.ACTION_CLOSE->closeAnimated()
   }
  }
 }
 private val clockTick=object:Runnable{override fun run(){updateClock();handler.postDelayed(this,1000)}}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  window.statusBarColor=Color.BLACK;window.navigationBarColor=Color.BLACK
  @Suppress("DEPRECATION")
  window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
  setContentView(R.layout.activity_charging)
  clock=findViewById(R.id.clock);date=findViewById(R.id.date);info=findViewById(R.id.powerInfo)
  remaining=findViewById(R.id.timeRemaining);ring=findViewById(R.id.chargingView)
  ring.setColor(prefs.getInt("color",0));ring.setStyle(prefs.getInt("style",0))
  updateClock();handler.post(clockTick)
  val filter=IntentFilter().apply{addAction(ChargingService.ACTION_UPDATE);addAction(ChargingService.ACTION_CLOSE)}
  if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") registerReceiver(receiver,filter)
  val sticky=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
  sticky?.let{render(it.getIntExtra("plugged",0)!=0,it.getIntExtra("level",0),it.getIntExtra("temperature",-1))}
  listOf(clock,date,ring,info,remaining).forEachIndexed{i,v->v.alpha=0f;v.animate().alpha(1f).setStartDelay(i*90L).setDuration(450).setInterpolator(DecelerateInterpolator()).start()}
 }
 private fun updateClock(){
  val pattern=if(prefs.getInt("format",0)==1)"hh:mm a" else "HH:mm"
  clock.text=SimpleDateFormat(pattern,Locale.getDefault()).format(Date())
  date.text=SimpleDateFormat("EEEE, d MMMM",Locale("ru")).format(Date())
 }
 private fun render(charging:Boolean,level:Int,temp:Int){
  ring.update(level,charging)
  val bm=getSystemService(BATTERY_SERVICE) as BatteryManager
  val current=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
  val batteryIntent=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
  val mv=batteryIntent?.getIntExtra("voltage",0)?:0
  val watts=abs(current.toLong())/1_000_000.0*mv/1000.0
  val tempText=if(temp>=0)" · "+String.format(Locale.US,"%.1f°C",temp/10.0) else ""
  info.text=if(watts>0.02)String.format(Locale.US,"%.1f Вт",watts)+tempText else "Мощность не определяется"+tempText
  remaining.text=estimateTime(bm,level)
  if(level>=100&&charging&&prefs.getBoolean("sound",true)&&!fullSoundPlayed){fullSoundPlayed=true;tone.startTone(ToneGenerator.TONE_PROP_ACK,500)}
  if(level<100)fullSoundPlayed=false
  applyBrightness()
 }
 private fun estimateTime(bm:BatteryManager,level:Int):String{
  if(level>=100)return "Аккумулятор полностью заряжен"
  if(Build.VERSION.SDK_INT>=28){
   val ms=bm.computeChargeTimeRemaining()
   if(ms>0){val mins=ms/60000;return "До 100%: ${mins/60} ч ${mins%60} мин (оценка)"}
  }
  return "Время до 100% рассчитывается телефоном"
 }
 private fun applyBrightness(){
  val value=prefs.getInt("brightness",65).coerceIn(5,100)/100f
  val hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
  val night=prefs.getBoolean("auto_night",false)&&(hour>=22||hour<7)
  window.attributes=window.attributes.apply{screenBrightness=if(night)minOf(value,.15f)else value}
 }
 private fun hideScreen(){
  startService(Intent(this,ChargingService::class.java).setAction(ChargingService.ACTION_HIDE))
  window.decorView.animate().alpha(0f).setDuration(220).withEndAction{finish()}.start()
 }
 private fun closeAnimated(){window.decorView.animate().alpha(0f).setDuration(220).withEndAction{finish()}.start()}
 override fun dispatchTouchEvent(event:MotionEvent):Boolean{
  when(event.action){
   MotionEvent.ACTION_DOWN->downY=event.rawY
   MotionEvent.ACTION_UP->{
    val delta=downY-event.rawY
    val threshold=prefs.getInt("swipe",100).coerceIn(20,250).toFloat()
    if(delta>threshold||abs(delta)<12f){hideScreen();return true}
   }
  }
  return super.dispatchTouchEvent(event)
 }
 override fun onDestroy(){
  handler.removeCallbacks(clockTick)
  try{unregisterReceiver(receiver)}catch(_:Exception){}
  try{tone.release()}catch(_:Exception){}
  super.onDestroy()
 }
}