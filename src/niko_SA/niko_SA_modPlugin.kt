package niko_SA

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import java.lang.RuntimeException

class niko_SA_modPlugin: BaseModPlugin() {
    override fun onApplicationLoad() {
        super.onApplicationLoad()

        /*val starsectorVers = Global.getSettings().gameVersion
        if (starsectorVers > "0.97a-RC11") {
            throw RuntimeException("CHECK TO SEE IF stationMarketNullPatch IS NECESSARY! https://fractalsoftworks.com/forum/index.php?topic=30567")
        }*/
    }
}