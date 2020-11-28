package top.ntutn.kfeed.deepinbbs

import com.fasterxml.jackson.databind.ObjectMapper
import com.rometools.rome.feed.rss.Content
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import com.rometools.rome.io.impl.DateParser
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit


class MyArgs(parser: ArgParser) {
    val saveDirectory by parser.storing("-s", "--save", help = "保存到的文件目录").default("")
    val port by parser.storing("-p", "--port", help = "服务端口号").default("8080")
}

fun HttpExchange.normalResponse(content: ByteArray, contentType: String = "text/html") {
    responseHeaders.add("Content-Type", "$contentType; charset=UTF-8")
    sendResponseHeaders(200, content.size.toLong())
    responseBody.write(content)
    close()
}

fun convertJsonToFeed(jsonString: String, feedTitle: String? = ""): String {
    val jsonNode = ObjectMapper().readTree(jsonString)
    val articles = jsonNode.get("ThreadIndex").map { node ->
        SyndEntryImpl().apply {
            title = node.get("subject").textValue()
            link = "https://bbs.deepin.org/post/${node.get("id").numberValue()}"
            publishedDate = DateParser.parseDate(node.get("created_at").textValue(), Locale.CHINA)
            description = SyndContentImpl().apply {
                type = Content.HTML
                val doc: Document = Jsoup.connect(link).get()
                value = doc.select("div.post_conten").outerHtml()
//                value = node.get("subject").textValue()
            }
        }
    }

    val feed = SyndFeedImpl().apply {
        feedType = "rss_2.0"
        title = feedTitle
        description = "deepin的非官方rss-$feedTitle"
        link = "https://bbs.deepin.org/"
        entries = articles
    }
    return SyndFeedOutput().outputString(feed, true)
}

fun getFeedString(okHttpClient: OkHttpClient, url: String): String? {
    val call = okHttpClient.newCall(
        Request.Builder()
            .url(url)
            .get().build()
    )
    return call.execute().body?.string()
}

fun getAllFeedString(okHttpClient: OkHttpClient) =
    getFeedString(okHttpClient, "https://bbs.deepin.org/api/v1/thread/index?order=updated_at&limit=20&where=&offset=0")

fun getHotFeedString(okHttpClient: OkHttpClient) =
    getFeedString(
        okHttpClient,
        "https://bbs.deepin.org/api/v1/thread/index?order=updated_at&limit=20&where=hot_value&offset=0"
    )

fun getHighLightFeedString(okHttpClient: OkHttpClient) =
    getFeedString(
        okHttpClient,
        "https://bbs.deepin.org/api/v1/thread/index?order=updated_at&limit=20&where=is_digest&offset=0"
    )

fun main(args: Array<String>) {
    mainBody {
        val okHttpClient = OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build()
        ArgParser(args).parseInto(::MyArgs).run {
            if (saveDirectory.trim() != "") {
                File(saveDirectory, "all.xml").writeText(
                    convertJsonToFeed(
                        getAllFeedString(okHttpClient) ?: ""
                    )
                )
                File(saveDirectory, "hot.xml").writeText(
                    convertJsonToFeed(
                        getHotFeedString(okHttpClient) ?: ""
                    )
                )
                File(saveDirectory, "highlight.xml").writeText(
                    convertJsonToFeed(
                        getHighLightFeedString(okHttpClient) ?: ""
                    )
                )
                return@mainBody
            }

            println("服务运行在${port}端口。")

            HttpServer.create(InetSocketAddress(port.toInt()), 0).apply {
                createContext("/") {
                    when (it.requestURI.path) {
                        "/all" -> {
                            val responseString = getAllFeedString(okHttpClient)
                            responseString?.let { resp ->
                                it.normalResponse(convertJsonToFeed(resp).toByteArray(), "application/xml")
                            }
                        }
                        "/hot" -> {
                            val responseString = getHotFeedString(okHttpClient)
                            responseString?.let { resp ->
                                it.normalResponse(convertJsonToFeed(resp).toByteArray(), "application/xml")
                            }
                        }
                        "/highlight" -> {
                            val responseString = getHighLightFeedString(okHttpClient)
                            responseString?.let { resp ->
                                it.normalResponse(convertJsonToFeed(resp).toByteArray(), "application/xml")
                            }
                        }
                        else -> {
                            val content = """
                                /hot查看所有帖子
                                /hot查看热门
                                /highlight查看精华帖
                            """.trimIndent().encodeToByteArray()
                            it.normalResponse(content)
                        }
                    }
                }
                start()
            }
        }
    }
}