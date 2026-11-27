package com.chargingscreen.pro
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.min
class ChargingView(context:Context):View(context){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private var percent=0
 private var charging=false
 private var pulse=0f
 private var color=Color.rgb(0,190,255)
 private var style=0
 private val anim=ValueAnimator.ofFloat(0f,1f).apply{
  duration=1200;repeatCount=ValueAnimator.INFINITE;repeatMode=ValueAnimator.REVERSE
  addUpdateListener{pulse=it.animatedValue as Float;invalidate()}
 }
 fun update(level:Int,isCharging:Boolean){percent=level.coerceIn(0,100);charging=isCharging;if(charging&&!anim.isStarted)anim.start() else if(!charging)anim.cancel();invalidate()}
 fun setColor(i:Int){color=when(i){1->Color.rgb(0,230,110);2->Color.rgb(180,80,255);3->Color.rgb(255,70,75);else->Color.rgb(0,190,255)};invalidate()}
 fun setStyle(i:Int){style=i.coerceIn(0,3);invalidate()}
 override fun onDraw(canvas:Canvas){
  super.onDraw(canvas)
  val x=width/2f;val y=height/2f;val r=min(width,height)*.34f
  paint.style=Paint.Style.STROKE;paint.strokeWidth=13f*resources.displayMetrics.density;paint.strokeCap=Paint.Cap.ROUND
  paint.color=color;paint.alpha=if(charging)(80+100*pulse).toInt() else 70
  canvas.drawCircle(x,y,r+10+pulse*10,paint)
  paint.color=Color.DKGRAY;paint.alpha=255;canvas.drawCircle(x,y,r,paint)
  paint.color=color;paint.alpha=255
  if(style==2)paint.strokeWidth*=1.8f
  canvas.drawArc(RectF(x-r,y-r,x+r,y+r),-90f,percent*3.6f,false,paint)
  paint.style=Paint.Style.FILL;paint.textAlign=Paint.Align.CENTER;paint.color=Color.WHITE
  paint.typeface=Typeface.create("sans-serif-light",Typeface.NORMAL);paint.textSize=40f*resources.displayMetrics.scaledDensity
  canvas.drawText("$"+"{percent}%",x,y+14f,paint)
  paint.color=color;paint.textSize=29f*resources.displayMetrics.scaledDensity;paint.typeface=Typeface.DEFAULT_BOLD
  canvas.drawText("ϟ",x,y-35f,paint)
 }
 override fun onDetachedFromWindow(){anim.cancel();super.onDetachedFromWindow()}
}