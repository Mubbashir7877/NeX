package com.pck.nex.widget

import android.content.Intent
import android.widget.RemoteViewsService

class NeXWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return NeXWidgetFactory(applicationContext)
    }
}
