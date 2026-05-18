package mirujam.nekomemo.ui.model

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    data class StringResource(
        @StringRes val resId: Int,
        val args: Array<Any> = emptyArray()
    ) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as StringResource

            if (resId != other.resId) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UiText) return false
        return when (this) {
            is DynamicString -> other is DynamicString && value == other.value
            is StringResource -> other is StringResource && resId == other.resId && args.contentEquals(other.args)
        }
    }

    override fun hashCode(): Int {
        return when (this) {
            is DynamicString -> value.hashCode()
            is StringResource -> 31 * resId + args.contentHashCode()
        }
    }
}
