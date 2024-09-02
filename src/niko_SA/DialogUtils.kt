package niko_SA

import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI

object DialogUtils {
    @JvmStatic
    fun UIPanelAPI.getChildrenCopy() : List<UIComponentAPI> {
        return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
    }
    @JvmStatic
    fun UIPanelAPI.getChildrenNonCopy() : List<UIComponentAPI>  {
        return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
    }
    @JvmStatic
    fun UIComponentAPI.getParent() : UIPanelAPI {
        return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
    }
}