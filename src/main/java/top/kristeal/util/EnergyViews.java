package top.kristeal.util;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * ENERGY FIXES
 */
public final class EnergyViews {
    private EnergyViews() {
    }

    /** Accepts FE but refuses extraction: for consumers. */
    public static IEnergyStorage receiveOnly(IEnergyStorage storage) {
        return new View(storage, true, false);
    }

    /** Gives FE but refuses insertion: for output faces. */
    public static IEnergyStorage extractOnly(IEnergyStorage storage) {
        return new View(storage, false, true);
    }

    private record View(IEnergyStorage storage, boolean receive, boolean extract) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receive ? storage.receiveEnergy(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return extract ? storage.extractEnergy(maxExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return extract;
        }

        @Override
        public boolean canReceive() {
            return receive;
        }
    }
}
