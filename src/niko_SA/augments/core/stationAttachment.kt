package niko_SA.augments.core

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.CoreAutoresolveListener
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.removeStationAugment
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.codex.CodexData.getAugmentEntryId

/** Industries of this type attempt to modify an existing station in combat, and potentially, in campaign.*/
abstract class stationAttachment() : BaseCampaignEventListener(false), CoreAutoresolveListener {

    /** If null, exists in the codex or a blueprint. */
    var market: MarketAPI? = null
    lateinit var id: String

    @Transient
    var reapplying = false
    /** Have we been applied to our market yet? */
    var applied = false

    /** We can only be built on stations with these industry ids. If empty, is ignored. */
    open val stationTypeWhitelist = HashSet<String>()
    @Transient
    var considerAP = true // used in [isAvailableToBuild]
    @Transient
    var considerEngagement = true
    @Transient
    var gettingDescFromBlueprint = false

    /** If an augment with this id in this set is present, the augment cannot be built. */
    val incompatibleAugments: MutableSet<String> = HashSet()

    var builtInMode = BuiltInMode.NOT
        get() {
            if (field == null) field = BuiltInMode.NOT
            return field
        }

    open var apToMemberStrengthMult = BASE_AP_TO_MEMBER_STRENGH_MULT
        get() {
            if (field == null) {
                field = BASE_AP_TO_MEMBER_STRENGH_MULT
            }
            return field
        }

    companion object {
        const val BASE_AP_TO_MEMBER_STRENGH_MULT = 1.2f // arbitrary
        const val STATION_IMPROVED_AP_BONUS = 10f // arbitrary
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
    var initialized = false
    /** Called directly after creation, in [stationAugmentSpec.getNewPluginInstance]. Called BEFORE apply(). */
    open fun init() {
        initialized = true
    }

    /** Ran once at the beginning of combat. */
    abstract fun applyInCombat(station: ShipAPI)

    open fun readResolve(): Any {
        return this
    }

    fun reapply() {
        reapplying = true
        unapply()
        apply()
        reapplying = false
    }

    open fun apply() {
        applied = true
        Global.getSector().addListener(this)
        Global.getSector().listenerManager.addListener(this, false)
        Global.getSector().addScript(ConstantStationCheckingScript(this)) // just in case
        doEnabledCheck()
    }

    open fun unapply() {
        applied = false
        Global.getSector().removeListener(this)
        Global.getSector().listenerManager.removeListener(this)
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

    /** If null is returned, the game will assume this augment CAN be added to a station. */
    open fun getUnavailableReason(): String? {
        val station = getStationIndustry() ?: return "No orbital station"
        if (considerEngagement && getStationFleet()?.battle != null) return "Station currently engaged"
        if (stationTypeWhitelist.isNotEmpty() && !stationTypeWhitelist.contains(station.spec.id)) {
            return "Requires ${getNeededStationTypeText()}"
        }
        if (considerAP && (station.getRemainingAugmentBudget() < getAugmentCost())) return "Not enough augment points to install"
        if (incompatibleAugments.isNotEmpty() && market?.getStationAugments()?.any { existingAugment -> existingAugment != this && (incompatibleAugments.contains(existingAugment.id) || existingAugment.incompatibleAugments.contains(id)) } == true ) {
            return "Incompatible with existing augments"
        }
        return null
    }

    /** Only needed if [stationTypeWhitelist] is not empty. */
    open fun getNeededStationTypeText(): String {
        return ""
    }

    /** Returns the orbital station industry instance. Required to not be null for us to be buildable.*/
    fun getStationIndustry(): OrbitalStation? {
        if (market == null) return null
        return Misc.getStationIndustry(market) as? OrbitalStation
    }

    fun getStationFleet(): CampaignFleetAPI? {
        val stationIndustry = getStationIndustry() ?: return null
        return stationIndustry.stationFleet
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
        if (market?.primaryEntity?.hasTag(Tags.STATION) == true) return market!!.primaryEntity
        return getStationIndustry()?.stationEntity
    }

    open fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val orbitalStation = getStationIndustry()
        if (orbitalStation != null) {
            val remainingAugmentBudget = orbitalStation.getRemainingAugmentBudget()
            val para = tooltip.addPara(
                "This augment costs %s AP to install. The ${orbitalStation.currentName} currently has %s AP remaining. " +
                        "AP can be increased by upgrading the station, or by improving it with story points (%s).",
                5f,
                Misc.getHighlightColor(),
                "${getAugmentCost().trimHangingZero()}",
                "${remainingAugmentBudget.trimHangingZero()}",
                "${STATION_IMPROVED_AP_BONUS.trimHangingZero()} AP"
            )
            if (orbitalStation.isImproved) {
                tooltip.addPara(
                    "The ${orbitalStation.currentName} has been improved, increasing it's AP by %s.",
                    5f,
                    Misc.getStoryOptionColor(),
                    "${STATION_IMPROVED_AP_BONUS.trimHangingZero()}"
                )
            }
            val augmentBudgetColor =
                if (remainingAugmentBudget < getAugmentCost()) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
            para.setHighlightColors(Misc.getHighlightColor(), augmentBudgetColor, Misc.getStoryOptionColor())
            builtInMode.createDesc(tooltip)
        }

        if (!gettingDescFromBlueprint) {
            tooltip.addSectionHeading("Augment Info", Alignment.MID, 10f)
            tooltip.addSpacer(5f)
            Misc.addDesignTypePara(tooltip, getSpec().manufacturer, 5f)
        }

        if (market == null) {
            tooltip.addPara("This augment nominally costs %s to install.", 10f, Misc.getHighlightColor(), "${getAugmentCost().trimHangingZero()} AP")
        }

        val stationRequiredString = getNeededStationTypeText()
        if (stationRequiredString.isNotEmpty()) {
            tooltip.addPara("Requires ${stationRequiredString}.", 5f).color = Misc.getGrayColor()
        }

        if (!Global.getSettings().isShowingCodex) {
            val spec = getSpec()
            if (spec.codexTags.contains(Tags.HIDE_IN_CODEX)) return
            tooltip.codexEntryId = getAugmentEntryId(id)
        }
    }

    open fun getImageName(market: MarketAPI? = null): String {
        return getSpec().spritePath
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
        return getSpec().dropCombatWeight
    }

    override fun modifyDataForFleet(data: BattleAutoresolverPluginImpl.FleetAutoresolveData?) {
        if (data == null) return
        val fleet = getStationFleet() ?: return
        if (fleet.fleetData.membersListCopy.isEmpty() || data.fleet.fleetData.membersListCopy.isEmpty()) return
        if (fleet.fleetData.membersListCopy[0] == data.fleet.fleetData.membersListCopy[0]) {
            modifyAutoresolveForOurFleet(data)
        }
    }

    // TODO: flesh this out
    open fun modifyAutoresolveForOurFleet(data: BattleAutoresolverPluginImpl.FleetAutoresolveData) {
        val ourMember = data.members.firstOrNull { it.member.isStation } ?: return
        if (getSpec().usageTags.none { it.contains("combat") }) return
        val ap = getAugmentCost()
        val bonus = if (isDetrimentalToCombat()) ap / apToMemberStrengthMult else ap * apToMemberStrengthMult

        ourMember.strength += bonus
    }

    open fun isDetrimentalToCombat(): Boolean {
        val usageTags = getSpec().usageTags
        return (usageTags.none { it.contains("combatgood") } && usageTags.any { it.contains("combatbad") })
    }

    fun isSmodded(): Boolean = (builtInMode == BuiltInMode.SMOD)
    open fun canBeRemoved(): Boolean = (builtInMode == BuiltInMode.NOT)

    open fun getName(): String {
        return getSpec().name
    }
    open fun getAugmentCost(): Float {
        return getSpec().apCost
    }

    fun getSpec(): stationAugmentSpec {
        return stationAugmentStore.allAugments[id]!!
    }

    fun getKnowledgeTags(): MutableSet<String> {
        return getSpec().knowledgeTags
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