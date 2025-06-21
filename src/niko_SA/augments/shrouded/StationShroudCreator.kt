package niko_SA.augments.shrouded

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.combat.RiftLanceEffect
import com.fs.starfarer.api.impl.combat.dweller.BaseDwellerShipCreator
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud.DwellerShroudParams
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud.ShroudNegativeParticleFilter
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import org.lwjgl.util.vector.Vector2f
import kotlin.math.max

class StationShroudCreator(val masterAugment: ShroudedAugment): BaseDwellerShipCreator() {

    override fun initBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
    }

    override fun initAfterShipCreation(ship: ShipAPI?, id: String?) {
    }

    override fun initAfterShipAddedToCombatEngine(ship: ShipAPI?, id: String?) {
    }

    override fun initInCombat(ship: ShipAPI) {
        val shroud = createShroud(ship)
        //setOverloadColorAndText(ship, shroud);
        val color = Misc.setAlpha(Misc.setBrightness(shroud.getParams().flashFringeColor, 255), 255)
        ship.fluxTracker.overloadColor = color
    }

    var masterStation: ShipAPI? = null
    override fun modifyBaselineShroudParams(module: ShipAPI, params: DwellerShroudParams) {
        //params.maxOffset = ship.getCollisionRadius();
        var maxOffset = 100f
        var scale = 1f
        var overloadGlowScale = 1f
        when (module.hullSize) {
            HullSize.CAPITAL_SHIP -> {
                maxOffset = 150f
                scale = 1.75f
                overloadGlowScale = 0.75f
            }

            HullSize.CRUISER -> {
                maxOffset = 120f
                scale = 1.5f
                overloadGlowScale = 0.75f
            }

            HullSize.DESTROYER -> {
                maxOffset = 100f
                scale = 1.25f
                overloadGlowScale = 0.5f
            }

            HullSize.FRIGATE, HullSize.FIGHTER -> {
                overloadGlowScale = 0.5f
                maxOffset = 75f
            }

            HullSize.DEFAULT -> {}
        }
        if (module == masterStation) {
            maxOffset = 175f
            scale = 2f
        }
        //params.maxOffset = ship.getCollisionRadius();
        params.maxOffset = maxOffset * 0.5f
        params.initialMembers = 0
        params.baseMembersToMaintain = params.initialMembers

        params.memberExchangeClass = "SA_AUGMENTSHROUDID"
        params.memberExchangeRange = 2500f

        params.spawnOffsetMult = 0.75f

        var num = (module.collisionRadius * module.collisionRadius / 800).toInt()
        num = num.coerceAtLeast(5).coerceAtMost(100)
        params.baseMembersToMaintain = num
        params.initialMembers = num

        //			params.flashProbability = 0f;
//			params.flashFrequency = 0f;
//			params.alphaMult = 0.25f;
//			params.baseSpriteSize = 128f * 1.5f * 0.67f * 1.5f;

        //params.alphaMult = 0.25f;
        var numShroudMods = 0f
        for (augment in masterAugment.market!!.getStationAugments()) {
            val spec = augment.getSpec()
            if (Tags.SHROUDED in spec.knowledgeTags) {
                numShroudMods++
            }
        }

        params.alphaMult = 0.1f + (numShroudMods - 1f) * 0.1f
        if (params.alphaMult > 0.75f) params.alphaMult = 0.75f
        if (params.alphaMult < 0.1f) params.alphaMult = 0.25f

        params.baseSpriteSize *= scale


        //params.negativeParticleAlphaIntOverride = 100;
        params.negativeParticleSpeedCap = module.maxSpeedWithoutBoost + 100f
        params.negativeParticleColorOverride =
            RiftLanceEffect.getColorForDarkening(module.spriteAPI.averageColor)
        //params.negativeParticleSizeMult = 1.5f;
        params.negativeParticleSizeMult = scale

        params.negativeParticleAreaMult = module.collisionRadius / params.maxOffset


//			params.negativeParticleAlphaIntOverride = 50;
//			params.negativeParticleSizeMult = 0.5f;
//			params.negativeParticleNumBase *= 3;
        params.overloadGlowSizeMult *= overloadGlowScale
        params.overloadArcOffsetMult = params.negativeParticleAreaMult * 0.8f


//			params.overloadArcThickness *= 2f;
//			params.overloadArcCoreThickness *= 2f;
        params.generateOffsetAroundAttachedEntityOval = true

        //			params.offsetModifier = new SwarmMemberOffsetModifier() {
//				@Override
//				public void modifyOffset(SwarmMember p) {
//					p.offset.x *= 0.75f;
//				}
//			};
        params.negativeParticleFilter = object : ShroudNegativeParticleFilter {
            val master = masterStation!! // so when we clear, this isnt lost

            override fun isParticleOk(shroud: DwellerShroud, loc: Vector2f): Boolean {
                if (shroud.getAttachedTo() is ShipAPI) {
                    val ship = shroud.getAttachedTo() as ShipAPI
                    val targetingRadius = Misc.getTargetingRadius(loc, ship, false)
                    val dist = Misc.getDistance(ship.location, loc)
                    var pad = params.maxOffset
                    //pad = params.maxOffset
                    if (!(dist < targetingRadius + pad && dist > targetingRadius * 0.75f)) return false

                    for (childModule in master.childModulesCopy) {
                        if (childModule == ship) continue
                        val theirShroud = DwellerShroud.getShroudFor(childModule) ?: continue

                        val theirDist = Misc.getDistance(childModule.location, loc)
                        val theirRad = childModule.collisionRadius
                        if (theirDist <= theirShroud.params.maxOffset * 2f || theirDist <= (theirRad / 2)) {
                            return false
                        }
                    }

                    return true
                }
                return true
            }
        }
    }
}