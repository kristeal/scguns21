package top.kristeal.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import top.kristeal.network.message.S2CMessageRaidFlareBurst;
import top.kristeal.network.message.S2CMessageRaidFlareBurst.ParticleData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Draws the burst of a raid flare as one of the reference's particle patterns.
 * <p>
 * Port notes: the patterns are the reference's {@code ClientPlayHandler.handleRaidFlareBurst} and its helpers, unchanged.
 * An unknown pattern name falls back to a single burst. {@code minecraft:dust} takes its colour from the flare JSON; other
 * particles must be simple (parameterless) particle types.
 */
public final class RaidFlareBurstHandler
{
    private RaidFlareBurstHandler() {}

    public static void handle(S2CMessageRaidFlareBurst message)
    {
        Level world = Minecraft.getInstance().level;
        if (world == null)
        {
            return;
        }

        double x = message.getX();
        double y = message.getY();
        double z = message.getZ();
        double scale = message.getScale();
        int repetitions = message.getRepetitions();
        List<ParticleData> particles = message.getParticles();

        switch (message.getPatternType())
        {
            case "star" -> star(world, x, y, z, scale, repetitions, particles);
            case "circle" -> circle(world, x, y, z, scale, repetitions, particles);
            case "union_jack" -> unionJack(world, x, y, z, scale, particles);
            case "spiral" -> spiral(world, x, y, z, scale, repetitions, particles);
            case "wave" -> wave(world, x, y, z, scale, repetitions, particles);
            case "cross" -> cross(world, x, y, z, scale, particles);
            case "double_helix" -> doubleHelix(world, x, y, z, scale, repetitions, particles);
            case "burst" -> burst(world, x, y, z, scale, repetitions, particles);
            case "ring" -> ring(world, x, y, z, scale, repetitions, particles);
            case "pig_snout" -> pigSnout(world, x, y, z, scale, particles);
            case "skulk_pulse" -> skulkPulse(world, x, y, z, scale, repetitions, particles);
            default -> spawnAt(world, x, y, z, particles);
        }
    }

    private static void pigSnout(Level world, double cx, double cy, double cz, double scale, List<ParticleData> particles)
    {
        int segments = 25;
        double nostrilRadius = scale * 0.25;
        double nostrilOffset = scale * 0.35;

        for (int i = 0; i < segments; i++)
        {
            double angle = (Math.PI * 2 * i) / segments;
            spawnAt(world, cx - nostrilOffset + Math.cos(angle) * nostrilRadius, cy, cz + Math.sin(angle) * nostrilRadius, particles);
        }
        for (int i = 0; i < segments; i++)
        {
            double angle = (Math.PI * 2 * i) / segments;
            spawnAt(world, cx + nostrilOffset + Math.cos(angle) * nostrilRadius, cy, cz + Math.sin(angle) * nostrilRadius, particles);
        }

        int outerSegments = 40;
        for (int i = 0; i < outerSegments; i++)
        {
            double angle = (Math.PI * 2 * i) / outerSegments;
            spawnAt(world, cx + Math.cos(angle) * scale, cy, cz + Math.sin(angle) * scale * 0.7, particles);
        }
    }

    private static void skulkPulse(Level world, double cx, double cy, double cz, double scale, int repetitions, List<ParticleData> particles)
    {
        int segments = 30;
        for (int rep = 0; rep < repetitions; rep++)
        {
            double pulseRadius = scale * ((rep + 1.0) / repetitions);

            for (int i = 0; i < segments; i++)
            {
                double angle = (Math.PI * 2 * i) / segments;
                spawnAt(world, cx + Math.cos(angle) * pulseRadius, cy, cz + Math.sin(angle) * pulseRadius, particles);
            }

            if (rep % 2 == 0)
            {
                int tendrils = 8;
                for (int t = 0; t < tendrils; t++)
                {
                    double tendrilAngle = (Math.PI * 2 * t) / tendrils + (rep * 0.3);
                    for (int p = 0; p < 5; p++)
                    {
                        double distance = pulseRadius + (p * scale * 0.2);
                        spawnAt(world, cx + Math.cos(tendrilAngle) * distance, cy, cz + Math.sin(tendrilAngle) * distance, particles);
                    }
                }
            }
        }
    }

    private static void spiral(Level world, double cx, double cy, double cz, double radius, int repetitions, List<ParticleData> particles)
    {
        int segments = 40;
        for (int rep = 0; rep < repetitions; rep++)
        {
            for (int i = 0; i < segments; i++)
            {
                double t = (double) i / segments;
                double angle = t * Math.PI * 4;
                double r = radius * t;
                spawnAt(world, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r, particles);
            }
        }
    }

    private static void wave(Level world, double cx, double cy, double cz, double scale, int repetitions, List<ParticleData> particles)
    {
        int segments = 30;
        for (int rep = 0; rep < repetitions; rep++)
        {
            for (int i = 0; i < segments; i++)
            {
                double t = (double) i / segments;
                double angle = t * Math.PI * 2;
                double y = cy + Math.sin(angle * 3) * scale * 0.3;

                spawnAt(world, cx + (t - 0.5) * scale * 2, y, cz, particles);
                spawnAt(world, cx, y, cz + (t - 0.5) * scale * 2, particles);
            }
        }
    }

    private static void cross(Level world, double cx, double cy, double cz, double scale, List<ParticleData> particles)
    {
        int segments = 30;
        for (int i = 0; i < segments; i++)
        {
            double offset = ((double) i / segments - 0.5) * scale * 2;
            spawnAt(world, cx + offset, cy, cz, particles);
            spawnAt(world, cx, cy, cz + offset, particles);
        }
    }

    private static void doubleHelix(Level world, double cx, double cy, double cz, double radius, int repetitions, List<ParticleData> particles)
    {
        int segments = 50;
        for (int rep = 0; rep < repetitions; rep++)
        {
            for (int i = 0; i < segments; i++)
            {
                double t = (double) i / segments;
                double angle = t * Math.PI * 4;
                double height = (t - 0.5) * radius * 2;

                spawnAt(world, cx + Math.cos(angle) * radius * 0.5, cy + height, cz + Math.sin(angle) * radius * 0.5, particles);
                spawnAt(world, cx + Math.cos(angle + Math.PI) * radius * 0.5, cy + height, cz + Math.sin(angle + Math.PI) * radius * 0.5, particles);
            }
        }
    }

    private static void burst(Level world, double cx, double cy, double cz, double scale, int repetitions, List<ParticleData> particles)
    {
        int rays = 12;
        int pointsPerRay = 8;
        for (int rep = 0; rep < repetitions; rep++)
        {
            for (int ray = 0; ray < rays; ray++)
            {
                double angle = (Math.PI * 2 * ray) / rays;
                for (int point = 0; point < pointsPerRay; point++)
                {
                    double distance = (scale / pointsPerRay) * (point + 1);
                    spawnAt(world, cx + Math.cos(angle) * distance, cy, cz + Math.sin(angle) * distance, particles);
                }
            }
        }
    }

    private static void ring(Level world, double cx, double cy, double cz, double radius, int repetitions, List<ParticleData> particles)
    {
        int count = 40;
        for (int rep = 0; rep < repetitions; rep++)
        {
            double ringHeight = cy + (rep - repetitions / 2.0) * 0.5;
            for (int i = 0; i < count; i++)
            {
                double angle = (Math.PI * 2 * i) / count;
                spawnAt(world, cx + Math.cos(angle) * radius, ringHeight, cz + Math.sin(angle) * radius, particles);
            }
        }
    }

    private static void star(Level world, double cx, double cy, double cz, double radius, int repetitions, List<ParticleData> particles)
    {
        int points = 5;
        for (int rep = 0; rep < repetitions; rep++)
        {
            for (int i = 0; i < points * 2; i++)
            {
                double angle = (Math.PI * 2 * i) / (points * 2);
                double r = (i % 2 == 0) ? radius : radius * 0.4;
                spawnAt(world, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r, particles);
            }
        }
    }

    private static void unionJack(Level world, double cx, double cy, double cz, double scale, List<ParticleData> particles)
    {
        int segments = 30;
        for (int i = 0; i < segments; i++)
        {
            double offset = ((double) i / segments - 0.5) * scale * 2;
            spawnAt(world, cx + offset, cy, cz + offset, particles);
            spawnAt(world, cx + offset, cy, cz - offset, particles);
            spawnAt(world, cx + offset, cy, cz, particles);
            spawnAt(world, cx, cy, cz + offset, particles);
        }

        for (int i = 0; i <= segments / 4; i++)
        {
            double offset = ((double) i / (segments / 4)) * scale * 0.3;
            spawnAt(world, cx - scale + offset, cy, cz + scale - offset, particles);
            spawnAt(world, cx + scale - offset, cy, cz + scale - offset, particles);
            spawnAt(world, cx - scale + offset, cy, cz - scale + offset, particles);
            spawnAt(world, cx + scale - offset, cy, cz - scale + offset, particles);
        }
    }

    private static void circle(Level world, double cx, double cy, double cz, double radius, int repetitions, List<ParticleData> particles)
    {
        int count = 20;
        for (int rep = 0; rep < repetitions; rep++)
        {
            double repRadius = radius * (rep + 1) / repetitions;
            for (int i = 0; i < count; i++)
            {
                double angle = (Math.PI * 2 * i) / count;
                spawnAt(world, cx + Math.cos(angle) * repRadius, cy, cz + Math.sin(angle) * repRadius, particles);
            }
        }
    }

    private static void spawnAt(Level world, double x, double y, double z, List<ParticleData> particles)
    {
        for (ParticleData data : particles)
        {
            ParticleOptions particle = particleWithColor(data.particleId, data.color);
            if (particle == null)
            {
                continue;
            }

            for (int i = 0; i < data.count; i++)
            {
                double offsetX = (world.random.nextDouble() - 0.5) * data.spread;
                double offsetY = (world.random.nextDouble() - 0.5) * data.spread;
                double offsetZ = (world.random.nextDouble() - 0.5) * data.spread;

                double velX = (world.random.nextDouble() - 0.5) * data.speed * 2;
                double velY = (world.random.nextDouble() - 0.5) * data.speed * 2;
                double velZ = (world.random.nextDouble() - 0.5) * data.speed * 2;

                world.addParticle(particle, x + offsetX, y + offsetY, z + offsetZ, velX, velY, velZ);
            }
        }
    }

    @Nullable
    public static ParticleOptions particleWithColor(String particleId, int color)
    {
        if (particleId.equals("minecraft:dust"))
        {
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            return new DustParticleOptions(new Vector3f(r, g, b), 1.0f);
        }
        ResourceLocation location = ResourceLocation.tryParse(particleId);
        if (location != null && BuiltInRegistries.PARTICLE_TYPE.get(location) instanceof ParticleOptions options)
        {
            return options;
        }
        return null;
    }
}
