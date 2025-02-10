package configurable.drops.parser.loot;

import configurable.drops.parser.File;
import configurable.drops.parser.Parser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.entity.LivingEntity;
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
    @Override
    protected void process()
    {
        if(super.parentCount == 0)
        {
            this.item = null;
            this.quota = 0.0d;
            this.reducer = 1.0d;
            this.minimumPower = 0;
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
                    this.loots.add(new Loot(this.itemParser, this.item, this.quota, this.reducer, this.minimumPower));
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
            final double power = ((float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE) + 3.0f) * (livingEntity.getMaxHealth() + Math.pow((float) livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 2.0f)
                    * Math.sqrt((float) livingEntity.getAttributeValue(Attributes.MOVEMENT_SPEED) + (float) livingEntity.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY))) / 120.0f;
            return power;
        }
        catch(IllegalArgumentException illegalArgumentException)
        {
            return 0;
        }
    }
    public void dropForLivingEntity(@NotNull final LivingEntity livingEntity)
    {
        final Level level = livingEntity.level();
        final Vec3 position = livingEntity.position();
        //power =
        //(attack damage + 3) * (hp + armor point^2) * sqrt(movement speed on land + movement speed on sea)
        final double power = LootParser.getPower(livingEntity);
        for(Loot loot : this.loots)
        {
            loot.dropAtLocation(level, position, power);
        }
    }
}
