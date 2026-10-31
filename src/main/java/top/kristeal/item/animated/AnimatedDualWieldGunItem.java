package top.kristeal.item.animated;

import net.minecraft.sounds.SoundEvent;

public class AnimatedDualWieldGunItem extends AnimatedGunItem{
    public AnimatedDualWieldGunItem(Properties properties, String path, SoundEvent reloadSoundMagOut, SoundEvent reloadSoundMagIn, SoundEvent reloadSoundEnd, SoundEvent boltPullSound, SoundEvent boltReleaseSound) {
        // Port note: Item#isFireResistant() was removed in 1.20.5; fire resistance is now an item property
        super(properties.fireResistant(), path, reloadSoundMagOut, reloadSoundMagIn, reloadSoundEnd, boltPullSound, boltReleaseSound);
    }
}
