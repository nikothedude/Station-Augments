package niko_SA.augments.core

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.CoreAutoresolveListener
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl
import com.fs.starfarer.api.impl.campaign.HullModItemManager
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getAugmentBudget
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getUsedAugmentBudget
import niko_SA.MarketUtils.removeStationAugment
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.SA_settings
import niko_SA.SA_settings.ALLOW_FP_RATIO_VIEWING
import niko_SA.SA_settings.ALLOW_MODIFY_OF_ALL_STATIONS
import niko_SA.codex.CodexData.getAugmentEntryId
import org.magiclib.kotlin.getStorageCargo
import java.awt.Color
import kotlin.math.abs

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
    var considerReqItem = true

    @Transient
    var gettingDescFromBlueprint = false

    /** If an augment with this id in this set is present, the augment cannot be built. */
    open val incompatibleAugments: MutableSet<String> = HashSet()

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
        const val BEST_OF_THE_BEST_AP_BONUS = 10f // also arbitrary

        /** Additive atop BASE_STATION_AUGMENT_BUDGET. */
        @JvmStatic
        val tagToExtraAugmentBudget = hashMapOf(
            Pair(Industries.BATTLESTATION, 10f),
            Pair(Industries.STARFORTRESS, 20f),
            Pair("starcitadel", 10f), // aotd
        )

        fun removeRequiredItem(itemId: String, dockedAt: MarketAPI?) {
            val fleet = Global.getSector().playerFleet ?: return
            val cargo = fleet.cargo!!

            if (cargo.getQuantity(CargoItemType.SPECIAL, SpecialItemData(itemId, null)) >= 1) {
                cargo.removeItems(
                    CargoItemType.SPECIAL,
                    SpecialItemData(itemId, null),
                    1f
                )
                return
            }

            if (dockedAt != null) {
                val dockedCargo = dockedAt.getStorageCargo()
                if (dockedCargo == null) return
                if (dockedCargo.getQuantity(CargoItemType.SPECIAL, SpecialItemData(itemId, null)) >= 1) {
                    dockedCargo.removeItems(
                        CargoItemType.SPECIAL,
                        SpecialItemData(itemId, null),
                        1f
                    )
                    return
                }
            }
        }

        fun addRequiredItem(itemId: String, dockedAt: MarketAPI?) {
            val fleet = Global.getSector().playerFleet ?: return
            val cargo = fleet.cargo!!

            cargo.addSpecial(
                SpecialItemData(
                    itemId,
                    null
                ), 1f
            )

            /*if (dockedAt != null) {
                val dockedCargo = dockedAt.getStorageCargo()
                if (dockedCargo == null) return
                if (dockedCargo.getQuantity(CargoItemType.SPECIAL, itemId) >= 1) {
                    dockedCargo.removeItems(
                        CargoItemType.SPECIAL,
                        itemId,
                        1f
                    )
                    return
                }
            }*/
        }
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

    /** Called once when the augment is added to the market. */
    open fun onAdded() {
        market?.getStationAugments() += this
        apply()

        val reqId = getRequiredItemId()
        if (applied && considerReqItem && (ALLOW_MODIFY_OF_ALL_STATIONS || market?.isPlayerOwned == true) && reqId != null) {
            var dockedAtMarket = if (market != null && Global.getSector().playerFleet?.interactionTarget == market) market else null

            removeRequiredItem(reqId, dockedAtMarket)
        }
    }

    open fun apply() {
        applied = true
        Global.getSector().addListener(this)
        Global.getSector().listenerManager.addListener(this, false)
        Global.getSector().addScript(ConstantStationCheckingScript(this))
        doEnabledCheck() // just in case
    }

    /** Called once when the augment is removed from the market. */
    open fun onRemoved() {
        market?.getStationAugments() -= this
        unapply()

        val reqId = getRequiredItemId()
        if (considerReqItem && reqId != null && (ALLOW_MODIFY_OF_ALL_STATIONS || market?.isPlayerOwned == true)) {
            var dockedAtMarket = if (market != null && Global.getSector().playerFleet?.interactionTarget == market) market else null

            addRequiredItem(reqId, dockedAtMarket)
        }
    }

    open fun unapply() {
        applied = false
        Global.getSector().removeListener(this)
        Global.getSector().listenerManager.removeListener(this)
    }

    fun doEnabledCheck() {
        considerAP = false
        considerEngagement = false
        considerReqItem = false
        if (!canBeModifiedOrBuilt()) {
            considerReqItem = true
            market?.removeStationAugment(this)
        }
        considerReqItem = true
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
        val reqId = getRequiredItemId()
        if (reqId != null && considerReqItem) {
            val stack = Global.getSettings().createCargoStack(
                CargoItemType.SPECIAL,
                SpecialItemData(getRequiredItemId(), null), null
            )
            val available = HullModItemManager.getInstance().getNumAvailable(stack, market)
            if (!applied && available <= 0) {
                val spec = Global.getSettings().getSpecialItemSpec(getRequiredItemId())
                val name = spec.name
                val aOrAnd = Misc.getAOrAnFor(name)
                return "No $name"
            }
        }
        if (stationTypeWhitelist.isNotEmpty() && !stationTypeWhitelist.contains(station.spec.id)) {
            return "Requires ${getNeededStationTypeText()}"
        }
        if (considerAP && (station.getRemainingAugmentBudget() < getAugmentCost())) return "Not enough augment points to install"
        if (market?.getStationAugments()?.any { existingAugment -> existingAugment != this && (incompatibleAugments.contains(existingAugment.id) || existingAugment.incompatibleAugments.contains(id)) } == true ) {
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

    open fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        val orbitalStation = getStationIndustry()
        val augmentCost = getAugmentCost()
        val augmentCostAbs = abs(getAugmentCost())
        if (orbitalStation != null) {
            val initialString = if (augmentCost >= 0f) "This augment costs %s AP to install" else "This augment increases the AP budget by %s"
            val remainingAugmentBudget = orbitalStation.getRemainingAugmentBudget()
            val para = tooltip.addPara(
                "$initialString. The ${orbitalStation.currentName} currently has %s AP remaining. " +
                        "AP can be increased by upgrading the station, or by improving it with story points (%s).",
                5f,
                Misc.getHighlightColor(),
                "${augmentCostAbs.trimHangingZero()}",
                "${remainingAugmentBudget.trimHangingZero()}",
                "${STATION_IMPROVED_AP_BONUS.trimHangingZero()} AP"
            )
            if (orbitalStation.isImproved) {
                tooltip.addPara(
                    "The ${orbitalStation.currentName} has been improved, increasing its AP by %s.",
                    5f,
                    Misc.getStoryOptionColor(),
                    "${STATION_IMPROVED_AP_BONUS.trimHangingZero()}"
                )
            }
            if (market == null || market!!.isPlayerOwned) {
                val hasBOTB = Global.getSector().playerPerson.stats.hasSkill(Skills.BEST_OF_THE_BEST)
                if (hasBOTB) {
                    tooltip.addPara(
                        "You have %s, increasing AP by %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "best of the best", "${BEST_OF_THE_BEST_AP_BONUS.trimHangingZero()}"
                    )
                } else {
                    tooltip.addPara(
                        "Additionally, the skill %s can increase AP by %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "best of the best", "${BEST_OF_THE_BEST_AP_BONUS.trimHangingZero()}"
                    )
                }
            }
            val augmentBudgetColor =
                if (remainingAugmentBudget < getAugmentCost()) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
            para.setHighlightColors(Misc.getHighlightColor(), augmentBudgetColor, Misc.getStoryOptionColor())
            builtInMode.createDesc(tooltip)
        }

        if (ALLOW_FP_RATIO_VIEWING) {
            tooltip.addPara(
                "The augment currently has a AP-to-FP autoresolve ratio of %s. Note this does not always tell the whole story - some augments manually " +
                        "apply their FP changes.",
                5f,
                Misc.getHighlightColor(),
                "${apToMemberStrengthMult}x"
            )
        }

        if (!gettingDescFromBlueprint) {
            tooltip.addSectionHeading("Augment Info", Alignment.MID, 10f)
            tooltip.addSpacer(5f)
            Misc.addDesignTypePara(tooltip, getSpec().manufacturer, 5f)
        }

        if (market == null) {
            val initialString = if (augmentCost >= 0f) "This augment nominally costs %s to install." else "This augment nominally increases AP by %s."

            tooltip.addPara(initialString, 10f, Misc.getHighlightColor(), "${augmentCostAbs.trimHangingZero()} AP")
        }

        if (getRequiredItemId() != null) {
            val spec = Global.getSettings().getSpecialItemSpec(getRequiredItemId())
            val name = spec.name
            val aOrAnd = Misc.getAOrAnFor(name)

            if (gettingDescFromBlueprint || Global.getSettings().isShowingCodex) {
                tooltip.addPara("Requires $aOrAnd $name", 5f).color = Misc.getNegativeHighlightColor()
            } else if (applied) {
                tooltip.addPara("Using $aOrAnd $name", 5f).color = Misc.getPositiveHighlightColor()
            } else {
                val stack = Global.getSettings().createCargoStack(
                    CargoItemType.SPECIAL,
                    SpecialItemData(getRequiredItemId(), null), null
                )
                val available = HullModItemManager.getInstance().getNumAvailable(stack, market)
                tooltip.addPara("Requires $aOrAnd $name ($available available)", 5f).color = Misc.getNegativeHighlightColor()
            }

            tooltip.addSpacer(5f)
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
    open fun getNameColor(): Color {
        return getSpec().nameColor
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

    fun getRequiredItemId(): String? = getSpec().requiredItemId

    fun getAPChangeInapplicableReason(newAp: Float): String? {
        if (getStationIndustry() == null) return null
        val apTotal = getStationIndustry()!!.getUsedAugmentBudget() - getAugmentCost()
        if ((apTotal + newAp) > getStationIndustry()!!.getAugmentBudget()) {
            return "Exceeds maximum AP cost"
        }
        return null
    }

    fun stationHasDrones(): Boolean {
        if (Global.getSettings().isGeneratingNewGame) return true // station fleets dont exist til later
        val fleet = getStationFleet() ?: return false
        val flagship = fleet.fleetData.membersListCopy.firstOrNull() ?: return false
        val sysId = flagship.hullSpec.shipSystemId ?: return false
        val system = Global.getSettings().getShipSystemSpec(sysId) ?: return false
        if (system.maxDrones > 0f) return true

        return false
    }

    open fun modifyAugmentMenu(tooltip: TooltipMakerAPI, panel: CustomPanelAPI?, buttonPanel: CustomPanelAPI?, delegate: AugmentMenuDialogueDelegate) {

    }

    open fun getIdealButtonWidth(panel: CustomPanelAPI?): Float = 595.0f
    open fun getIdealButtonHeight(panel: CustomPanelAPI?): Float = 86.0f

    open fun sortInUIAgainst(other: stationAttachment): Int {
        return getName().compareTo(other.getName(), true)
    }

    class ConstantStationCheckingScript(val augment: stationAttachment): EveryFrameScript {
        var done = false
        override fun isDone(): Boolean = done

        override fun runWhilePaused(): Boolean = true

        override fun advance(amount: Float) {
            val market = augment.market
            if (!Global.getSector().campaignUI.isShowingDialog || (market != null && !market.getStationAugments().contains(augment))) {
                done = true
                return
            }

            augment.doEnabledCheck()
        }
    }
}