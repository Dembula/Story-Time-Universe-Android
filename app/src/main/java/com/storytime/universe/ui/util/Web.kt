package com.storytime.universe.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a URL in the browser — payments & account management always leave the app. */
fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
