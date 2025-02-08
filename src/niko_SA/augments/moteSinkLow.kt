package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI

class moteSinkLow(market: MarketAPI?, id: String): moteSink(market, id) {
    override val augmentCost: Float
        get() {
            return 15f // i really need to just make this a method...
        }
    override val name: String = "Mote Sink"
    override val highVolatility = false

    override fun getBlueprintValue(): Int {
        return 500000
    }

    override fun getUnavailableReason(): String? {
        val superString = super.getUnavailableReason()
        if (superString != null) return superString

        if (applied) return null
        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$SA_moteSinkInstalled")) {
            return "Rift-core installed in another station" // only one at a time mf
        }

        return null
    }

    override fun apply() {
        super.apply()

        Global.getSector().memoryWithoutUpdate["\$SA_moteSinkInstalled"] = true
    }

    override fun unapply() {
        super.unapply()

        Global.getSector().memoryWithoutUpdate["\$SA_moteSinkInstalled"] = false
    }
}