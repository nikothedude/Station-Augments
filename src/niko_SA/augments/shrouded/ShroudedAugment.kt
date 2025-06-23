package niko_SA.augments.shrouded

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import niko_SA.SA_ids
import niko_SA.augments.core.stationAttachment

abstract class ShroudedAugment: stationAttachment(), EveryFrameScript {

    companion object {
        const val LUDDIC_CHURCH_HATE_DAYS_MIN = 15f
        const val LUDDIC_CHURCH_HATE_DAYS_MAX = 20f
    }

    val creator = StationShroudCreator(this)

    val churchHateInterval = IntervalUtil(LUDDIC_CHURCH_HATE_DAYS_MIN, LUDDIC_CHURCH_HATE_DAYS_MAX)

    override var apToMemberStrengthMult: Float = super.apToMemberStrengthMult * 1.2f // its somewhat powerful for its ap cost

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun apply() {
        super.apply()

        if (!Global.getSector().memoryWithoutUpdate.getBoolean(SA_ids.SA_didPathReactionToShroudedAugment)) {
            Global.getSector().addScript(this)
        }
    }

    override fun getUnavailableReason(): String? {
        if (market != null) {
            for (augment in market!!.getStationAugments()) {
                val spec = augment.getSpec()
                if (spec.knowledgeTags.contains(Tags.THREAT)) return "Incompatible with threat augments"
            }
        }

        return super.getUnavailableReason()
    }

    protected fun skipFluxUseWhenOverloadedOrVenting(): Boolean {
        return true
    }

    open fun deductFlux(ship: ShipAPI, fluxCost: Float): Boolean {
        if (skipFluxUseWhenOverloadedOrVenting() && ship.fluxTracker.isOverloadedOrVenting) {
            return true
        }
        if (!ship.fluxTracker.increaseFlux(fluxCost, false)) {
            return false
        }
        return true
    }

    override fun unapply() {
        super.unapply()

        Global.getSector().removeScript(this)
    }

    override fun advance(amount: Float) {
        if (Global.getSector().memoryWithoutUpdate.getBoolean(SA_ids.SA_didPathReactionToShroudedAugment)) { // sanity
            Global.getSector().removeScript(this)
            return
        }
        churchHateInterval.advance(Misc.getDays(amount))
        if (churchHateInterval.intervalElapsed()) {
            procChurchHate()
            Global.getSector().removeScript(this)
        }
    }

    private fun procChurchHate() {
        Global.getSector().memoryWithoutUpdate.set(SA_ids.SA_didPathReactionToShroudedAugment, true)
        Global.getSector().memoryWithoutUpdate.set("\$SA_waitingToDoPathReactionToShroudedAug", true)
    }

    override fun applyInCombat(station: ShipAPI) {
        applyGenericShroudedEffects(station)
    }

    override fun onRemoved() {
        super.onRemoved()

        churchHateInterval.elapsed = 0f
    }

    private fun applyGenericShroudedEffects(station: ShipAPI) {
        creator.masterStation = station
        creator.initInCombat(station)
        for (module in station.childModulesCopy) {
            creator.initInCombat(module)
        }
        creator.masterStation = null
    }

    override fun getBlueprintValue(): Int {
        return 350000
    }
}