package com.chargingscreen.pro
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
class MainActivity:AppCompatActivity(){
 private val prefs by lazy{getSharedPreferences("charging_settings",MODE_PRIVATE)}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main)
  findViewById<Spinner>(R.id.clockFormat).setup(listOf("24 часа","12 часов"),prefs.getInt("format",0)){prefs.edit().putInt("format",it).apply()}
  findViewById<Spinner>(R.id.animationColor).setup(listOf("Голубой","Зелёный","Фиолетовый","Красный"),prefs.getInt("color",0)){prefs.edit().putInt("color",it).apply()}
  findViewById<Spinner>(R.id.screenStyle).setup(listOf("Классический круг","Тонкое кольцо","Неоновый","Минималистичный"),prefs.getInt("style",0)){prefs.edit().putInt("style",it).apply()}
  val bright=findViewById<SeekBar>(R.id.brightness);bright.progress=prefs.getInt("brightness",65)
  bright.setOnSeekBarChangeListener(listener{prefs.edit().putInt("brightness",it).apply()})
  val swipe=findViewById<SeekBar>(R.id.swipeSensitivity);swipe.progress=prefs.getInt("swipe",100)
  swipe.setOnSeekBarChangeListener(listener{prefs.edit().putInt("swipe",it).apply()})
  val sound=findViewById<CheckBox>(R.id.soundEnabled);sound.isChecked=prefs.getBoolean("sound",true)
  sound.setOnCheckedChangeListener{_,v->prefs.edit().putBoolean("sound",v).apply()}
  val auto=findViewById<CheckBox>(R.id.autoBrightness);auto.isChecked=prefs.getBoolean("auto_night",false)
  auto.setOnCheckedChangeListener{_,v->prefs.edit().putBoolean("auto_night",v).apply()}
  findViewById<Button>(R.id.startService).setOnClickListener{
   if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),44)
   prefs.edit().putBoolean("monitoring",true).apply()
   ContextCompat.startForegroundService(this,Intent(this,ChargingService::class.java))
   Toast.makeText(this,"Мониторинг включён",Toast.LENGTH_SHORT).show()
  }
  findViewById<Button>(R.id.stopService).setOnClickListener{
   stopService(Intent(this,ChargingService::class.java));prefs.edit().putBoolean("monitoring",false).apply()
   Toast.makeText(this,"Мониторинг выключен",Toast.LENGTH_SHORT).show()
  }
 }
 private fun listener(save:(Int)->Unit)=object:SeekBar.OnSeekBarChangeListener{
  override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean){if(user)save(v)}
  override fun onStartTrackingTouch(s:SeekBar?){}
  override fun onStopTrackingTouch(s:SeekBar?){}
 }
 private fun Spinner.setup(items:List<String>,selected:Int,changed:(Int)->Unit){
  adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,items);setSelection(selected)
  onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
   override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
   override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long){changed(position)}
  }
 }
}