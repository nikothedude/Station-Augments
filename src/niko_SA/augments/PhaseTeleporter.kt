/*package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.combat.ShipSystemSpecAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.combat.ai.BasicShipAI
import com.fs.starfarer.combat.ai.D
import com.fs.starfarer.combat.ai.attack.AttackAIModule
import com.fs.starfarer.combat.ai.movement.A
import com.fs.starfarer.combat.ai.movement.maneuvers.M
import com.fs.starfarer.combat.ai.system.C
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.loading.specs.`do`
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAITacticalModule
import niko_SA.augments.core.stationAttachment
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.MagicSubsystem
import org.magiclib.subsystems.MagicSubsystemsManager
import niko_SA.ReflectionUtils
import niko_SA.ReflectionUtils.get

class PhaseTeleporter: stationAttachment() {
    override fun applyInCombat(station: ShipAPI) {
        MagicSubsystemsManager.addSubsystemToShip(station, PhaseTeleporterAugmentSubsys(station))
    }

    class PhaseTeleporterAugmentSubsys(station: ShipAPI): MagicSubsystem(station) {
        private val systemSpec: com.fs.starfarer.loading.specs.`do` = Global.getSettings().getShipSystemSpec("phaseteleporter") as `do`
        private val system: ShipSystemAPI = com.fs.starfarer.combat.systems.int(
            ship as Ship,
            Global.getSettings().getShipSystemSpec("phaseteleporter") as `do`?,
            false
        ) as ShipSystemAPI
        val systemAI: com.fs.starfarer.combat.ai.system.C? = createSystemAI()

        private fun createSystemAI(): C? {
            val threatEvalAI = get("threatEvalAI", ship.ai, BasicShipAI::class.java)
            val attackAI = get("attackAI", ship.ai, BasicShipAI::class.java)
            val flockingAI = get("flockingAI", ship.ai, BasicShipAI::class.java)

            val ai = systemSpec.createSystemAI(
                ship as Ship,
                ship.shipAI.aiFlags,
                threatEvalAI as D?,
                attackAI as AttackAIModule,
                flockingAI as com.fs.starfarer.combat.ai.movement.A?,
                ship.ai as M.o?
            ) as C
            ReflectionUtils.set("if.super", ai, system)

            return ai
        }

        private val systemStats = ReflectionUtils.get("ø00000", system, com.fs.starfarer.combat.systems.F::class.java) as? ShipSystemStatsScript
        val uncastedSystem = system as com.fs.starfarer.combat.systems.int

        override fun hasCharges(): Boolean {
            return getMaxCharges() > 0
        }

        public override fun getMaxCharges(): Int {
            var uses = systemStats?.getUsesOverride(ship) ?: -1
            if (uses < 0) {
                uses = systemSpec.getMaxUses(ship.mutableStats)
            }
            return uses
        }

        override fun getBaseChargeRechargeDuration(): Float {
            var regen = -1f
            if (regen < 0) {
                regen = systemSpec.getRegen(ship.mutableStats)
            }
            return regen
        }

        override fun shouldActivateAI(amount: Float): Boolean {
            /*val playerShip = Global.getCombatEngine().playerShip ?: return false
            val loc = playerShip.location ?: return false

            ship.aiFlags.setFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM, 0f, Vector2f(loc))*/

            /*val aliveModules = ship.childModulesCopy.filter { it.isAlive }
            val combinedModules = (aliveModules + ship).filter { it.isAlive }

            for (module in ship.childModulesCopy + niko_MPC_satelliteFleetAITacticalModule)*/

            val target = ship.childModulesCopy.find { it.shipTarget != null }?.shipTarget ?: return false
            val oldFixedLoc = ship.fixedLocation
            ship.fixedLocation = null
            ship.mutableStats.maxSpeed.modifyFlat("TEST", 100f)
            ship.mutableStats.acceleration.modifyFlat("TEST", 100f)

            val flockingAI = get("flockingAI", ship.ai, BasicShipAI::class.java) as com.fs.starfarer.combat.ai.movement.A
            flockingAI.Ô00000(20f)
            val posOne = flockingAI.Õ00000()
            val posTwo = flockingAI.Ø00000()

            ship.mutableStats.maxSpeed.unmodify("TEST")
            ship.mutableStats.acceleration.unmodify("TEST")


            if (systemAI?.Ô00000(target as Ship) == true || systemAI?.Ò00000(target as Ship, posOne, posTwo) == true || systemAI?.o00000(target as Ship, posOne, posTwo) == true) {
                ship.mutableStats.systemRangeBonus.modifyMult("SA_INFINITEFUCKINGTELEPORTERRANGE", 999f)
                val commands = (ship as Ship).commands
                val command = (commands.firstOrNull { it.Ò00000 == com.fs.starfarer.combat.entities.Ship.oo.Õ00000 }) ?: return false
                commands -= command
                val custom = command.Ó00000
                val useSystemLoc = custom as? Vector2f ?: return false
                uncastedSystem.fire(Vector2f(useSystemLoc))
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
                //ReflectionUtils.set("targetLoc", system, Vector2f(loc), com.fs.starfarer.combat.systems.F::class.java)
                //ReflectionUtils.set("ö00000", system, true, com.fs.starfarer.combat.systems.F::class.java)

                //ReflectionUtils.invoke("useSystem", system, declared = false)\
                ship.fixedLocation = Vector2f(useSystemLoc)

                return true
            }
            ship.fixedLocation = oldFixedLoc
            return false
        }

        override fun getBaseInDuration(): Float {
            var `in` = -1f
            if (`in` < 0) {
                `in` = systemSpec.getIn()
            }
            return `in`
        }

        override fun getBaseActiveDuration(): Float {
            var active = -1f
            if (active < 0) {
                active = systemSpec.active
            }
            return active
        }

        override fun getBaseOutDuration(): Float {
            var active = -1f
            if (active < 0) {
                active = systemSpec.out
            }
            return active
        }

        override fun getBaseCooldownDuration(): Float {
            return systemSpec.getCooldown(stats)
        }

        override fun getDisplayText(): String? {
            var name = system.displayName
            if (name == null) {
                name = systemSpec.name
            }
            return name
        }

        private fun translateState(CAstate: State?): ShipSystemStatsScript.State? {
            if (CAstate == State.ACTIVE) return ShipSystemStatsScript.State.ACTIVE
            if (CAstate == State.IN) return ShipSystemStatsScript.State.IN
            if (CAstate == State.OUT) return ShipSystemStatsScript.State.OUT
            if (CAstate == State.COOLDOWN) return ShipSystemStatsScript.State.COOLDOWN
            if (CAstate == State.READY) return ShipSystemStatsScript.State.IDLE
            return null
        }

        override fun advance(amount: Float, isPaused: Boolean) {

            val target = ship.childModulesCopy.find { it.shipTarget != null }?.shipTarget
            val flockingAI = get("flockingAI", ship.ai, BasicShipAI::class.java) as com.fs.starfarer.combat.ai.movement.A
            ReflectionUtils.invoke("advance", system, amount, declared = false)
            systemAI?.o00000(amount, flockingAI.Õ00000(), flockingAI.Ø00000(), target as? Ship)

            if (!isPaused && state != State.READY && state != State.COOLDOWN) {
                //systemStats.apply(ship.getMutableStats(), systemSpec.getId(), translateState(getState()), getEffectLevel())
            }

            if (ship == Global.getCombatEngine().playerShip) {
                val statusIndex = 0
                var statusData: StatusData?
                do {
                    statusData = systemStats?.getStatusData(statusIndex, translateState(getState()), getEffectLevel())
                    if (statusData != null) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(
                            "phaseteleporter" + statusIndex,
                            systemSpec.iconSpriteName,
                            systemSpec.name,
                            statusData.text,
                            statusData.isDebuff
                        )
                    }
                } while (statusData != null)
            }
        }

        override fun onStateSwitched(oldState: State?) {
            if (state == State.COOLDOWN) {
                systemStats?.unapply(ship.mutableStats, systemSpec.id)
            }
        }

    }
}*/