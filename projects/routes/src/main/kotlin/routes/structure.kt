package nl.komponents.routes

import java.util.concurrent.atomic.AtomicReference

public class Routes<T : Any> {
    private val methods = AtomicReference<ListItem<Method<T>>>(null)
    private val builder = ConcreteRoutesBuilder()
    public fun mappingForPath(method: String, value: String): PathMapping<T>? {
        methods.iterate { m ->
            if (m.hasName(method)) {
                val path = Path(value)
                val mapping = Mapping()
                val result = m.findValue(path, mapping)
                if (result != null) {
                    return PathMapping(result, mapping)
                }
            }
        }
        return null
    }

    fun add(body: RoutesBuilder<T>.() -> Unit) = builder.body()

    interface RoutesBuilder<T : Any> {
        fun get(path: String, value: T)
        fun post(path: String, value: T)
    }

    private inner class ConcreteRoutesBuilder : RoutesBuilder<T> {
        override fun get(path: String, value: T) = add("GET", path, value)

        override fun post(path: String, value: T) = add("POST", path, value)

        private fun add(methodName: String, path: String, value: T) {
            val method = getOrAddMethod(methodName)
            addPath(method, path, value)
        }
    }

    private fun addPath(method: Method<T>, path: String, value: T) {
        var tail: Node<T> = method
        parts(path).forEach { part ->
            val node: Node<T> = when {
                part == "*" -> SingleWildcard()
                part == "**" -> MultiWildcard()
                else -> Directory(part)
            }
            tail = tail.addOrGetSibling(node)
        }
        tail.trySetValue(value)
    }

    private fun parts(path: String): List<String> {
        return path.split("/").filter { it.isNotBlank() }
    }

    private fun getOrAddMethod(methodName: String): Method<T> {
        val method = Method.forName<T>(methodName)
        while (true) {
            val head = methods.get()
            if (head == null) {
                if (methods.compareAndSet(null, ListItem(method))) {
                    return method
                }
            } else {
                var tail = head
                head.iterate {
                    tail = it
                    if (it.value == method) return it.value
                }
                if (tail.tryAppend(ListItem(method))) return method
            }
        }
    }
}


public class PathMapping<T : Any>(val value: T, val mapping: Mapping)

private class Mapping() {
    fun add(key: String, value: String) {
        println("TODO, key=$key, value=$value")
    }
}

//revise this, avoid excessive instance creation
//good enough for demo purposes
private class Path(path: String) {
    val value: String
    val subPath: Path?

    init {
        val trimmed = path.trimStart { it == '/' }
        val index = trimmed.indexOf('/')
        value = if (index > -1) trimmed.substring(0, index) else trimmed
        subPath = if (index > -1) Path(trimmed.substring(index)) else null
    }
}

private abstract class Node<T : Any> {

    protected val siblings = AtomicReference<ListItem<Node<T>>>(null)

    private val valueRef = AtomicReference<T?>(null)

    public val value: T? get() = valueRef.get()

    public fun trySetValue(value: T): Boolean = valueRef.compareAndSet(null, value)

    abstract fun findValue(path: Path, mapping: Mapping): T?

    fun <V : Node<T>> addOrGetSibling(node: V): V {
        while (true) {
            val head = siblings.get()
            if (head == null ) {
                if (siblings.compareAndSet(null, ListItem(node))) return node
            } else {
                var tail = head
                head.iterate {
                    tail = it
                    if (it.value == node) return it.value as V
                }
                if (tail.tryAppend(ListItem(node))) return node
            }
        }
    }

    private inline fun iterateSiblings(body: (Node<T>) -> Unit) = siblings.iterate(body)
}

class Method<T : Any> private constructor(private val name: String) : Node<T>() {
    companion object {
        public fun <T : Any> forName(name: String) = when (name) {
            "GET" -> Method<T>("GET")
            "HEAD" -> Method<T>("HEAD")
            "POST" -> Method<T>("POST")
            "PUT" -> Method<T>("PUT")
            "DELETE" -> Method<T>("DELETE")
            "OPTIONS" -> Method<T>("OPTIONS")
            "TRACE" -> Method<T>("TRACE")
            "CONNECT" -> Method<T>("CONNECT")
            else -> throw IllegalArgumentException("No HTTP method with name [$name]")
        }
    }

    override fun findValue(path: Path, mapping: Mapping): T? {
        siblings.iterate {
            val value = it.findValue(path, mapping)
            if (value != null) return value
        }
        return null
    }

    public fun hasName(name: String, ignoreCase: Boolean = true): Boolean {
        return this.name.equals(name, ignoreCase = ignoreCase)
    }

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other !is Method<*>) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class Directory<T : Any>(value: String, private val ignoreCase: Boolean = false) : Node<T>() {
    private val _value: String

    init {
        if (value.isBlank()) throw IllegalArgumentException("path can not be blank")
        _value = if (ignoreCase) value.toLowerCase() else value
    }

    override fun findValue(path: Path, mapping: Mapping): T? {
        if (_value.equals(path.value, ignoreCase)) {
            val subPath = path.subPath
            //TODO, query string
            if (subPath == null) {
                return value
            }
            siblings.iterate {
                val value = it.findValue(subPath, mapping)
                if (value != null) return value
            }

        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other !is Directory<*>) return false
        return _value == other._value
    }

    override fun hashCode(): Int = _value.hashCode()
}

class SingleWildcard<T : Any>() : Node<T>() {
    override fun equals(other: Any?): Boolean = other is SingleWildcard<*>
    override fun hashCode(): Int = 23

    override fun findValue(path: Path, mapping: Mapping): T? {
        val subPath = path.subPath
        //TODO, query string
        if (subPath == null) {
            return value
        }
        siblings.iterate {
            val value = it.findValue(subPath, mapping)
            if (value != null) return value
        }
        return null
    }
}

class MultiWildcard<T : Any>() : Node<T>() {
    override fun equals(other: Any?): Boolean = other is MultiWildcard<*>
    override fun hashCode(): Int = 31

    override fun findValue(path: Path, mapping: Mapping): T? {
        var localPath = path
        do {
            val subPath = localPath.subPath
            //TODO, query string
            if (subPath == null) {
                return value
            }
            siblings.iterate {
                val value = it.findValue(subPath, mapping)
                if (value != null) return value
            }
            localPath = subPath
        } while (subPath != null)
        return null
    }
}

class StringDirectoryValue<T : Any>(val identifier: String) : Node<T>() {
    init {
        if (identifier.isBlank()) throw IllegalArgumentException("identifier can not be blank")
    }

    override fun findValue(path: Path, mapping: Mapping): T? {
        val subPath = path.subPath
        //TODO, query string
        if (subPath == null) {
            if (value != null) {
                mapping.add(identifier, path.value)
            }
            return value
        }
        siblings.iterate {
            val value = it.findValue(subPath, mapping)
            if (value != null) {
                mapping.add(identifier, path.value)
                return value
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other !is StringDirectoryValue<*>) return false
        return identifier == other.identifier
    }

    override fun hashCode(): Int = identifier.hashCode()
}

class IntDirectoryValue<T : Any>(val identifier: String) : Node<T>() {
    init {
        if (identifier.isBlank()) throw IllegalArgumentException("identifier can not be blank")
    }

    override fun findValue(path: Path, mapping: Mapping): T? {
        IntValue.whenInt(path.value) {
            val subPath = path.subPath
            //TODO, query string
            if (subPath == null) {
                if (value != null) {
                    //TODO, create true IntValue container
                    mapping.add(identifier, it.toString())
                }
                return value
            }
            siblings.iterate {
                val value = it.findValue(subPath, mapping)
                if (value != null) {
                    //TODO, create true IntValue container
                    mapping.add(identifier, it.toString())
                    return value
                }
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this.identityEquals(other)) return true
        if (other !is IntDirectoryValue<*>) return false
        return identifier == other.identifier
    }

    override fun hashCode(): Int = identifier.hashCode()
}


private class ListItem<T>(val value: T) {
    val next = AtomicReference<ListItem<T>>(null)

    fun tryAppend(item: ListItem<T>): Boolean {
        val tail = getTailNode()
        return tail.next.compareAndSet(null, item)
    }

    private fun getTailNode(): ListItem<T> {
        var tail = this
        while (true) {
            val next = tail.next.get()
            if (next == null) return tail
            tail = next
        }
    }

    inline fun iterate(body: (ListItem<T>) -> Unit) {
        var node = this
        while (true) {
            body(node)
            val next = node.next.get()
            if (next == null) break
            node = next
        }
    }
}


private inline fun <T : Any, N : Node<T>> AtomicReference<ListItem<N>>.iterate(body: (N) -> Unit) = get()?.iterate { body(it.value) }





