package me.stageguard.obms.cache

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runInterruptible
import me.stageguard.obms.OsuMapSuggester
import me.stageguard.obms.osu.api.OsuWebApi
import me.stageguard.obms.utils.Either
import org.jetbrains.skija.Image
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.security.MessageDigest

object ImageCache {
    @Suppress("NOTHING_TO_INLINE")
    private inline fun imageFile(name: String) =
        File(OsuMapSuggester.dataFolder.absolutePath + File.separator + "image" + File.separator + name)

    suspend fun getImageAsStream(
        url: String, maxTryCount: Int = 4, tryCount: Int = 0
    ) : Result<InputStream> = if(maxTryCount == tryCount + 1) {
        Result.failure(IllegalStateException("Failed to get image from $url after $maxTryCount tries"))
    } else {
        val headers = OsuWebApi.head(url, headers = mapOf(), parameters = mapOf())
        val etag = headers["etag"]
        if(etag != null) {
            val file = imageFile(etag.trim('"'))
            file.parentFile.mkdirs()
            if(file.exists()) {
                try {
                    Result.success(file.inputStream())
                } catch (ex: Exception) {
                    file.delete()
                    getImageAsStream(url, maxTryCount, tryCount + 1)
                }
            } else {
                try {
                    val stream = OsuWebApi.openStream(url, headers = mapOf(), parameters = mapOf())
                    runInterruptible {
                        file.createNewFile()
                        stream.use {
                            file.writeBytes(it.readAllBytes())
                        }
                    }
                    Result.success(file.inputStream())
                } catch (ex: Exception) { getImageAsStream(url, maxTryCount, tryCount + 1) }
            }
        } else { getImageAsStream(url, maxTryCount, tryCount + 1) }
    }

    suspend fun getImageAsSkijaImage(url: String) = getImageAsStream(url).run {
        runInterruptible {
            if(isSuccess) {
                Result.success(Image.makeFromEncoded(getOrThrow().readAllBytes()))
            } else {
                Result.failure(exceptionOrNull()!!)
            }
        }
    }
}