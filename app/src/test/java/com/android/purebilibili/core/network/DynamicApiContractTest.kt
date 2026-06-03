package com.android.purebilibili.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

class DynamicApiContractTest {

    @Test
    fun likeDynamic_usesQueryCsrfAndJsonBody() {
        val method = DynamicApi::class.java.methods.first { it.name == "likeDynamic" }

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        val secondParamAnnotations = method.parameterAnnotations[1].toList()
        val thirdParamAnnotations = method.parameterAnnotations[2].toList()

        val query = firstParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("csrf", query?.value)

        val csrfTokenQuery = secondParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("csrf_token", csrfTokenQuery?.value)

        assertTrue(thirdParamAnnotations.any { it is Body })
        assertEquals(DynamicThumbRequest::class.java, method.parameterTypes[2])
    }

    @Test
    fun dynamicThumbRequest_defaultsMatchDesktopWebClient() {
        val request = DynamicThumbRequest(dyn_id_str = "123", up = 1)

        assertEquals("333.1369.0.0", request.spmid)
        assertEquals("333.999.0.0", request.from_spmid)
    }

    @Test
    fun repostDynamic_usesJsonDynReqBodyAndWebQueries() {
        val method = DynamicApi::class.java.methods.first { it.name == "repostDynamic" }
        val post = method.getAnnotation(POST::class.java)

        assertEquals("x/dynamic/feed/create/dyn", post?.value)
        assertFalse(method.isAnnotationPresent(FormUrlEncoded::class.java))

        val queryNames = method.parameterAnnotations
            .mapNotNull { annotations ->
                annotations.filterIsInstance<Query>().firstOrNull()?.value
            }

        assertTrue("csrf" in queryNames)
        assertTrue("platform" in queryNames)
        assertTrue("x-bili-device-req-json" in queryNames)
        assertTrue("x-bili-web-req-json" in queryNames)

        val bodyParamIndex = method.parameterAnnotations.indexOfFirst { annotations ->
            annotations.any { it is Body }
        }
        assertEquals(DynamicRepostRequest::class.java, method.parameterTypes[bodyParamIndex])
        assertTrue(method.parameterAnnotations.none { annotations ->
            annotations.any { it is Field }
        })
    }

    @Test
    fun deleteDynamic_usesOperateRemoveJsonBodyAndWebQueries() {
        val method = DynamicApi::class.java.methods.first { it.name == "deleteDynamic" }
        val post = method.getAnnotation(POST::class.java)

        assertEquals("x/dynamic/feed/operate/remove", post?.value)
        assertFalse(method.isAnnotationPresent(FormUrlEncoded::class.java))

        val queryNames = method.parameterAnnotations
            .mapNotNull { annotations ->
                annotations.filterIsInstance<Query>().firstOrNull()?.value
            }

        assertTrue("csrf" in queryNames)
        assertTrue("platform" in queryNames)

        val bodyParamIndex = method.parameterAnnotations.indexOfFirst { annotations ->
            annotations.any { it is Body }
        }
        assertEquals(DynamicDeleteRequest::class.java, method.parameterTypes[bodyParamIndex])
        assertTrue(method.parameterAnnotations.none { annotations ->
            annotations.any { it is Field }
        })
    }

    @Test
    fun dynamicDeleteRequest_omitsNullOptionalFields() {
        val request = DynamicDeleteRequest(dyn_id_str = "1063487284684259332")
        val json = Json.encodeToString(request)

        assertTrue(json.contains("\"dyn_id_str\":\"1063487284684259332\""))
        assertFalse(json.contains("dyn_type"))
        assertFalse(json.contains("rid_str"))
    }

    @Test
    fun dynamicDeleteRequest_keepsDeleteMenuParamsWhenProvided() {
        val request = DynamicDeleteRequest(
            dyn_id_str = "1063487284684259332",
            dyn_type = 1,
            rid_str = "1063487284684259332"
        )
        val json = Json.encodeToString(request)

        assertTrue(json.contains("\"dyn_id_str\":\"1063487284684259332\""))
        assertTrue(json.contains("\"dyn_type\":1"))
        assertTrue(json.contains("\"rid_str\":\"1063487284684259332\""))
    }

    @Test
    fun buildDynamicRepostRequest_emptyContentMatchesWebRepostPayload() {
        val request = buildDynamicRepostRequest(dynamicId = "977045888118554640", content = "")
        val json = Json.encodeToString(request)

        assertEquals("977045888118554640", request.web_repost_src.dyn_id_str)
        assertEquals(4, request.dyn_req.scene)
        assertTrue(request.dyn_req.content.contents.isEmpty())
        assertTrue(json.contains("\"scene\":4"))
        assertTrue(json.contains("\"attach_card\":null"))
    }

    @Test
    fun buildDynamicRepostRequest_textContentUsesPlainTextContentItem() {
        val request = buildDynamicRepostRequest(
            dynamicId = "977045888118554640",
            content = "转发动态"
        )
        val contentItem = request.dyn_req.content.contents.single()
        val json = Json.encodeToString(request)

        assertEquals("转发动态", contentItem.raw_text)
        assertEquals(1, contentItem.type)
        assertEquals("", contentItem.biz_id)
        assertTrue(json.contains("\"scene\":4"))
        assertTrue(json.contains("\"type\":1"))
        assertTrue(json.contains("\"biz_id\":\"\""))
    }

    @Test
    fun getDynamicDetail_usesDesktopDetailEndpointAndIdQuery() {
        val method = DynamicApi::class.java.methods.first { it.name == "getDynamicDetail" }
        val get = method.getAnnotation(GET::class.java)
        assertEquals("x/polymer/web-dynamic/desktop/v1/detail", get?.value)

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        val idQuery = firstParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("id", idQuery?.value)
    }

    @Test
    fun dynamicDetailFeatures_includeOnlyFansDetailFlagsFromApiDocs() {
        assertTrue(DYNAMIC_DETAIL_FEATURES.contains("commentsNewVersion"))
        assertTrue(DYNAMIC_DETAIL_FEATURES.contains("onlyfansAssetsV2"))
        assertTrue(DYNAMIC_DETAIL_FEATURES.contains("onlyfansQaCard"))
        assertTrue(DYNAMIC_DETAIL_FEATURES.contains("endFooterHidden"))
    }

    @Test
    fun getDynamicDetailFallback_usesLegacyDetailEndpointAndIdQuery() {
        val method = DynamicApi::class.java.methods.first { it.name == "getDynamicDetailFallback" }
        val get = method.getAnnotation(GET::class.java)
        assertEquals("x/polymer/web-dynamic/v1/detail", get?.value)

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        val idQuery = firstParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("id", idQuery?.value)
    }

    @Test
    fun getOpusDetail_usesDocumentedOpusDetailEndpointAndIdQuery() {
        val method = DynamicApi::class.java.methods.first { it.name == "getOpusDetail" }
        val get = method.getAnnotation(GET::class.java)
        assertEquals("x/polymer/web-dynamic/v1/opus/detail", get?.value)

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        val idQuery = firstParamAnnotations.filterIsInstance<Query>().firstOrNull()
        assertEquals("id", idQuery?.value)
    }

    @Test
    fun getSpaceArticleList_usesDocumentedOpusSpaceFeedEndpointAndQueryMap() {
        val method = SpaceApi::class.java.methods.first { it.name == "getSpaceArticleList" }
        val get = method.getAnnotation(GET::class.java)
        assertEquals("x/polymer/web-dynamic/v1/opus/feed/space", get?.value)

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        assertTrue(firstParamAnnotations.any { it is QueryMap })
    }

    @Test
    fun getSpaceDynamic_requestsFeatureFlagsForOpusTextAndCommentCounts() {
        val method = SpaceApi::class.java.methods.first { it.name == "getSpaceDynamic" }
        val get = method.getAnnotation(GET::class.java)
        assertEquals("x/polymer/web-dynamic/v1/feed/space", get?.value)

        val queryNames = method.parameterAnnotations
            .mapNotNull { annotations ->
                annotations.filterIsInstance<Query>().firstOrNull()?.value
            }

        assertTrue("features" in queryNames)
        assertTrue(SPACE_DYNAMIC_FEATURES.contains("itemOpusStyle"))
        assertTrue(SPACE_DYNAMIC_FEATURES.contains("opusBigCover"))
        assertTrue(SPACE_DYNAMIC_FEATURES.contains("commentsNewVersion"))
    }

    @Test
    fun getUserDynamicFeed_usesDynamicFeedAllEndpointAndQueryMap() {
        val method = DynamicApi::class.java.methods.first { it.name == "getUserDynamicFeed" }
        val get = method.getAnnotation(GET::class.java)
        assertEquals("x/polymer/web-dynamic/v1/feed/all", get?.value)

        val firstParamAnnotations = method.parameterAnnotations[0].toList()
        assertTrue(firstParamAnnotations.any { it is QueryMap })
    }

    @Test
    fun getPbpData_usesBilivideoPbpEndpoint() {
        val method = BilibiliApi::class.java.methods.first { it.name == "getPbpData" }
        val get = method.getAnnotation(GET::class.java)

        assertEquals("https://bvc.bilivideo.com/pbp/data", get?.value)
    }
}
