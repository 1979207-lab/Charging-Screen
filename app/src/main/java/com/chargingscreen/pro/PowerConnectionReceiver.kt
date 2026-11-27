package com.chargingscreen.pro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Получает системное событие подключения питания даже если приложение закрыто.
 * Сам экран запускается службой; Android может ограничить показ Activity поверх блокировки.
 */
class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return

        val prefs = context.getSharedPreferences("charging_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("monitoring", false)) return

        val serviceIntent = Intent(context, ChargingService::class.java)
            .setAction(ChargingService.ACTION_POWER_CONNECTED)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // На некоторых прошивках запуск фоновой службы ограничен системой.
        }
    }
}
