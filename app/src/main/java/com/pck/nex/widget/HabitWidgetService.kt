package com.pck.nex.widget

import android.content.Intent
import android.widget.RemoteViewsService

class HabitWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetFactory(applicationContext, intent)
    }
}
