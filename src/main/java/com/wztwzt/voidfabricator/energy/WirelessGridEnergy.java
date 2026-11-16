package com.wztwzt.voidfabricator.energy;

import java.math.BigInteger;
import java.util.UUID;

import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;

import gregtech.common.misc.WirelessNetworkManager;

/**
 * Bridges accepted virtual crafts to GregTech's player-bound wireless EU map.
 *
 * The map is the same pool used by GT wireless output/input hatches. A craft is
 * charged once, before AE inputs are consumed, at:
 *
 * max(0, recipe EU/t) * recipe duration (ticks) * requested batches
 *
 * GT overclocking keeps total EU roughly invariant (4x power for 1/4 duration),
 * so the recipe snapshot's base EU/t * duration is the correct per-craft cost.
 */
public final class WirelessGridEnergy {

    private WirelessGridEnergy() {}

    public static BigInteger cost(RecipeSnapshot snapshot, int batches) {
        if (snapshot == null || batches <= 0) {
            return BigInteger.ZERO;
        }
        long euPerTick = Math.max(0L, (long) snapshot.getEuPerTick());
        long duration = Math.max(0L, (long) snapshot.getDuration());
        if (euPerTick == 0L || duration == 0L) {
            return BigInteger.ZERO;
        }
        return BigInteger.valueOf(euPerTick)
            .multiply(BigInteger.valueOf(duration))
            .multiply(BigInteger.valueOf(batches));
    }

    public static BigInteger balance(UUID owner) {
        if (owner == null) {
            return BigInteger.ZERO;
        }
        BigInteger balance = WirelessNetworkManager.getUserEU(owner);
        return balance == null ? BigInteger.ZERO : balance;
    }

    /** Atomically charges the player/team wireless EU map. */
    public static boolean charge(UUID owner, BigInteger cost) {
        if (cost == null || cost.signum() <= 0) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        return WirelessNetworkManager.addEUToGlobalEnergyMap(owner, cost.negate());
    }

    /** Refunds a charge when the accepted output cannot be buffered. */
    public static void refund(UUID owner, BigInteger cost) {
        if (owner == null || cost == null || cost.signum() <= 0) {
            return;
        }
        WirelessNetworkManager.addEUToGlobalEnergyMap(owner, cost);
    }

    public static void ensureUser(UUID owner) {
        if (owner != null) {
            WirelessNetworkManager.strongCheckOrAddUser(owner);
        }
    }
}
