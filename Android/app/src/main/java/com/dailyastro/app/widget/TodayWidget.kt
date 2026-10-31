package com.dailyastro.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dailyastro.app.MainActivity
import com.dailyastro.app.R
import com.dailyastro.app.model.Horoscope

/** Home-screen widget with today's sign and advice. Content is pushed whenever the app or the reminder loads today's reading. */
class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = render(context, manager, ids)

    companion object {
        private const val PREFS = "today_widget"

        fun update(context: Context, h: Horoscope) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("title", "${h.sign.symbol}  ${h.sign.name}").putString("date", h.date).putString("text", h.special_advice)
                .putString("sign", h.sign.id).apply()
            refresh(context)
        }

        fun clear(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            refresh(context)
        }

        fun isPinned(context: Context) = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TodayWidget::class.java)).isNotEmpty()

        fun requestPin(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            if (!isPinned(context) && manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, TodayWidget::class.java), null, null)
        }

        private fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            render(context, manager, manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java)))
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
                setTextViewText(R.id.widget_title, prefs.getString("title", context.getString(R.string.app_name)))
                setTextViewText(R.id.widget_date, prefs.getString("date", ""))
                setTextViewText(R.id.widget_text, prefs.getString("text", context.getString(R.string.widget_tap)))
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
