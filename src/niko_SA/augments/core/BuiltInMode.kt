package niko_SA.augments.core

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

enum class BuiltInMode {
    NOT,
    /** Currently unused. Use [NORMAL] instead.*/
    SMOD {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara(
                "This augment has been %s into station - it cannot be removed, save for scuttling the entire thing.",
                5f,
                Misc.getStoryOptionColor(),
                "s-modded", "scuttling the entire thing"
            ).setHighlightColors(Misc.getStoryOptionColor(), Misc.getNegativeHighlightColor())
        }
    },
    NORMAL {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara(
                "This augment has been %s to the station, and cannot be removed.",
                5f,
                Misc.getHighlightColor(),
                "built-in"
            )
        }
    };

    open fun createDesc(info: TooltipMakerAPI) {

    }
}