package com.android.purebilibili.feature.livesite

object LiveSiteRegistry {
    private val sites = LinkedHashMap<String, LiveSite>()

    fun register(site: LiveSite) {
        sites[site.id] = site
    }

    fun all(): List<LiveSite> = sites.values.toList()

    fun get(id: String): LiveSite? = sites[id]

    fun ids(): List<String> = sites.keys.toList()
}
