package niko_SA.augments.threat

import niko_SA.MarketUtils.hasFragmentSwarm
import niko_SA.MarketUtils.hasStationAugment
import niko_SA.augments.core.stationAttachment

abstract class ThreatAugment: stationAttachment() {

    override fun getUnavailableReason(): String? {
        val superResult = super.getUnavailableReason()
        if (superResult != null) return superResult

        if (market != null && !market!!.hasFragmentSwarm()) return "No fragment swarm"

        return null
    }

}