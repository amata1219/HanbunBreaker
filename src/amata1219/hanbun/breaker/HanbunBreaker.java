package amata1219.hanbun.breaker;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Slab.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class HanbunBreaker extends JavaPlugin implements Listener {

	private static HanbunBreaker plugin;

	private HashSet<UUID> always = new HashSet<>();

	@Override
	public void onEnable(){
		plugin = this;

		saveDefaultConfig();

		for(String key : getConfig().getStringList("Always"))
			always.add(UUID.fromString(key));

		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin) this);

		getConfig().set("Always", always.stream().map(UUID::toString).collect(Collectors.toList()));
		saveConfig();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "ゲーム内から実行して下さい。");
			return true;
		}

		Player player = (Player) sender;
		UUID uuid = player.getUniqueId();
		boolean contains = always.contains(uuid);
		if(contains)
			always.remove(uuid);
		else
			always.add(uuid);

		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "半分破壊機能を" + (contains ? "無効" : "有効") + "にしました。"));
		return true;
	}

	public static HanbunBreaker getPlugin(){
		return plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e){
		Player player = e.getPlayer();
		if(!always.contains(player.getUniqueId()))
			return;

		Block block = e.getBlock();
		BlockData data = block.getBlockData();
		if(!(data instanceof Slab))
			return;

		Slab slab = (Slab) data;
		if(slab.getType() != Type.DOUBLE)
			return;

		double distance = player.getLocation().distance(block.getLocation());
		Location location = player.getEyeLocation();
		double y =location.toVector().clone().add(location.getDirection().clone().multiply(distance)).getY();
		/*for(double d = 0; d <= player.getLocation().distance(block.getLocation()); d += 0.1){

		}*/

		slab.setType(y - Double.valueOf(y) >= 0.5 ? Type.BOTTOM : Type.TOP);
		block.setBlockData(slab);

		e.setCancelled(true);

		if(player.getGameMode() == GameMode.CREATIVE)
			return;

		Collection<ItemStack> drops = block.getDrops();
		if(!drops.isEmpty()) for(ItemStack drop : drops){
			drop.setAmount(1);
			block.getWorld().dropItemNaturally(location.add(0.0, 0.6, 0.0), drop);
			break;
		}
	}

	public static boolean isUpper(Player player, Location blockLocation){
		Location eye = player.getEyeLocation();
		Location center = blockLocation.add(0.5, 0.5, 0.5);
		double distance = eye.distance(center);
		double y = eye.toVector().multiply(distance).getY();
		return y - Double.valueOf(y).intValue() >= 0.5;
	}

	public static boolean isUpper(Player player, Block block){
		return isUpper(player, block.getLocation());
	}

	public static double getLength(double x, double z){
		return Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2));
	}

}
