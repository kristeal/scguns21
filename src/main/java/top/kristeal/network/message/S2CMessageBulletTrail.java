package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.kristeal.client.network.ClientPlayHandler;
import top.kristeal.common.Gun;
import top.kristeal.entity.projectile.ProjectileEntity;
import top.kristeal.network.BufferUtil;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload; the wire format is unchanged.
 */
public class S2CMessageBulletTrail implements CustomPacketPayload
{
    public static final Type<S2CMessageBulletTrail> TYPE = new Type<>(Reference.id("s2c_bullet_trail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageBulletTrail> STREAM_CODEC = StreamCodec.ofMember(S2CMessageBulletTrail::encode, S2CMessageBulletTrail::decode);

    private int[] entityIds;
    private Vec3[] positions;
    private Vec3[] motions;
    private ItemStack item;
    private int trailColor;
    private double trailLengthMultiplier;
    private int life;
    private double gravity;
    private int shooterId;
    private boolean enchanted;
    private ParticleOptions particleData;
    private boolean isVisible;
    private double trailThickness; // Add this

    public S2CMessageBulletTrail() {}

    public S2CMessageBulletTrail(ProjectileEntity[] spawnedProjectiles, Gun.Projectile projectileProps, int shooterId, ParticleOptions particleData) {
        this.positions = new Vec3[spawnedProjectiles.length];
        this.motions = new Vec3[spawnedProjectiles.length];
        this.entityIds = new int[spawnedProjectiles.length];

        for(int i = 0; i < spawnedProjectiles.length; ++i) {
            ProjectileEntity projectile = spawnedProjectiles[i];
            this.positions[i] = projectile.position();
            this.motions[i] = projectile.getDeltaMovement();
            this.entityIds[i] = projectile.getId();
        }

        this.item = spawnedProjectiles[0].getItem();
        this.enchanted = spawnedProjectiles[0].getWeapon().isEnchanted();
        this.trailColor = this.enchanted ? 10252799 : projectileProps.getTrailColor();
        this.trailLengthMultiplier = projectileProps.getTrailLengthMultiplier();
        this.life = projectileProps.getLife();
        this.gravity = spawnedProjectiles[0].getModifiedGravity();
        this.shooterId = shooterId;
        this.particleData = particleData;
        this.trailThickness = projectileProps.getTrailThickness();
    }

    public S2CMessageBulletTrail(ProjectileEntity[] spawnedProjectiles, Gun.Projectile projectileProps, int shooterId, ParticleOptions particleData, boolean isVisible)
    {
        this.positions = new Vec3[spawnedProjectiles.length];
        this.motions = new Vec3[spawnedProjectiles.length];
        this.entityIds = new int[spawnedProjectiles.length];
        for(int i = 0; i < spawnedProjectiles.length; i++)
        {
            ProjectileEntity projectile = spawnedProjectiles[i];
            this.positions[i] = projectile.position();
            this.motions[i] = projectile.getDeltaMovement();
            this.entityIds[i] = projectile.getId();
        }
        this.item = spawnedProjectiles[0].getItem();
        this.enchanted = spawnedProjectiles[0].getWeapon().isEnchanted();
        this.trailColor = this.enchanted ? 0x9C71FF : projectileProps.getTrailColor();
        this.trailLengthMultiplier = projectileProps.getTrailLengthMultiplier();
        this.life = projectileProps.getLife();
        this.gravity = spawnedProjectiles[0].getModifiedGravity();
        this.shooterId = shooterId;
        this.particleData = particleData;
        this.isVisible = isVisible;
        this.trailThickness = projectileProps.getTrailThickness();
    }

    public S2CMessageBulletTrail(int[] entityIds, Vec3[] positions, Vec3[] motions, ItemStack item,
                                 int trailColor, double trailLengthMultiplier, int life, double gravity,
                                 int shooterId, boolean enchanted, ParticleOptions particleData,
                                 boolean isVisible, double trailThickness) // Add parameter
    {
        this.entityIds = entityIds;
        this.positions = positions;
        this.motions = motions;
        this.item = item;
        this.trailColor = trailColor;
        this.trailLengthMultiplier = trailLengthMultiplier;
        this.life = life;
        this.gravity = gravity;
        this.shooterId = shooterId;
        this.enchanted = enchanted;
        this.particleData = particleData;
        this.isVisible = isVisible;
        this.trailThickness = trailThickness;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.entityIds.length);
        for(int i = 0; i < this.entityIds.length; i++)
        {
            buffer.writeInt(this.entityIds[i]);
            BufferUtil.writeVec3(buffer, this.positions[i]);
            BufferUtil.writeVec3(buffer, this.motions[i]);
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.item); // Port note: 1.21 stream codecs
        buffer.writeVarInt(this.trailColor);
        buffer.writeDouble(this.trailLengthMultiplier);
        buffer.writeInt(this.life);
        buffer.writeDouble(this.gravity);
        buffer.writeInt(this.shooterId);
        buffer.writeBoolean(this.enchanted);
        buffer.writeBoolean(this.isVisible);
        ParticleTypes.STREAM_CODEC.encode(buffer, this.particleData); // Port note: writes the particle type and its data together
        buffer.writeDouble(this.trailThickness);
    }
    public static S2CMessageBulletTrail decode(RegistryFriendlyByteBuf buffer)
    {
        int size = buffer.readInt();
        int[] entityIds = new int[size];
        Vec3[] positions = new Vec3[size];
        Vec3[] motions = new Vec3[size];
        for(int i = 0; i < size; i++)
        {
            entityIds[i] = buffer.readInt();
            positions[i] = BufferUtil.readVec3(buffer);
            motions[i] = BufferUtil.readVec3(buffer);
        }
        ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        int trailColor = buffer.readVarInt();
        double trailLengthMultiplier = buffer.readDouble();
        int life = buffer.readInt();
        double gravity = buffer.readDouble();
        int shooterId = buffer.readInt();
        boolean enchanted = buffer.readBoolean();
        boolean isVisible = buffer.readBoolean();
        ParticleOptions particleData = ParticleTypes.STREAM_CODEC.decode(buffer);
        double trailThickness = buffer.readDouble();
        return new S2CMessageBulletTrail(entityIds, positions, motions, item, trailColor,
                trailLengthMultiplier, life, gravity, shooterId,
                enchanted, particleData, isVisible, trailThickness);
    }
    public double getTrailThickness() {
        return this.trailThickness;
    }
    public static void handle(S2CMessageBulletTrail message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleMessageBulletTrail(message));
    }

    public int getCount()
    {
        return this.entityIds.length;
    }

    public int[] getEntityIds()
    {
        return this.entityIds;
    }

    public Vec3[] getPositions()
    {
        return this.positions;
    }

    public Vec3[] getMotions()
    {
        return this.motions;
    }

    public int getTrailColor()
    {
        return this.trailColor;
    }

    public double getTrailLengthMultiplier()
    {
        return this.trailLengthMultiplier;
    }

    public int getLife()
    {
        return this.life;
    }

    public ItemStack getItem()
    {
        return this.item;
    }

    public double getGravity()
    {
        return this.gravity;
    }

    public int getShooterId()
    {
        return this.shooterId;
    }

    public boolean isEnchanted()
    {
        return this.enchanted;
    }

    public boolean isVisible()
    {
        return this.isVisible;
    }

    public ParticleOptions getParticleData()
    {
        return this.particleData;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
