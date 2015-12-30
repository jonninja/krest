package com.steamstreet.krest

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import uy.kohesive.injekt.api.erasedType
import uy.kohesive.injekt.api.fullType
import java.lang.reflect.Type
import java.net.URI
import java.util.*

private val defaultHttpClient = HttpClients.createDefault()
private val defaultMapper = jacksonObjectMapper().apply {
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    this.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
    this.setSerializationInclusion(JsonInclude.Include.NON_NULL)
}

class KResponse<T>(val type: Type, val response: HttpResponse) {
    var jsonMapper: ObjectMapper = defaultMapper

    @Suppress("UNCHECKED_CAST")
    val body: T by lazy {
        val contentType = ContentType.get(response.entity).mimeType
        if (contentType == "application/json") {
            defaultMapper.readValue(response.entity.content, type.erasedType() as Class<*>) as T
        } else {
            throw IllegalStateException()
        }
    }

    /**
     * The status code of the response
     */
    val code: Int get() {
        return response.statusLine.statusCode
    }
}

abstract class KRequest<T: HttpUriRequest>(val request: T) {
    var client = defaultHttpClient

    inline fun <reified T: Any> response(): KResponse<T> = response<T>(fullType<T>().type)

    open fun <T> response(type: Type): KResponse<T> {
        return KResponse<T>(type, client.execute(request))
    }

    fun header(key: String, value: String) {
        request.addHeader(key, value)
    }
}

class KGetRequest(uri: URI): KRequest<HttpGet>(HttpGet(uri)) {
}

abstract class KBodyRequest<T: HttpEntityEnclosingRequestBase>(request: T): KRequest<T>(request) {
    protected var entity: HttpEntity? = null
    var mapper: ObjectMapper = defaultMapper

    override fun <T> response(type: Type): KResponse<T> {
        if (entity != null) {
            request.entity = entity
        }
        return super.response(type)
    }

    /**
     * Add a JSON body
     */
    fun json(body: Any) {
        val jsonString = mapper.writeValueAsString(body)
        entity = StringEntity(jsonString, ContentType.APPLICATION_JSON)
    }
}

class KPostRequest(uri: URI): KBodyRequest<HttpPost>(HttpPost(uri)) {
    private val params = ArrayList<Pair<String, String>>()

    override fun <T> response(type: Type): KResponse<T> {
        if (params.size > 0 && request.entity == null) {
            entity = UrlEncodedFormEntity(params.map {
                BasicNameValuePair(it.first, it.second)
            })
        }
        return super.response(type)
    }

    /**
     * Add a form field to the post request
     */
    fun field(key: String, value: String) {
        params.add(Pair(key, value))
    }
}

/**
 * Prepare a REST GET request
 */
fun URI.get(init: (KGetRequest.() -> Unit)? = null): KGetRequest {
    val result = KGetRequest(this)
    if (init != null) {
        result.init()
    }
    return result
}

/**
 * Prepare a REST POST request
 */
fun URI.post(init: (KPostRequest.() -> Unit)? = null): KPostRequest {
    val result = KPostRequest(this)
    if (init != null) result.init()
    return result
}