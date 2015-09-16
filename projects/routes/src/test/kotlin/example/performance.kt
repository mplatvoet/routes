package examples

import nl.komponents.routes.Routes
import java.text.DecimalFormat
import java.util.*

val entries = 100000
val length = 20

val lookups = 100000
val attempts = 10

fun main(args: Array<String>) {
    val routes = Routes<Int>()

    routes.add {
        (1..entries).forEach {
            val randomPath = randomPath(length)
            //println("$it: $randomPath")
            get(randomPath, it)
        }

    }
    println("${entries} routes added, start timing lookups:")

    (1..attempts).forEach {
        val start = System.currentTimeMillis()
        (1..lookups).forEach {
            routes.mappingForPath("GET", "/a/b/c/d")?.value
            routes.mappingForPath("GET", "/a/b/c/d/e/f/g")?.value
            routes.mappingForPath("GET", "/a/b/c/d/e/f/g/h/i/j/k")?.value
        }
        val delta = System.currentTimeMillis() - start
        println("${lookups * 3} lookups took ${delta}ms, ${(delta.toDouble() / (lookups * 3).toDouble()).format()}ms per lookup")
    }

    println(routes.mappingForPath("GET", "/a/b/c/d")?.value)
    println(routes.mappingForPath("GET", "/a/b/c/d/e/f/g")?.value)
    println(routes.mappingForPath("GET", "/a/b/c/d/e/f/g/h/i/j/k")?.value)
}


val chars = "abcdefghijk".toArrayList().map { "$it" } + listOf("*", "**")
val random = Random()
fun randomPath(length: Int): String {
    val segments = random.nextInt(length) + 1
    val sb = StringBuilder()
    (1..segments).forEach {
        val idx = random.nextInt(chars.size())
        sb.append('/').append(chars[idx])
    }
    return sb.toString()
}
val df = DecimalFormat("##0.00000")
fun Double.format() = df.format(this)


