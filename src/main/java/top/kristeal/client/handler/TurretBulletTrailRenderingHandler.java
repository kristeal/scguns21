package top.kristeal.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import top.kristeal.Reference;
import top.kristeal.client.TurretBulletTrail;

import java.util.HashMap;
import java.util.Map;

public class TurretBulletTrailRenderingHandler {
    private static TurretBulletTrailRenderingHandler instance;
    private static final ResourceLocation TURRET_TRAIL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/trail/turret_projectile.png");

    public static TurretBulletTrailRenderingHandler get() {
        if (instance == null) {
            instance = new TurretBulletTrailRenderingHandler();
        }
        return instance;
    }

    private final Map<Integer, TurretBulletTrail> trails = new HashMap<>();

    private TurretBulletTrailRenderingHandler() {}

    public void add(TurretBulletTrail trail) {
        this.trails.put(trail.getEntityId(), trail);
    }

    public void remove(int entityId) {
        this.trails.remove(entityId);
    }

    // Port note: TickEvent.ClientTickEvent (phase END) -> ClientTickEvent.Post
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Level world = Minecraft.getInstance().level;
        if (world != null) {
            this.trails.values().forEach(trail -> {
                trail.tick();
                Entity entity = Minecraft.getInstance().getCameraEntity();
                double distance = entity != null ? Math.sqrt(entity.distanceToSqr(trail.getPosition())) : Double.MAX_VALUE;
                if (trail.getAge() >= trail.getMaxAge() || distance > 256) {
                    trail.setDead(true);
                }
            });
            this.trails.values().removeIf(TurretBulletTrail::isDead);
        } else if (!this.trails.isEmpty()) {
            this.trails.clear();
        }
    }

    /**
     * Port note: the reference called render() from mixin/client/LevelRendererMixin, injected at the first
     * LevelRenderer#checkPoseStack in renderLevel. In 1.21.1 that call sits right before the AFTER_ENTITIES render stage,
     * so this subscriber draws at the same point with the stage's camera-relative pose stack. No mixin needed.
     */
    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            this.render(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    public void render(PoseStack stack, float partialTicks) {
        for (TurretBulletTrail trail : this.trails.values()) {
            this.renderTurretTrail(trail, stack, partialTicks);
        }
    }

    @SubscribeEvent
    public void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        this.trails.clear();
    }

    @SubscribeEvent
    public void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        this.trails.clear();
    }

    private void renderTurretTrail(TurretBulletTrail trail, PoseStack poseStack, float deltaTicks) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.getCameraEntity();
        Level world = mc.level;

        if (entity == null || trail.isDead() || world == null)
            return;

        Entity projectileEntity = world.getEntity(trail.getEntityId());
        if (projectileEntity == null)
            return;

        poseStack.pushPose();

        Vec3 view = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 position = trail.getPosition();
        Vec3 motion = trail.getMotion();
        double bulletX = position.x + motion.x * deltaTicks;
        double bulletY = position.y + motion.y * deltaTicks;
        double bulletZ = position.z + motion.z * deltaTicks;
        poseStack.translate(bulletX - view.x(), bulletY - view.y(), bulletZ - view.z());

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(deltaTicks, trail.getYaw(), trail.getYaw()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(deltaTicks, trail.getPitch(), trail.getPitch())));

        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        MultiBufferSource.BufferSource renderTypeBuffer = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(
                RenderType.energySwirl(TURRET_TRAIL_TEXTURE, 0.0F, 0.15625F));

        PoseStack.Pose posestack$pose = poseStack.last();
        Matrix4f matrix4f = posestack$pose.pose();

        double speed = Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
        float speedFactor = (float) Math.max(1.0, speed * 0.5);

        int baseSize = 25;
        int size = (int) Math.min((trail.getAge() + 1) * 25 * trail.getTrailThickness() * speedFactor,
                baseSize * trail.getTrailThickness() * speedFactor);

        int color = trail.getTrailColor();
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        float brightnessFactor = (float) Math.min(1.3, 1.0 + speed * 0.1);
        red = Math.min(255, (int)(red * brightnessFactor));
        green = Math.min(255, (int)(green * brightnessFactor));
        blue = Math.min(255, (int)(blue * brightnessFactor));

        int light = 15728880;

        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, -1, 0.0F, 0.15625F, -1, 0, 0, light);
        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, 1, 0.15625F, 0.15625F, -1, 0, 0, light);
        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, 1, 0.15625F, 0.3125F, -1, 0, 0, light);
        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, -1, 0.0F, 0.3125F, -1, 0, 0, light);

        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, -1, 0.0F, 0.15625F, 1, 0, 0, light);
        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, 1, 0.15625F, 0.15625F, 1, 0, 0, light);
        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, 1, 0.15625F, 0.3125F, 1, 0, 0, light);
        this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, -1, 0.0F, 0.3125F, 1, 0, 0, light);

        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, 1, 0.0F, 0.0F, 0, 1, 0, light);
            this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, 1, 0.5F, 0.0F, 0, 1, 0, light);
            this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, 1, 0.5F, 0.15625F, 0, 1, 0, light);
            this.vertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, 1, 0.0F, 0.15625F, 0, 1, 0, light);
        }

        poseStack.popPose();
    }

    // Port note: 1.21 VertexConsumer API (addVertex/setColor/setUv/setOverlay/setLight/setNormal, no endVertex). The
    // normal now takes the PoseStack.Pose; it is the same mutable pose the reference read its Matrix3f from.
    public void vertex(int red, int green, int blue, Matrix4f pMatrix, PoseStack.Pose pNormal,
                       VertexConsumer pConsumer, int pX, int pY, int pZ, float pU, float pV,
                       int pNormalX, int pNormalZ, int pNormalY, int pPackedLight) {
        pConsumer.addVertex(pMatrix, (float)pX, (float)pY, (float)pZ)
                .setColor(red, green, blue, 255)
                .setUv(pU, pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pPackedLight)
                .setNormal(pNormal, (float)pNormalX, (float)pNormalY, (float)pNormalZ);
    }
}