package top.kristeal.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import top.kristeal.Reference;
import top.kristeal.interfaces.IGunModifier;
import top.kristeal.item.attachment.impl.UnderBarrel;

public class BayonetItem extends UnderBarrelItem {
    private final float attackDamage;
    private final float attackSpeed;
    private final float attackReach;
    private final IGunModifier modifier;


    public BayonetItem(UnderBarrel underBarrel, Properties properties, float attackDamage, float attackSpeed, float attackReach) {
        super(underBarrel, properties);
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.attackReach = attackReach;
        this.modifier = underBarrel.getModifier();
    }

    public BayonetItem(UnderBarrel underBarrel, Properties properties, boolean colored, float attackDamage, float attackSpeed, float attackReach) {
        super(underBarrel, properties, colored);
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.attackReach = attackReach;
        this.modifier = underBarrel.getModifier();
    }

    public float getAdditionalDamage() {
        return this.modifier.additionalDamage();
    }

    // Port note: canApplyAtEnchantingTable (BAYONET and WEAPON categories) is gone in 1.21; which enchantments apply
    // is decided by the enchantments' item tags (the bayonet tag and #minecraft:enchantable/weapon).

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        if (state.getDestroySpeed(world, pos) != 0.0F) {
            stack.hurtAndBreak(2, entityLiving, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        return !player.isCreative();
    }
    //BAYONET REACH WEAEWAWA
    private static final ResourceLocation BASE_REACH = Reference.id("bayonet_reach");

    // Port note: 1.21 attribute modifiers are an ItemAttributeModifiers component with slot groups; same values and slot
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, this.attackDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, this.attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(BASE_REACH, this.attackReach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public float getDamage() {
        return this.attackDamage;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(Blocks.COBWEB) ? 15.0F : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState block) {
        return block.is(Blocks.COBWEB);
    }
}


