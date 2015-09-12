package nl.komponents.routes

public interface Value {
    fun asString(): String
    fun asInt(): Int
    //long, double, etc
}

public class StringValue(private val value: String) : Value {
    override fun asString(): String = value

    override fun asInt(): Int {
        IntValue.whenInt(value) {
            return it
        }
        throw IllegalStateException("Value [$value] can not be converted to an Int")
    }
}

public class IntValue(private val value: Int) : Value {
    companion object {
        public inline fun whenInt(value: String, body: (Int) -> Unit) = try {
            body(Integer.parseInt(value))
        } catch (e: Exception) {
            //ignore
        }
    }

    override fun asInt(): Int = value
    override fun asString(): String = value.toString()


}
