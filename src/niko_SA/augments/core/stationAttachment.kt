package niko_SA.augments.core

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.removeStationAugment
import niko_SA.ReflectionUtils

/** Industries of this type attempt to modify an existing station in combat, and potentially, in campaign.*/
abstract class stationAttachment(val market: MarketAPI?, val id: String): BaseCampaignEventListener(true) {

    /** Domain restricted, Ko combine, etc... */
    open val manufacturer: String = "Common"

    /** The "cost" to be subtracted from our stations augment budget. We cannot be built if our station doesnt have enough budget for us. */
    abstract val augmentCost: Float
    var reapplying = false
    /** Have we been applied to our market yet? */
    var applied = false

    /** We can only be built on stations with these industry ids. If empty, is ignored. */
    open val stationTypeWhitelist = HashSet<String>()
    var considerAP = true // used in [isAvailableToBuild]
    var considerEngagement = true

    abstract val name: String
    abstract val spriteId: String
    /** If an augment with this id in this set is present, the augment cannot be built. */
    val incompatibleAugments: MutableSet<String> = HashSet()

    companion object {
        const val stationImprovedAPBonus = 10f // arbitrary
        /** Additive atop BASE_STATION_AUGMENT_BUDGET. */
        @JvmStatic
        val tagToExtraAugmentBudget = hashMapOf(
            Pair(Industries.BATTLESTATION, 10f),
            Pair(Industries.STARFORTRESS, 20f)
        )

        /*/** Returns the augment budget this station has. An augment budget controls how many augments a station can have - each augment has its own cost.*/
        fun OrbitalStation.getAugmentBudget(): Float {
            var points = BASE_STATION_AUGMENT_BUDGET

            for (tag in spec.tags) {
                val tagBonus = tagToExtraAugmentBudget[tag]
                if (tagBonus != null) {
                    points += tagBonus
                }
            }
            return points
        }

        fun MarketAPI.getUsedAugmentBudget(): Float {
            var used = 0f

            for (augment in getStationAugments()) {
                used += augment.augmentCost
            }

            return used
        }

        fun OrbitalStation.getUsedAugmentBudget(): Float {
            return market.getUsedAugmentBudget()
        }

        fun OrbitalStation.getRemainingAugmentBudget(): Float {
            val budget = getAugmentBudget()
            val usedBudget = getUsedAugmentBudget()

            return (budget - usedBudget)
        }

        fun MarketAPI.getStationAugments(): MutableSet<stationAttachment> {
            val augments = HashSet<stationAttachment>()

            for (industry in industries) {
                if (!industry.isFunctional) continue
                if (industry.spec.hasTag(SA_ids.SA_structureTag)) {
                    augments += (industry as stationAttachment)
                }
            }

            return augments
        }*/
    }

    /** Ran once at the beginning of combat. */
    abstract fun applyInCombat(station: ShipAPI)

    fun reapply() {
        reapplying = true
        unapply()
        apply()
        reapplying = false
    }

    open fun apply() {
        applied = true
        Global.getSector().addListener(this)
        Global.getSector().addScript(ConstantStationCheckingScript(this)) // just in case
        doEnabledCheck()
    }

    open fun unapply() {
        applied = false
        Global.getSector().removeListener(this)
    }

    fun doEnabledCheck() {
        considerAP = false
        considerEngagement = false
        if (!canBeModifiedOrBuilt()) {
            market?.removeStationAugment(this)
        }
        considerAP = true
        considerEngagement = true


    }

    open fun canBeModifiedOrBuilt(): Boolean {
        return (getUnavailableReason() == null)
    }

    open fun getUnavailableReason(): String? {
        val station = getStationIndustry() ?: return "No orbital station"
        if (considerEngagement && getStationFleet()?.battle != null) return "Station currently engaged"
        if (stationTypeWhitelist.isNotEmpty() && !stationTypeWhitelist.contains(station.spec.id)) {
            return "Requires ${getNeededStationTypeText()}"
        }
        if (considerAP && (station.getRemainingAugmentBudget() < augmentCost)) return "Not enough augment points to install"
        if (incompatibleAugments.isNotEmpty() && market?.getStationAugments()?.any { existingAugment -> existingAugment != this && (incompatibleAugments.contains(existingAugment.id) || existingAugment.incompatibleAugments.contains(id)) } == true ) {
            return "Incompatible with existing augments"
        }
        return null
    }

    /** Only needed if [stationTypeWhitelist] is not empty. */
    open fun getNeededStationTypeText(): String {
        return "error! please report to the mod author"
    }

    /** Returns the orbital station industry instance. Required to not be null for us to be buildable.*/
    fun getStationIndustry(): OrbitalStation? {
        if (market == null) return null

        for (industry in market.industries) {
            if (industry.spec.hasTag(Tags.STATION)) {
                return industry as OrbitalStation
            }
        }
        return null
    }

    /** Uses reflection - expensive. */
    fun getStationFleet(): CampaignFleetAPI? {
        val stationIndustry = getStationIndustry() ?: return null
        return ReflectionUtils.get("stationFleet", stationIndustry, OrbitalStation::class.java) as? CampaignFleetAPI
    }

    /** Returns the in-combat station entity we are affecting. Returns null if we're not in combat, or it doesnt exist. */
    fun getStationCombatEntity(): ShipAPI? {
        if (Global.getCurrentState() != GameState.COMBAT) return null
        val engine = Global.getCombatEngine()

        for (ship in engine.ships) {
            if (ship.isStation) {
                val member = ship.fleetMember ?: continue
                val fleet = member.fleetData?.fleet ?: continue
                if (fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] == market) {
                    return ship
                }
            }
        }
        return null
    }

    fun getStationCampaignEntity(): SectorEntityToken? {
        val stationIndustry = getStationIndustry() ?: return null
        return ReflectionUtils.get("stationEntity", stationIndustry, OrbitalStation::class.java) as? SectorEntityToken
    }

    open fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val orbitalStation = getStationIndustry() ?: return
        val remainingAugmentBudget = orbitalStation.getRemainingAugmentBudget()
        val para = tooltip.addPara(
            "This augment costs %s AP to install. The ${orbitalStation.currentName} currently has %s AP remaining. " +
            "AP can be increased by upgrading the station, or by improving it with story points (%s).",
            5f,
            Misc.getHighlightColor(),
            "$augmentCost", "$remainingAugmentBudget", "$stationImprovedAPBonus AP"
        )
        val augmentBudgetColor = if (remainingAugmentBudget < augmentCost) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
        para.setHighlightColors(Misc.getHighlightColor(), augmentBudgetColor)
    }

    open fun getImageName(market: MarketAPI): String {
        return spriteId
    }

    open fun canAfford(): Boolean {
        return true
    }

    override fun reportEconomyTick(iterIndex: Int) {
        super.reportEconomyTick(iterIndex)

        if (applied) {
            reapply()
        }
        //doEnabledCheck()
    }

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        super.reportPlayerOpenedMarket(market)

        if (applied && market == this.market) {
            reapply()
            Global.getSector().addScript(ConstantStationCheckingScript(this))
        }
        //doEnabledCheck()
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        super.reportPlayerClosedMarket(market)

        if (applied) {
            reapply()
            //doEnabledCheck()
        }
    }

    open fun getBlueprintValue(): Int {
        return 5000
    }

    /** The chance for this augment to drop in combat, assuming our station was destroyed. 0-100. */
    open fun getCombatDropChance(): Float {
        return 20f
    }

    class ConstantStationCheckingScript(val augment: stationAttachment): EveryFrameScript {
        var done = false
        override fun isDone(): Boolean = done

        override fun runWhilePaused(): Boolean = true

        override fun advance(amount: Float) {
            if (!Global.getSector().campaignUI.isShowingDialog) {
                done = true
                return
            }

            augment.doEnabledCheck()
        }
    }
}