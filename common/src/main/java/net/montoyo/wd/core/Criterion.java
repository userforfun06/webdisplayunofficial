package net.montoyo.wd.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;

/**
 * Simple criterion trigger implementation for WebDisplays advancements.
 * Uses SimpleCriterionTrigger as base for simple instance-based triggers.
 */
public class Criterion extends SimpleCriterionTrigger<Criterion.TriggerInstance> {
    private final String id;
    private final ResourceLocation resourceLocation;

    public Criterion(String id) {
        this.id = id;
        this.resourceLocation = ResourceLocation.fromNamespaceAndPath("webdisplays", id);
    }

    @Override
    public Codec<TriggerInstance> codec() {
        return RecordCodecBuilder.create(instance -> instance.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));
    }

    public ResourceLocation getId() {
        return resourceLocation;
    }

    public String getIdString() {
        return id;
    }
    
    public void trigger(ServerPlayer player) {
        this.trigger(player, triggerInstance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
    }
}
