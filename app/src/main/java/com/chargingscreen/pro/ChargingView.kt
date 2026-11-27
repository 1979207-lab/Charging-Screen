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
  val x=width/2f;val y=height/2f
  val r=min(width,height)*when(style){1->.37f;3->.30f;else->.34f}
  val density=resources.displayMetrics.density
  val stroke=density*when(style){1->7f;2->17f;3->5f;else->13f}
  paint.style=Paint.Style.STROKE;paint.strokeWidth=stroke;paint.strokeCap=Paint.Cap.ROUND
  if(style==2){
   paint.color=color;paint.alpha=if(charging)(75+100*pulse).toInt() else 45
   canvas.drawCircle(x,y,r+12*density+pulse*8*density,paint)
  }else if(style==0){
   paint.color=color;paint.alpha=if(charging)(45+65*pulse).toInt() else 25
   canvas.drawCircle(x,y,r+8*density,paint)
  }
  val rect=RectF(x-r,y-r,x+r,y+r)
  if(style!=3){paint.color=Color.DKGRAY;paint.alpha=230;canvas.drawCircle(x,y,r,paint)}
  paint.color=color;paint.alpha=255
  canvas.drawArc(rect,-90f,percent*3.6f,false,paint)
  paint.style=Paint.Style.FILL;paint.textAlign=Paint.Align.CENTER;paint.color=Color.WHITE
  paint.typeface=Typeface.create("sans-serif-light",Typeface.NORMAL)
  paint.textSize=(if(style==3)48f else 40f)*resources.displayMetrics.scaledDensity
  canvas.drawText("$"+"percent%",x,y+14f*density,paint)
  paint.color=color;paint.textSize=29f*resources.displayMetrics.scaledDensity;paint.typeface=Typeface.DEFAULT_BOLD
  canvas.drawText("ϟ",x,y-35f*density,paint)
 }
 override fun onDetachedFromWindow(){anim.cancel();super.onDetachedFromWindow()}
}