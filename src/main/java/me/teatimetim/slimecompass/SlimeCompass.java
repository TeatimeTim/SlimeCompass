package me.teatimetim.slimecompass;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class SlimeCompass extends JavaPlugin implements Listener {

    private final NamespacedKey _key = new NamespacedKey(this, "slime_compass");

    private final String _itemName = ChatColor.AQUA + "Slime Compass";
    private final List<String> _itemLore = new LinkedList<String>(){{add(ChatColor.DARK_PURPLE + "Right-click to check for slime chunks.");}};

    private final int _searchRadius = 1;

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
        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();

        //Only continue if player right-clicked with the slime compass in the overworld
        if (NotRightClick(event)
                || ItemIsNotSlimeCompass(heldItem)
                || PlayerIsNotInOverworld(player))
            return;

        //If the compass were used to click a lodestone it would act like a regular compass; we don't want this.
        if (ClickedBlockIsLodestone(event.getClickedBlock())) {
            event.setCancelled(true);
            return;
        }

        long seed = player.getWorld().getSeed();
        Location playerLocation = player.getLocation();
        Chunk playerChunk = playerLocation.getChunk();

        int chunkX = playerChunk.getX();
        int chunkZ = playerChunk.getZ();

        //Find all slime chunks within 3x3 radius
        int chunkCount = 0;

        Chunk closestSlimeChunk = playerChunk;
        double shortestDistance = Double.MAX_VALUE;

        for (int x = -_searchRadius; x <= _searchRadius; x++) {
            for (int z = -_searchRadius; z <= _searchRadius; z++) {
                Chunk currentChunk = player.getWorld().getChunkAt(chunkX + x, chunkZ + z);

                if (!LocationIsSlimeChunk(seed, currentChunk)) continue;

                chunkCount++;

                Location chunkCenter = currentChunk.getBlock(8, 64, 8).getLocation();
                double distanceFromPlayer = playerLocation.distance(chunkCenter);

                if (distanceFromPlayer > shortestDistance) continue;

                closestSlimeChunk = currentChunk;
                shortestDistance = distanceFromPlayer;
            }
        }

        if (chunkCount == 0) {
            player.sendMessage(ChatColor.RED + "There are no slime chunks nearby.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Found " + chunkCount + " slime chunks nearby. Pointing to nearest slime chunk...");
        PointCompassTowardsChunkCenter(heldItem, closestSlimeChunk);
    }

    private void PointCompassTowardsChunkCenter(ItemStack compass, Chunk chunk) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        Location chunkCenter = chunk.getBlock(8, 64, 8).getLocation();
        compassMeta.setLodestone(chunkCenter);

        compass.setItemMeta(compassMeta);
    }

    private void InitSlimeCompassRecipe() {
        ItemStack item = CreateSlimeCompassItem();

        ShapedRecipe recipe = new ShapedRecipe(_key, item);

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
        item.addUnsafeEnchantment(Enchantment.INFINITY, 1);

        CompassMeta meta = (CompassMeta) item.getItemMeta();

        meta.setDisplayName(_itemName);
        meta.setLore(_itemLore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLodestoneTracked(false);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(_key, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);

        return item;
    }

    private boolean LocationIsSlimeChunk(long seed, Chunk chunk) {
        int xPosition = chunk.getX();
        int zPosition = chunk.getZ();

        //The int casting in this formula is redundant, however this is true to Minecraft's code
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
        return item == null
                || !item.getItemMeta().getPersistentDataContainer().has(_key, PersistentDataType.BYTE);
    }

    private boolean ClickedBlockIsLodestone(Block block) {
        return block != null
                && block.getType() == Material.LODESTONE;
    }
    
    private boolean PlayerIsNotInOverworld(Player player) {
        return player.getWorld().getEnvironment() != World.Environment.NORMAL;
    }
}
