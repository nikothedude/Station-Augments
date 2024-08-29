/*//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package lyravega.proxies

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShieldSpecAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints
import com.fs.starfarer.api.loading.WeaponSlotAPI
import lyravega.utilities.logger.lyr_logger
import lyravega.utilities.lyr_reflectionUtilities.methodReflection
import niko_SA.ReflectionUtils
import java.lang.invoke.MethodHandle
import java.util.*

class lyr_hullSpec {
    private var hullSpec: ShipHullSpecAPI
    private var weaponSlot: lyr_weaponSlot? = null
    var shieldSpec: lyr_shieldSpec? = null
        get() {
            field = if (field == null) lyr_shieldSpec(hullSpec.shieldSpec) else field
            return field
        }
        private set
    var engineSlots: List<Any>? = null
        get() = if (field != null) {
            field
        } else {
            try {
                field = getEngineSlots!!.invoke(hullSpec)
            } catch (var2: Throwable) {
                lyr_logger.error("Failed to use 'getEngineSlots()' in 'lyr_hullSpec'", var2)
            }
            field
        }
        private set

    constructor(hullSpec: ShipHullSpecAPI) {
        this.hullSpec = hullSpec
    }

    constructor(hullSpec: ShipHullSpecAPI, clone: Boolean) {
        this.hullSpec = if (clone) duplicate(hullSpec) else hullSpec
    }

    fun retrieve(): ShipHullSpecAPI {
        return hullSpec
    }

    fun recycle(hullSpec: ShipHullSpecAPI): lyr_hullSpec {
        this.hullSpec = hullSpec
        return this
    }

    private fun duplicate(hullSpec: ShipHullSpecAPI): ShipHullSpecAPI {
        return try {
            clone!!.invoke(hullSpec)
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'duplicate()' in 'lyr_hullSpec'", var3)
            hullSpec
        }
    }

    override fun clone(): lyr_hullSpec {
        return lyr_hullSpec(duplicate(hullSpec))
    }

    fun getWeaponSlot(weaponSlotId: String?): lyr_weaponSlot? {
        weaponSlot =
            if (weaponSlot == null) lyr_weaponSlot(hullSpec.getWeaponSlotAPI(weaponSlotId)) else weaponSlot!!.recycle(
                hullSpec.getWeaponSlotAPI(weaponSlotId)
            )
        return weaponSlot
    }

    fun setShieldSpec(shieldSpec: ShieldSpecAPI?) {
        try {
            setShieldSpec!!.invoke(hullSpec, shieldSpec)
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'setShieldSpec()' in 'lyr_hullSpec'", var3)
        }
    }

    @Deprecated("")
    fun addWeaponSlot(weaponSlot: WeaponSlotAPI?) {
        try {
            addWeaponSlot!!.invoke(hullSpec, lyr_weaponSlot.weaponSlotClass.cast(weaponSlot))
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'addWeaponSlot()' in 'lyr_hullSpec'", var3)
        }
    }

    fun addWeaponSlot(weaponSlot: lyr_weaponSlot) {
        try {
            addWeaponSlot!!.invoke(hullSpec, lyr_weaponSlot.weaponSlotClass.cast(weaponSlot.retrieve()))
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'addWeaponSlot()' in 'lyr_hullSpec'", var3)
        }
    }

    fun addBuiltInWing(wingId: String?) {
        try {
            addBuiltInWing!!.invoke(hullSpec, wingId)
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'addBuiltInWing()' in 'lyr_hullSpec'", var3)
        }
    }

    fun setOrdnancePoints(ordnancePoints: Int) {
        try {
            setOrdnancePoints!!.invoke(hullSpec, ordnancePoints)
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'setOrdnancePoints()' in 'lyr_hullSpec'", var3)
        }
    }

    fun setBaseHullId(baseHullId: String?) {
        try {
            setBaseHullId!!.invoke(hullSpec, baseHullId)
        } catch (var3: Throwable) {
            lyr_logger.error("Failed to use 'setBaseHullId()' in 'lyr_hullSpec'", var3)
        }
    }

    var spriteSpec: Any?
        get() {
            return try {
                getSpriteSpec!!.invoke(hullSpec)
            } catch (var2: Throwable) {
                lyr_logger.error("Failed to use 'getSpriteSpec()' in 'lyr_hullSpec'", var2)
                null
            }
        }
        set(spriteSpec) {
            try {
                setSpriteSpec!!.invoke(hullSpec, spriteSpec)
            } catch (var3: Throwable) {
                lyr_logger.error("Failed to use 'setSpriteSpec()' in 'lyr_hullSpec'", var3)
            }
        }
    val builtInMods: List<String>
        get() = hullSpec.builtInMods

    fun addBuiltInMod(hullModSpecId: String?) {
        hullSpec.addBuiltInMod(hullModSpecId)
    }

    fun setManufacturer(manufacturer: String?) {
        hullSpec.manufacturer = manufacturer
    }

    var descriptionPrefix: String?
        get() = hullSpec.descriptionPrefix
        set(destriptionPrefix) {
            hullSpec.descriptionPrefix = destriptionPrefix
        }

    fun addBuiltInWeapon(slotId: String?, weaponSpecId: String?) {
        hullSpec.addBuiltInWeapon(slotId, weaponSpecId)
    }

    fun setShipDefenseId(defenseId: String?) {
        hullSpec.shipDefenseId = defenseId
    }

    fun getOrdnancePoints(characterStats: MutableCharacterStatsAPI?): Int {
        return hullSpec.getOrdnancePoints(characterStats)
    }

    fun setDParentHullId(parentHullId: String?) {
        hullSpec.dParentHullId = parentHullId
    }

    fun setRestoreToBase(restoreToBase: Boolean) {
        hullSpec.isRestoreToBase = restoreToBase
    }

    var baseValue: Float
        get() = hullSpec.baseValue
        set(value) {
            try {
                setBaseValue!!.invoke(hullSpec, value)
            } catch (var3: Throwable) {
                lyr_logger.error("Failed to use 'setBaseValue()' in 'lyr_hullSpec'", var3)
            }
        }
    var hullName: String?
        get() = hullSpec.hullName
        set(hullName) {
            hullSpec.hullName = hullName
        }
    val tags: Set<String>
        get() = hullSpec.tags

    fun addTag(tag: String?) {
        hullSpec.addTag(tag)
    }

    val allWeaponSlotsCopy: List<WeaponSlotAPI>
        get() = hullSpec.allWeaponSlotsCopy
    var shipSystemId: String?
        get() = hullSpec.shipSystemId
        set(shipSystemId) {
            hullSpec.shipSystemId = shipSystemId
        }
    val hints: EnumSet<ShipTypeHints>
        get() = hullSpec.hints

    companion object {
        var hullSpecClass: Class<*>? = null
        private var clone: MethodHandle? = null
        private var getEngineSlots: MethodHandle? = null
        private var setShieldSpec: MethodHandle? = null
        private var addWeaponSlot: MethodHandle? = null
        private var addBuiltInWing: MethodHandle? = null
        private var setOrdnancePoints: MethodHandle? = null
        private var setBaseHullId: MethodHandle? = null
        private var setBaseValue: MethodHandle? = null
        private var getSpriteSpec: MethodHandle? = null
        private var setSpriteSpec: MethodHandle? = null

        init {
            try {
                hullSpecClass = (Global.getSettings().allShipHullSpecs.iterator().next() as ShipHullSpecAPI).javaClass
                clone = ReflectionUtils.getMethod("clone", hullSpecClass, *arrayOfNulls(0))
                getEngineSlots =
                    methodReflection.findMethodByName("getEngineSlots", hullSpecClass, *arrayOfNulls(0)).methodHandle
                setShieldSpec =
                    methodReflection.findMethodByName("setShieldSpec", hullSpecClass, *arrayOfNulls(0)).methodHandle
                addWeaponSlot =
                    methodReflection.findMethodByName("addWeaponSlot", hullSpecClass, *arrayOfNulls(0)).methodHandle
                addBuiltInWing =
                    methodReflection.findMethodByName("addBuiltInWing", hullSpecClass, *arrayOfNulls(0)).methodHandle
                setOrdnancePoints =
                    methodReflection.findMethodByName("setOrdnancePoints", hullSpecClass, *arrayOfNulls(0)).methodHandle
                setBaseHullId =
                    methodReflection.findMethodByName("setBaseHullId", hullSpecClass, *arrayOfNulls(0)).methodHandle
                setBaseValue =
                    methodReflection.findMethodByName("setBaseValue", hullSpecClass, *arrayOfNulls(0)).methodHandle
                getSpriteSpec =
                    methodReflection.findMethodByName("getSpriteSpec", hullSpecClass, *arrayOfNulls(0)).methodHandle
                setSpriteSpec =
                    methodReflection.findMethodByName("setSpriteSpec", hullSpecClass, *arrayOfNulls(0)).methodHandle
            } catch (var1: Throwable) {
                lyr_logger.fatal("Failed to find a method in 'lyr_hullSpec'", var1)
            }
        }
    }
}*/