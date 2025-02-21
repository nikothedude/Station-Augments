package niko_SA.augments.jumpPoint

import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import niko_SA.SA_baseNikoScript
import niko_SA.SA_miscUtils
import niko_SA.SA_miscUtils.getApproximateHyperspaceLoc

class SA_hyperEntityStaplingScript(
    val toMove: SectorEntityToken,
    val target: SectorEntityToken
): SA_baseNikoScript() {
    override fun startImpl() {
        toMove.addScript(this)
    }

    override fun stopImpl() {
        toMove.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!target.isAlive){
            delete()
            return
        }
        toMove.location.set(target.getApproximateHyperspaceLoc())
    }
}