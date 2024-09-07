package niko_SA

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import org.lazywizard.console.commands.ForceDeployAll
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

object SA_combatUtils {
    private const val MIN_OFFSET = 300f

    // Taken from console commands ForceDeployAll
    fun moveToSpawnLocations(toMove: List<ShipAPI>) {
        val engine = Global.getCombatEngine()
        val spawnLoc = Vector2f(
            -engine.mapWidth * 0.2f, engine.mapHeight / 2f
        )
        val ships = engine.ships
        for (ship in toMove) {
            val radius = ship.collisionRadius + MIN_OFFSET
            var i = 0
            while (i < ships.size) {
                val other = ships[i]
                if (MathUtils.isWithinRange(other, spawnLoc, radius)) {
                    spawnLoc.x += radius
                    if (spawnLoc.x >= engine.mapWidth / 2f) {
                        spawnLoc.x = -engine.mapWidth
                        spawnLoc.y -= radius
                    }

                    // We need to recheck for collisions in our new position
                    i = 0
                }
                i++
            }

            //System.out.println("Moving " + ship.getHullSpec().getHullId()
            //        + " to { " + spawnLoc.x + ", " + spawnLoc.y + " }");
            ship.location[spawnLoc.x] = spawnLoc.y
            spawnLoc.x += radius
        }
    }
}