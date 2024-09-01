package niko_SA

object stringUtils {
    fun toPercent(num: Float): String {
        return String.format("%.0f", num * 100) + "%"
    }
}