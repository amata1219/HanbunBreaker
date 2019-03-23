package amata1219.hanbun.breaker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class HanbunBreaker extends JavaPlugin implements Listener {

	private static HanbunBreaker plugin;
	private static Method setData;

	private HashSet<UUID> always = new HashSet<>();

	@Override
	public void onEnable(){
		plugin = this;

		String version = "v" + getServer().getClass().getPackage().getName().replaceFirst(".*(\\d+_\\d+_R\\d+).*", "$1");
		try {
			setData = Class.forName("org.bukkit.craftbukkit." + version + "block.CraftBlock").getMethod("setData", byte.class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}

		saveDefaultConfig();

		for(String key : getConfig().getStringList("Always"))
			always.add(UUID.fromString(key));

		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin) this);

		getConfig().set("Always", always);
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

		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "常時半分破壊機能を" + (contains ? "無効" : "有効") + "にしました。"));
		return true;
	}

	public static HanbunBreaker getPlugin(){
		return plugin;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e){
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND)
			System.out.println(e.getClickedBlock().getType().toString());
	}

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e){
		Player player = e.getPlayer();
		if(!player.isSneaking())
			return;

		Block block = e.getBlock();
		if(!isDoubleSlabs(block))
			return;

		block.setType(Material.valueOf(block.getType().toString().substring(14)));

		Location location = block.getLocation();
		byte data = block.getData();
		if(isUpper(player, location))
			if(data < 8)
				setData(block, data);
			else
				setData(block, (byte) (data - 8));
		else
			if(data < 8)
				setData(block, (byte) (data + 8));
			else
				setData(block, data);

		if(player.getGameMode() == GameMode.CREATIVE)
			return;

		Collection<ItemStack> drops = block.getDrops();
		if(!drops.isEmpty()) for(ItemStack drop : drops){
			drop.setAmount(1);
			block.getWorld().dropItemNaturally(location.add(0.0, 0.1, 0.0), drop);
			break;
		}
	}

	public static boolean isDoubleSlabs(Block block){
		String name = block.getType().toString();
		return name.startsWith("LEGACY_DOUBLE_") && !name.endsWith("PLANT");
	}

	/*
	 * player - 破壊者
	 * block - ターゲット、isDoubleSlabs(block) == true であることが保証されなければならない
	 */
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

	public static void setData(Block block, byte data){
		try {
			setData.invoke(block, data);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}
