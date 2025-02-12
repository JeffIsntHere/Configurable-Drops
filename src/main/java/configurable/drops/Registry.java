package configurable.drops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import configurable.drops.parser.loot.Loot;
import configurable.drops.parser.loot.LootParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class Registry
{
    public static int reloadLoot(CommandContext<CommandSourceStack> commandContext)
    {
        LootParser.instance.reload();
        commandContext.getSource().sendSuccess(() -> Component.translatable("successfully reloaded!"), true);
        commandContext.getSource().sendSuccess(() -> Component.translatable("loaded: " + LootParser.instance.loots.size() + " items."), true);
        return 1;
    }
    public static int getPower(CommandContext<CommandSourceStack> commandContext, Collection<? extends Entity> entities)
    {
        final Entity sender = commandContext.getSource().getEntity();
        if(sender == null)
        {
            ConfigurableDrops.logger.info("");
            for(Entity entity : entities)
            {
                if(entity instanceof LivingEntity livingEntity)
                {
                    ConfigurableDrops.logger.info("Entity: " + livingEntity);
                    ConfigurableDrops.logger.info(">Power: " + LootParser.getPower(livingEntity));
                    ConfigurableDrops.logger.info("");
                }
            }
        }
        else
        {
            sender.sendSystemMessage(Component.literal(""));
            for(Entity entity : entities)
            {
                if(entity instanceof LivingEntity livingEntity)
                {
                    sender.sendSystemMessage(Component.literal("§7Entity: §f" + livingEntity));
                    sender.sendSystemMessage(Component.literal("§7>Power: §f" + LootParser.getPower(livingEntity)));
                    sender.sendSystemMessage(Component.literal(""));
                }
            }
        }
        return 1;
    }
    public static int dumpLoot(CommandContext<CommandSourceStack> commandContext, boolean dumpData)
    {
        final Entity entity = commandContext.getSource().getEntity();
        if(entity == null)
        {
            ConfigurableDrops.logger.info("");
            for(Loot loot : LootParser.instance.loots)
            {
                ConfigurableDrops.logger.info("Item type: " + loot.item.getItem());
                if(dumpData)
                {
                    ConfigurableDrops.logger.info(">Item data: " + loot.item.getComponents());
                }
                ConfigurableDrops.logger.info(">Quota: " + loot.quota);
                ConfigurableDrops.logger.info(">Reducer: " + loot.reducer);
                ConfigurableDrops.logger.info(">MinimumPower: " + loot.minimumPower);
                ConfigurableDrops.logger.info(">Adder: " + loot.adder);
                for(String string : loot.list)
                {
                    entity.sendSystemMessage(Component.literal("> " + string));
                }
                ConfigurableDrops.logger.info("");
            }
        }
        else
        {
            entity.sendSystemMessage(Component.literal(""));
            for(Loot loot : LootParser.instance.loots)
            {
                entity.sendSystemMessage(Component.literal("§7Item type: §f" + loot.item.getItem()));
                if(dumpData)
                {
                    entity.sendSystemMessage(Component.literal("§7>Item data: §f" + loot.item.getComponents()));
                }
                entity.sendSystemMessage(Component.literal("§7>Quota: §f" + loot.quota));
                entity.sendSystemMessage(Component.literal("§7>Reducer: §f" + loot.reducer));
                entity.sendSystemMessage(Component.literal("§7>MinimumPower: §f" + loot.minimumPower));
                entity.sendSystemMessage(Component.literal("§7>Adder: §f" + loot.adder));
                entity.sendSystemMessage(Component.literal("§7>List: §f"));
                for(String string : loot.list)
                {
                    entity.sendSystemMessage(Component.literal("§7>§f " + string));
                }
                entity.sendSystemMessage(Component.literal(""));
            }
        }
        return 1;
    }
    public static int dropAllLoot(CommandContext<CommandSourceStack> commandContext)
    {
        final Entity entity = commandContext.getSource().getEntity();
        if(entity == null)
        {
            commandContext.getSource().sendFailure(Component.literal("command must be ran by an entity!"));
        }
        else
        {
            final Level level = entity.level();
            final Vec3 location = entity.position();
            for(Loot loot : LootParser.instance.loots)
            {
                level.addFreshEntity(new ItemEntity(level, location.x, location.y, location.z, loot.item.copyWithCount(1)));
            }
        }
        return 1;
    }
    public static int dropAllLoot(CommandContext<CommandSourceStack> commandContext, final double power)
    {
        final Entity entity = commandContext.getSource().getEntity();
        if(entity == null)
        {
            commandContext.getSource().sendFailure(Component.literal("command must be ran by an entity!"));
        }
        else
        {
            final Level level = entity.level();
            final Vec3 location = entity.position();
            for(Loot loot : LootParser.instance.loots)
            {
                loot.dropAtLocation(level, location, power);
            }
        }
        return 1;
    }
    public static void registerCommands(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        commandDispatcher.register(Commands.literal("configurableDrops")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("reloadLoot").executes(Registry::reloadLoot))
                .then(Commands.literal("dumpLoot").executes(commandContext -> Registry.dumpLoot(commandContext, false))
                        .then(Commands.argument("dumpData", BoolArgumentType.bool())
                                .executes(commandContext -> Registry.dumpLoot(commandContext, BoolArgumentType.getBool(commandContext, "dumpData")))))
                .then(Commands.literal("dropAllLoot").executes(Registry::dropAllLoot)
                        .then(Commands.argument("power", DoubleArgumentType.doubleArg())
                                .executes(commandContext -> Registry.dropAllLoot(commandContext, DoubleArgumentType.getDouble(commandContext, "power")))))
                .then(Commands.literal("getPower")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(commandContext -> Registry.getPower(commandContext, EntityArgument.getEntities(commandContext, "targets")))
                        )
                )
        );
    }
}
