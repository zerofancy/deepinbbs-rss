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
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit


class MyArgs(parser: ArgParser) {
    val port by parser.storing("-p", "--port", help = "服务端口号").default("8080")
}

fun HttpExchange.normalResponse(content: ByteArray, contentType: String = "text/html") {
    responseHeaders.add("Content-Type", "$contentType; charset=UTF-8")
    sendResponseHeaders(200, content.size.toLong())
    responseBody.write(content)
    close()
}

fun convertJsonToFeed(jsonString: String): String {
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
        title = "deepin论坛"
        description = "deepin的非官方rss"
        link = "https://bbs.deepin.org/"
        entries = articles
    }
    return SyndFeedOutput().outputString(feed, true)
}

fun main(args: Array<String>) {
    mainBody {
        ArgParser(args).parseInto(::MyArgs).run {
            val okHttpClient = OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build()

            println("服务运行在${port}端口。")

            HttpServer.create(InetSocketAddress(port.toInt()), 0).apply {
                createContext("/") {
                    when (it.requestURI.path) {
                        "/all" -> {
//                            it.queryToMap()?.let { println(it) }
                            val call = okHttpClient.newCall(
                                Request.Builder()
                                    .url("https://bbs.deepin.org/api/v1/thread/index?order=updated_at&limit=20&where=&offset=0")
                                    .get().build()
                            )
                            val responseString = call.execute().body?.string()
                            responseString?.let { resp ->
                                it.normalResponse(convertJsonToFeed(resp).toByteArray(), "application/xml")
                            }
                        }
                        "/hot" -> {
                            val call = okHttpClient.newCall(
                                Request.Builder()
                                    .url("https://bbs.deepin.org/api/v1/thread/index?order=updated_at&limit=20&where=hot_value&offset=0")
                                    .get().build()
                            )
                            val responseString = call.execute().body?.string()
                            responseString?.let { resp ->
                                it.normalResponse(convertJsonToFeed(resp).toByteArray(), "application/xml")
                            }
                        }
                        "/highlight" -> {
                            val call = okHttpClient.newCall(
                                Request.Builder()
                                    .url("https://bbs.deepin.org/api/v1/thread/index?order=updated_at&limit=20&where=is_digest&offset=0")
                                    .get().build()
                            )
                            val responseString = call.execute().body?.string()
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