package niko_SA

object SA_mathUtils {
    fun Float.trimHangingZero(): Number {
        if (this % 1 == 0f) return this.toInt()
        return this
    }
}