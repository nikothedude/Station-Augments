package niko_SA.augments.core

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color

enum class BuiltInMode {
    NOT,
    /** Currently unused. Use [NORMAL] instead.*/
    SMOD {
        override fun createDesc(info: TooltipMakerAPI) {
            val para = info.addPara(
                "This augment has been %s into the station - it cannot be removed, save for %s.",
                5f,
                Misc.getStoryOptionColor(),
                "s-modded", "scuttling the entire thing"
            )
            para.setHighlightColors(Misc.getStoryOptionColor(), Misc.getNegativeHighlightColor())
            para.color = Misc.getGrayColor()
        }

        override fun getAPColor(): Color? {
            return Misc.getStoryOptionColor()
        }
    },
    NORMAL {
        override fun createDesc(info: TooltipMakerAPI) {
            info.addPara(
                "This augment has been %s to the station, and cannot be removed.",
                5f,
                Misc.getHighlightColor(),
                "built-in"
            ).color = Misc.getGrayColor()
        }

        override fun getAPColor(): Color? {
            return Misc.getGrayColor()
        }
    };

    open fun createDesc(info: TooltipMakerAPI) {

    }
    open fun getAPColor(): Color? {
        return null
    }
}