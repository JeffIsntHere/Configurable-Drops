package configurable.drops.parser.loot;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import configurable.drops.ConfigurableDrops;
import configurable.drops.parser.File;
import configurable.drops.parser.Parser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.ArrayList;

public class LootParser extends Parser
{
    public static final LootParser instance = new LootParser();
    private LootParser(){}
    public ArrayList<Loot> loots = new ArrayList<>();
    protected ItemParser itemParser = null;
    protected String item = null;
    protected double quota = 0.0d;
    protected double reducer = 1.0d;
    protected double minimumPower = 0;
    protected double adder = 0;
    protected final ArrayList<String> list = new ArrayList<>();
    protected boolean hasList = false;
    @Override
    protected void process()
    {
        if(super.parentCount == 0)
        {
            this.item = null;
            this.quota = 0.0d;
            this.reducer = 1.0d;
            this.minimumPower = 0;
            this.adder = 0;
            this.list.clear();
            this.hasList = false;
            super.spinUntilOpen();
            super.parentCount++;
        }
        else if(super.parentCount == 1)
        {
            final char key = super.spinUntilNotWhitespace();
            if(key == '}' || !super.spinUntilOpenOrClose())
            {
                if(this.item != null)
                {
                    if(!this.hasList)
                    {
                        this.list.add("ALL");
                    }
                    this.loots.add(new Loot(this.itemParser, this.item, this.quota, this.reducer, this.minimumPower, this.adder));
                    this.loots.getLast().list.addAll(this.list);
                }
                super.parentCount--;
            }
            else if(key == 'i')
            {
                this.item = super.parseValueToKey();
            }
            else if(key == 'q')
            {
                try
                {
                    this.quota = Double.valueOf(super.parseValueToKey());
                }
                catch(NumberFormatException e)
                {
                    this.quota = 1.0d;
                }
            }
            else if(key == 'r')
            {
                try
                {
                    this.reducer = Double.valueOf(super.parseValueToKey());
                }
                catch(NumberFormatException e)
                {
                    this.reducer = 1.0d;
                }
            }
            else if(key == 'm')
            {
                try
                {
                    this.minimumPower = Double.valueOf(super.parseValueToKey());
                }
                catch(NumberFormatException e)
                {
                    this.minimumPower = 0;
                }
            }
            else if(key == 'a')
            {
                try
                {
                    this.adder = Double.valueOf(super.parseValueToKey());
                }
                catch(NumberFormatException e)
                {
                    this.adder = 0;
                }
            }
            else if(key == 'l')
            {
                this.hasList = true;
                super.parseValueOrValueArrayToKeyArrayList(this.list);
            }
        }
    }
    public void reload()
    {
        if(ServerLifecycleHooks.getCurrentServer() == null)
        {
            return;
        }
        loots.clear();
        this.itemParser = new ItemParser(ServerLifecycleHooks.getCurrentServer().registryAccess());
        final File file = new File();
        final Reader reader = file.getFileReader("loot.txt");
        super.parseFromInput(reader);
        File.closeReader(reader);
        this.itemParser = null;
        this.loots.removeIf(loot -> loot.item.isEmpty());
    }
    public static double getPower(final LivingEntity livingEntity)
    {
        try
        {
            final AttributeMap attributeMap = livingEntity.getAttributes();
            final AttributeInstance armorToughness = attributeMap.getInstance(Attributes.ARMOR_TOUGHNESS);
            final AttributeInstance attackDamageAttribute = attributeMap.getInstance(Attributes.ATTACK_DAMAGE);
            final AttributeInstance attackSpeed = attributeMap.getInstance(Attributes.ATTACK_SPEED);
            final AttributeInstance maxHealth = attributeMap.getInstance(Attributes.MAX_HEALTH);
            final AttributeInstance movementSpeed = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
            double power = 1.0d;
            double divisor = 1.0d;
            if(armorToughness != null)
            {
                power += armorToughness.getBaseValue();
            }
            if(attackDamageAttribute != null)
            {
                power *= attackDamageAttribute.getBaseValue();
                divisor *= 3.0d;
            }
            if(attackSpeed != null)
            {
                power *= attackSpeed.getBaseValue();
            }
            if(maxHealth != null)
            {
                power *= maxHealth.getBaseValue();
                divisor *= 20.0d;
            }
            if(movementSpeed != null)
            {
                power *= movementSpeed.getBaseValue();
                divisor *= 0.23000000417232513d;
            }
            return power / divisor;
        }
        catch(IllegalArgumentException illegalArgumentException)
        {
            return 0.0d;
        }
    }
    public void dropForLivingEntity(@NotNull final LivingEntity livingEntity)
    {
        final Level level = livingEntity.level();
        final Vec3 position = livingEntity.position();
        final double power = LootParser.getPower(livingEntity);
        for(Loot loot : this.loots)
        {
            final boolean all = !loot.list.isEmpty() && loot.list.getFirst().equals("ALL");
            final String encodeId = livingEntity.getEncodeId();
            ConfigurableDrops.logger.debug(encodeId);
            for(String string : loot.list)
            {
                if(string.equals(encodeId))
                {
                    if(!all)
                    {
                        loot.dropAtLocation(level, position, power);
                    }
                    else
                    {
                        return;
                    }
                }
            }
            if(all)
            {
                loot.dropAtLocation(level, position, power);
            }
        }
    }
}
