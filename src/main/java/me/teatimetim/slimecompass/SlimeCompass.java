package me.teatimetim.slimecompass;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class SlimeCompass extends JavaPlugin implements Listener {

    private final String _itemName = ChatColor.AQUA + "Slime Compass";
    private final List<String> _itemLore = new LinkedList<String>(){{add(ChatColor.DARK_PURPLE + "Right-click to check for slime chunks.");}};

    @Override
    public void onEnable() {
        System.out.println("Starting Slime Compass");
        InitSlimeCompassRecipe();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        System.out.println("Stopping Slime Compass");
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (NotRightClick(event)) return;
        
        ItemStack heldItem = event.getItem();
        if (ItemIsNotSlimeCompass(heldItem)) return;

        Player player = event.getPlayer();
        if (PlayerIsNotInOverworld(player)) return;

        long seed = player.getWorld().getSeed();
        Chunk playerChunk = player.getLocation().getChunk();

        if (LocationIsSlimeChunk(seed, playerChunk.getX(), playerChunk.getZ())) {
            player.sendMessage(ChatColor.GREEN + "You are in a slime chunk!");
            
            CompassMeta compassMeta = (CompassMeta) heldItem.getItemMeta();
            Location chunkCenter = playerChunk.getBlock(8, 64, 8).getLocation();
            compassMeta.setLodestone(chunkCenter);
            //compassMeta.setLodestoneTracked(false);

            heldItem.setItemMeta(compassMeta);
            
            return;
        }

        player.sendMessage(ChatColor.RED + "You are not in a slime chunk.");
    }

    private void InitSlimeCompassRecipe() {
        ItemStack item = CreateSlimeCompassItem();

        NamespacedKey key = new NamespacedKey(this, "slime_compass");
        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape(
                " I ",
                "ISI",
                " I ");

        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.SLIME_BALL);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack CreateSlimeCompassItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) item.getItemMeta();

        meta.setDisplayName(_itemName);
        meta.setLore(_itemLore);
        meta.setLodestoneTracked(false);


        //item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        //meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);

        return item;
    }

    private boolean LocationIsSlimeChunk(long seed, int xPosition, int zPosition) {
        //The int casting in this section is redundant, however it is how the function is written in Minecraft's code
        Random rnd = new Random(
                seed +
                        (int) (xPosition * xPosition * 0x4c1906) +
                        (int) (xPosition * 0x5ac0db) +
                        (int) (zPosition * zPosition) * 0x4307a7L +
                        (int) (zPosition * 0x5f24f) ^ 0x3ad8025fL
        );

        return (rnd.nextInt(10) == 0);
    }

    private boolean NotRightClick(PlayerInteractEvent event) {
        return event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK;
    }

    private boolean ItemIsNotSlimeCompass(ItemStack item) {
        return (item == null
                || item.getType() != Material.COMPASS
                //|| !item.containsEnchantment(Enchantment.INFINITY)
                || !item.getItemMeta().getDisplayName().equals(_itemName)
                || !item.getItemMeta().getLore().equals(_itemLore));
    }
    
    private boolean PlayerIsNotInOverworld(Player player) {
        return player.getWorld().getEnvironment() != World.Environment.NORMAL;
    }
}
