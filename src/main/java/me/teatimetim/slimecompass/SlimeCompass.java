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

    private final NamespacedKey KEY = new NamespacedKey(this, "slime_compass");

    //The underscores here are a C# holdover, and a reminder to make these configurable in the future
    private final String _itemName = ChatColor.AQUA + "Slime Compass";
    private final List<String> _itemLore = new LinkedList<String>(){{add(ChatColor.DARK_PURPLE + "Right-click to check for slime chunks.");}};
    private final int _searchRadius = 1;

    @Override
    public void onEnable() {
        System.out.println("Starting Slime Compass");
        initSlimeCompassRecipe();
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

        if (notRightClick(event)
                || itemIsNotSlimeCompass(heldItem))
            return;

        //If the compass were used to click a lodestone it would act like a regular compass; we don't want this.
        if (clickedBlockIsLodestone(event.getClickedBlock()) || playerIsNotInOverworld(player)) {
            event.setCancelled(true);
            return;
        }

        long seed = player.getWorld().getSeed();
        Location playerLocation = player.getLocation();
        Chunk playerChunk = playerLocation.getChunk();

        int chunkX = playerChunk.getX();
        int chunkZ = playerChunk.getZ();

        int chunkCount = 0;

        Chunk closestSlimeChunk = playerChunk;
        double shortestDistance = Double.MAX_VALUE;

        for (int xOffset = -_searchRadius; xOffset <= _searchRadius; xOffset++) {
            for (int zOffset = -_searchRadius; zOffset <= _searchRadius; zOffset++) {
                Chunk currentChunk = player.getWorld().getChunkAt(chunkX + xOffset, chunkZ + zOffset);

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
        pointCompassTowardsChunkCenter(heldItem, closestSlimeChunk);
    }

    private void pointCompassTowardsChunkCenter(ItemStack compass, Chunk chunk) {
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        Location chunkCenter = chunk.getBlock(8, 64, 8).getLocation();
        compassMeta.setLodestone(chunkCenter);

        compass.setItemMeta(compassMeta);
    }

    private void initSlimeCompassRecipe() {
        ItemStack item = CreateSlimeCompassItem();

        ShapedRecipe recipe = new ShapedRecipe(KEY, item);

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
        data.set(KEY, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);

        return item;
    }

    private boolean LocationIsSlimeChunk(long seed, Chunk chunk) {
        int xPosition = chunk.getX();
        int zPosition = chunk.getZ();

        Random rnd = new Random(
                seed +
                        (int) (xPosition * xPosition * 0x4c1906) +
                        (int) (xPosition * 0x5ac0db) +
                        (int) (zPosition * zPosition) * 0x4307a7L +
                        (int) (zPosition * 0x5f24f) ^ 0x3ad8025fL
        );

        return (rnd.nextInt(10) == 0);
    }

    private boolean notRightClick(PlayerInteractEvent event) {
        return event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK;
    }

    private boolean itemIsNotSlimeCompass(ItemStack item) {
        return item == null
                || !item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    private boolean clickedBlockIsLodestone(Block block) {
        return block != null
                && block.getType() == Material.LODESTONE;
    }
    
    private boolean playerIsNotInOverworld(Player player) {
        return player.getWorld().getEnvironment() != World.Environment.NORMAL;
    }
}
