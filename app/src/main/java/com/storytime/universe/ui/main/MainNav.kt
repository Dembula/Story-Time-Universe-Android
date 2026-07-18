package com.storytime.universe.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.PersonRoute
import com.storytime.universe.ui.player.PlaybackRequest

/** Holds transient navigation payloads (seeds, person routes, playback requests). */
class MainViewModel : ViewModel() {
    val seeds = mutableMapOf<String, ContentItem>()
    val personRoutes = mutableMapOf<String, PersonRoute>()
    var pendingPlayback: PlaybackRequest? = null
}

/** Convenience navigation actions shared across screens. */
class NavActions(val nav: NavController, val vm: MainViewModel) {

    fun openDetail(item: ContentItem) {
        vm.seeds[item.id] = item
        nav.navigate("detail/${Uri.encode(item.id)}")
    }

    fun openDetailById(contentId: String) {
        nav.navigate("detail/${Uri.encode(contentId)}")
    }

    fun openPerson(route: PersonRoute) {
        vm.personRoutes[route.id] = route
        nav.navigate("person/${Uri.encode(route.id)}")
    }

    fun play(request: PlaybackRequest) {
        vm.pendingPlayback = request
        nav.navigate("player")
    }
}
