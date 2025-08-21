package niko_SA.augments.jumpPoint

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.SA_miscUtils.getApproximateHyperspaceLoc
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class jumpPointCreator: stationAttachment() {
    var ourSide: JumpPointAPI? = null
    var hyperSide: JumpPointAPI? = null

    companion object {
        const val ACCESSIBILITY_INCREMENT = 0.2f
    }

    override fun applyInCombat(station: ShipAPI) {
        return
    }

    override fun apply() {
        super.apply()

        tryAddingJumpPoint()

        val stationIndustry = getStationIndustry() ?: return
        market?.accessibilityMod?.modifyFlat(id, ACCESSIBILITY_INCREMENT, "${stationIndustry.currentName}: ${getName()}")
    }

    override fun unapply() {
        super.unapply()

        if (!reapplying || getStationIndustry()?.isFunctional != true) {
            removeJumpPoint()
        }
        market?.accessibilityMod?.unmodify(id)
    }

    private fun removeJumpPoint() {
        ourSide?.isExpired = true
        hyperSide?.isExpired = true

        ourSide?.clearDestinations()
        hyperSide?.clearDestinations()

        ourSide = null
        hyperSide = null
    }

    private fun tryAddingJumpPoint() {
        if (canAddJumpPoint()) {
            addJumpPoint()
        }
    }

    private fun addJumpPoint() {
        val entity = getStationCampaignEntity() ?: return
        val orbitDays = (entity.radius * 0.2f)

        ourSide = Global.getFactory().createJumpPoint("${getBaseJumpPointId()}_NORMAL", "Manufactured Jump Point")
        if (market?.primaryEntity is PlanetAPI) {
            ourSide!!.relatedPlanet = market!!.primaryEntity
        }
        ourSide!!.setStandardWormholeToHyperspaceVisual()
        market!!.containingLocation!!.addEntity(ourSide)

        val startAngle = MathUtils.getRandomNumberInRange(0f, 360f)
        ourSide!!.setCircularOrbit(entity, startAngle, entity.radius, orbitDays)

        hyperSide = Global.getFactory().createJumpPoint("${getBaseJumpPointId()}_HYPER", entity.name + " Manufactured Jump Point")
        hyperSide!!.setStandardWormholeToStarOrPlanetVisual(entity)
        Global.getSector().hyperspace.addEntity(hyperSide)
        hyperSide?.location?.set(ourSide!!.getApproximateHyperspaceLoc())
        hyperSide?.addScript(SA_hyperEntityStaplingScript(toMove = hyperSide!!, target = ourSide!!))

        val fromSystemToHyper = JumpPointAPI.JumpDestination(hyperSide, "hyperspace")
        val fromHyperToSystem = JumpPointAPI.JumpDestination(ourSide, ourSide!!.name)

        ourSide?.addDestination(fromSystemToHyper)
        hyperSide?.addDestination(fromHyperToSystem)
    }

    private fun getBaseJumpPointId(): String {
        return "${id}_${market?.name}_jumpPoint"
    }

    private fun canAddJumpPoint(): Boolean {
        return getCantAddJumpPointReason() == null
    }

    private fun getCantAddJumpPointReason(): String? {
        if (ourSide != null || hyperSide != null) {
            return "A jump point is already deployed"
        }
        if (market == null) {
            return "This augment has no market"
        }
        if (market!!.containingLocation?.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER) == true || market!!.containingLocation?.jumpPoints?.isEmpty() == true) {
            return "Jump point impossible to create"
        }
        if (getStationIndustry()?.isFunctional != true) {
            return "Station damaged"
        }

        return null
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "An exceedingly rare modification, the \"jump engine\" is little more than a highly sophisticated set of drone-hangars " +
            "and management equipment to produce an artificial jump-point. Having been abandoned by its manufacturers for its potential to cause major " +
            "hyperspace ripples, its use is not recommended - but with the low traffic of the persean sector, it may be worth the now-low risk.",
            5f
        )

        tooltip.addPara(
            "Deploys a portal-drone that creates a %s in %s around the station.",
            5f,
            Misc.getHighlightColor(),
            "jump point", "tight orbit"
        )
        tooltip.addPara(
            "As a consequence, increases accessibility by %s.",
            5f,
            Misc.getHighlightColor(),
            "${(ACCESSIBILITY_INCREMENT * 100f).trimHangingZero()}%"
        )

        if (market != null) {
            val cantAddPointReason = getCantAddJumpPointReason()
            if (ourSide == null && cantAddPointReason != null) {
                tooltip.addSectionHeading("Complications", Alignment.MID, 5f)

                tooltip.addPara(
                    "The portal drone will be %s and %s for the following reason: %s",
                    5f,
                    Misc.getNegativeHighlightColor(),
                    "ineffective", "fail to deploy", cantAddPointReason
                )
            }
        }

        tooltip.addPara(
            "This augment cannot be installed in systems without any jump-points, due to the relative stability of the " +
            "local inter-dimensional barrier.",
            5f
        ).setColor(Misc.getGrayColor())
    }

    override fun getBlueprintValue(): Int {
        return 40000
    }
}