package com.android.purebilibili.data.repository

import android.net.Uri
import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

internal data class SpaceShopProduct(
    val title: String, val cover: String, val url: String,
    val price: String, val labels: String, val benefits: String, val source: String,
)
internal data class SpaceShopPage(
    val products: List<SpaceShopProduct>, val moreUrl: String?, val moreLabel: String,
)

/** 首批商品由接口提供，后续浏览通过小店网页承接。 */
internal object SpaceShopRepository {
    suspend fun load(mid: Long): Result<SpaceShopPage> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "actionKey" to "appkey", "build" to "8430300", "mVersion" to "309",
                "mallVersion" to "8430300", "statistics" to """{"appId":1,"platform":3,"version":"8.43.0","abtest":""}""", "appkey" to AppSignUtils.ANDROID_APP_KEY,
            )
            TokenManager.accessTokenCache?.takeIf { it.isNotBlank() }?.let { params["access_key"] = it }
            val response = NetworkModule.spaceApi.getSpaceShop(AppSignUtils.signForAndroidApi(params), buildJsonObject {
                put("from", "cps_productTab_$mid"); put("searchAfter", 0)
                put("msource", "cps_productTab_$mid"); put("pageSize", 8); put("upMid", mid.toString())
            })
            require(response["code"]?.jsonPrimitive?.intOrNull == 0) {
                response["message"]?.jsonPrimitive?.contentOrNull ?: "小店加载失败"
            }
            Result.success(parseSpaceShopPage(response["data"] as? JsonObject ?: error("小店响应缺少数据")))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    }
}

internal fun shopWebUrl(raw: String): String? = runCatching {
    val uri = Uri.parse(raw)
    val target = if (uri.scheme in listOf("http", "https")) raw else uri.takeIf { it.isHierarchical }?.getQueryParameter("url") ?: return@runCatching null
    target.takeIf { Uri.parse(it).scheme in listOf("http", "https") }
}.getOrNull()

internal fun parseSpaceShopPage(data: JsonObject): SpaceShopPage {
    fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun JsonObject.obj(key: String) = get(key) as? JsonObject ?: JsonObject(emptyMap())
    fun JsonObject.array(key: String) = (get(key) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    return SpaceShopPage(
        products = data.array("data").map { item ->
            val price = item.obj("netPrice")
            SpaceShopProduct(
                item.text("title"), item.obj("cover").text("url"), item.text("cardUrl"),
                listOf(price.text("pricePrefix"), price.text("priceSymbol") + price.text("netPrice")).filter { it.isNotBlank() }.joinToString(" "),
                item.array("belowLabels").map { it.text("title") }.filter { it.isNotBlank() }.joinToString(" | "),
                item.array("benefitInfos").map { it.text("prefix") + it.text("amount") + it.text("suffix") }.filter { it.isNotBlank() }.joinToString(" | "),
                item.text("itemSourceName"),
            )
        },
        moreUrl = data.text("clickUrl").takeIf { (data["showMoreTab"] as? JsonPrimitive)?.booleanOrNull == true }?.let(::shopWebUrl),
        moreLabel = data.text("showMoreDesc").ifBlank { "查看更多" },
    )
}
