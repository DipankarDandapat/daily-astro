package com.dailyastro.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.dailyastro.app.MainActivity
import com.dailyastro.app.R
import com.dailyastro.app.data.HoroscopeCache
import com.dailyastro.app.data.Settings
import com.dailyastro.app.data.SettingsStore
import com.dailyastro.app.network.ApiProvider
import com.dailyastro.app.ui.localized
import com.dailyastro.app.widget.TodayWidget
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/** Schedules the daily/twice-daily/weekly horoscope alert. Each run reschedules the next one, so changes apply immediately. */
object Reminders {
    private const val CHANNEL = "daily_horoscope"
    private val QUIET_START: LocalTime = LocalTime.of(22, 0)
    private val QUIET_END: LocalTime = LocalTime.of(6, 0)
    private val SLOTS = listOf("morning", "evening")

    fun schedule(context: Context, s: Settings, now: LocalDateTime = LocalDateTime.now()) {
        val work = WorkManager.getInstance(context)
        if (!s.onboarded || !s.notifEnabled) { cancel(context); return }
        val time = runCatching { LocalTime.parse(s.notifTime) }.getOrDefault(LocalTime.of(7, 0))
        val slots = when (s.notifFrequency) { "twice" -> listOf(time, time.plusHours(12)); else -> listOf(time) }
        SLOTS.forEachIndexed { i, slot ->
            val at = slots.getOrNull(i)
            if (at == null) { work.cancelUniqueWork("reminder-$slot"); return@forEachIndexed }
            val next = nextRun(now, outsideQuietHours(at), weekly = s.notifFrequency == "weekly")
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("slot" to slot))
                .build()
            work.enqueueUniqueWork("reminder-$slot", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancel(context: Context) = SLOTS.forEach { WorkManager.getInstance(context).cancelUniqueWork("reminder-$it") }

    fun outsideQuietHours(t: LocalTime): LocalTime = if (!t.isBefore(QUIET_START) || t.isBefore(QUIET_END)) QUIET_END else t

    fun nextRun(now: LocalDateTime, at: LocalTime, weekly: Boolean): LocalDateTime {
        var next = now.toLocalDate().atTime(at)
        if (weekly) {
            next = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(at)
            if (!next.isAfter(now)) next = next.plusWeeks(1)
        } else if (!next.isAfter(now)) next = next.plusDays(1)
        return next
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun notify(context: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val s = store.current()
        if (!s.onboarded || !s.notifEnabled) return Result.success()
        val sign = s.sign ?: return Result.success()
        val ctx = applicationContext.localized(s.language)
        ApiProvider.deviceId = store.deviceId()
        runCatching {
            if (s.notifFrequency == "weekly") {
                val week = ApiProvider.service.period(sign, "weekly", s.language)
                Reminders.notify(applicationContext, ctx.getString(R.string.notif_weekly_title, week.sign.name), week.summary)
            } else {
                val h = ApiProvider.service.horoscope(sign, s.language, "today")
                if (s.offline) HoroscopeCache(applicationContext).put(h)
                TodayWidget.update(applicationContext, h)
                Reminders.notify(applicationContext, ctx.getString(R.string.notif_title, h.sign.name), h.special_advice)
            }
        }
        Reminders.schedule(applicationContext, s)
        return Result.success()
    }
}
