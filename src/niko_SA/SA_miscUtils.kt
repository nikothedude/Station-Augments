package niko_SA

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

object SA_miscUtils {

    fun SectorEntityToken.getApproximateHyperspaceLoc(): Vector2f {
        if (isInHyperspace) return location
        if (containingLocation !is StarSystemAPI) return Vector2f()
        val starSystem = containingLocation as StarSystemAPI

        val offset = Vector2f.sub(location, starSystem.center.location, Vector2f()) // taken from transverse jump
        val maxInSystem = 20000f
        val maxInHyper = 2000f
        var f = offset.length() / maxInSystem
        if (f > 0.5f) f = 0.5f

        val angle = Misc.getAngleInDegreesStrict(offset)

        val destOffset = Misc.getUnitVectorAtDegreeAngle(angle)
        destOffset.scale(f * maxInHyper)

        Vector2f.add(starSystem.location, destOffset, destOffset)

        return destOffset
    }

    fun ShipAPI.getFurthestModule(): ShipAPI {
        var moduleWithMaxDist: ShipAPI = this
        var maxDist = 0f

        for (module in childModulesCopy) {
            val dist = MathUtils.getDistance(location, module.location)
            if (dist > maxDist) {
                moduleWithMaxDist = module
                maxDist = dist
            }
        }

        return moduleWithMaxDist
    }
}