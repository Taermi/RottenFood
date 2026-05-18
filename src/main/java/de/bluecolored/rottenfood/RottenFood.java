/*
 * The MIT License (MIT)
 *
 * Copyright (c) Blue <https://www.bluecolored.de>
 * Copyright (c) CraftedNature <https://www.craftednature.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.bluecolored.rottenfood;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

public class RottenFood extends JavaPlugin implements Listener {
	public static final String PLUGIN_ID = "rottenfood";

	public static final NamespacedKey KEY_LAST_UPDATE = new NamespacedKey(PLUGIN_ID, "last_update");
	public static final NamespacedKey KEY_AGE = new NamespacedKey(PLUGIN_ID, "age");

	private List<ItemConfig> itemConfigs;

	private long updateIntervall = 20;
	private BukkitTask updateTask;

	private String ageFormat;

	@Override
	public void onEnable(){
		getLogger().info("Initializing...");

		itemConfigs = Lists.newArrayList();

		try {
			load();
		} catch (IOException e) {
			getLogger().severe("Failed to load or create config-file!");
			e.printStackTrace();
			return;
		}

		getServer().getPluginManager().registerEvents(this, this);
		startUpdateTask();
	}

	@Override
	public void onDisable(){
		if (updateTask != null) updateTask.cancel();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")){
			getLogger().info("Reloading...");

			try {
				load();
			} catch (IOException e) {
				getLogger().severe("Failed to load or create config-file!");
				e.printStackTrace();
				return true;
			}

			startUpdateTask();
			return true;
		}
		return false;
	}

	public void startUpdateTask(){
		if (updateTask != null) updateTask.cancel();
		updateTask = getServer().getScheduler().runTaskTimer(this, () -> updatePlayerInventories(), 0L, updateIntervall);
	}

	public void updatePlayerInventories(){
		for (Player p : getServer().getOnlinePlayers()){
			updatePlayerInventory(p, false);
		}
	}

	public void updatePlayerInventory(Player p, boolean forceUpdate){
		PlayerInventory inv = p.getInventory();

		updateInventory(inv, forceUpdate);

		ItemStack[] armor = inv.getArmorContents();
		boolean armorChanged = false;
		for (int i = 0; i < armor.length; i++){
			if (armor[i] == null || armor[i].getType().isAir()) continue;
			ItemStack updated = updateItem(armor[i], inv, forceUpdate);
			if (updated != armor[i]){
				armor[i] = updated;
				armorChanged = true;
			}
		}
		if (armorChanged) inv.setArmorContents(armor);

		ItemStack offhand = inv.getItemInOffHand();
		if (!offhand.getType().isAir()){
			ItemStack updated = updateItem(offhand, inv, forceUpdate);
			if (updated != offhand){
				inv.setItemInOffHand(updated != null ? updated : new ItemStack(Material.AIR));
			}
		}
	}

	@EventHandler
	public void onOpenInventory(InventoryOpenEvent evt){
		scheduleInventoryUpdate(evt.getInventory(), true);
	}

	public void scheduleInventoryUpdate(Inventory inv, boolean forceUpdate){
		getServer().getScheduler().runTask(this, () -> updateInventory(inv, forceUpdate));
	}

	public void updateInventory(Inventory inv, boolean forceUpdate){
		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++){
			ItemStack is = contents[i];
			if (is == null || is.getType().isAir()) continue;
			ItemStack updated = updateItem(is, inv, forceUpdate);
			if (updated != is){
				inv.setItem(i, updated);
			}
		}
	}

	public ItemStack updateItem(ItemStack is, Inventory modifierScope, boolean forceUpdate){
		for (ItemConfig ic : itemConfigs){
			if (!ic.matchesItemStack(is)) continue;

			ItemMeta meta = is.getItemMeta();
			if (meta == null) continue;

			PersistentDataContainer pdc = meta.getPersistentDataContainer();
			long lastUpdate = pdc.getOrDefault(KEY_LAST_UPDATE, PersistentDataType.LONG, -1L);
			long age = pdc.getOrDefault(KEY_AGE, PersistentDataType.LONG, 0L);

			long now = (long) Math.floor((double) System.currentTimeMillis() / (double) ic.getStackingInterval()) * ic.getStackingInterval();
			long delta = now - lastUpdate;

			double modifier = 1;
			for (ItemAgeingModifierConfig mod : ic.getAgingModifiers()){
				ExtendedItemType type = mod.getItem();
				int count = mod.getMinItemCount();

				if (count > countItems(modifierScope, type)) continue;

				modifier *= mod.getAgeMultiplier();
			}

			long modDelta = (long)((double) delta * modifier);
			modDelta = (long) Math.floor((double) modDelta / (double) ic.getStackingInterval()) * ic.getStackingInterval();

			long newAge = age + modDelta;

			if (lastUpdate == -1){
				newAge = 0;
				delta = 0;
			}

			Component name = null;
			Component lore = null;
			ItemStack newIs = is;

			for (ItemAgeStateConfig as : ic.getAgeStates()){
				if (as.getAge() > newAge) break;

				if (as.getName() != null) name = as.getName();
				if (as.getLore() != null) lore = as.getLore();

				if (as.getReplacement() != null){
					ExtendedItemType repl = as.getReplacement();
					Material replMat = repl.getMaterial();

					if (replMat == null || replMat.isAir()){
						newIs = null;
					} else {
						newIs = new ItemStack(replMat, is.getAmount());
					}

					break;
				} else {
					newIs = is;
				}
			}

			if (newIs == null) return null;

			ItemStack result = newIs == is ? is.clone() : newIs;
			ItemMeta newMeta = result.getItemMeta();
			if (newMeta == null) return is;

			newMeta.getPersistentDataContainer().set(KEY_LAST_UPDATE, PersistentDataType.LONG, now);
			newMeta.getPersistentDataContainer().set(KEY_AGE, PersistentDataType.LONG, newAge);

			List<Component> lorelist = Lists.newArrayList();
			if (lore != null) lorelist.add(lore);
			if (ic.isShowingAge()){
				String ageString = formatTimeInterval(newAge, ageFormat);
				lorelist.add(LegacyComponentSerializer.legacyAmpersand().deserialize(ageString));
			}

			if (name != null) newMeta.displayName(name);
			newMeta.lore(lorelist.isEmpty() ? null : lorelist);

			result.setItemMeta(newMeta);

			if (!result.isSimilar(is) || forceUpdate){
				return result;
			}

			return is;
		}

		return is;
	}

	public int countItems(Inventory inv, ExtendedItemType type){
		ItemStack[] contents;

		if (inv instanceof PlayerInventory playerInv){
			ItemStack[] main = playerInv.getContents();
			ItemStack[] armor = playerInv.getArmorContents();
			ItemStack[] extra = playerInv.getExtraContents();
			contents = new ItemStack[main.length + armor.length + extra.length];
			System.arraycopy(main, 0, contents, 0, main.length);
			System.arraycopy(armor, 0, contents, main.length, armor.length);
			System.arraycopy(extra, 0, contents, main.length + armor.length, extra.length);
		} else {
			contents = inv.getContents();
		}

		int count = 0;

		for (ItemStack is : contents){
			if (is != null && type.matches(is)){
				count += is.getAmount();
			}
		}

		return count;
	}

	public void load() throws IOException {
		List<ItemConfig> configs = Lists.newArrayList();

		if (!getDataFolder().exists()) getDataFolder().mkdirs();

		File configFile = new File(getDataFolder(), "defaultConfig.conf");
		if (!configFile.exists()){
			getLogger().warning("No config file found! Generating new config..");
			getLogger().info("You can use '/rottenfood reload' to reload the config file after you edited it!");
			saveResource("defaultConfig.conf", false);
		}

		TypeSerializerCollection serializers = TypeSerializerCollection.defaults().childBuilder()
			.register(ItemConfig.TOKEN, new ItemConfigSerializer())
			.register(ExtendedItemType.TOKEN, new ExtendedItemTypeSerializer())
			.build();

		HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
			.path(configFile.toPath())
			.defaultOptions(opts -> opts.serializers(serializers))
			.build();

		ConfigurationNode config = loader.load();

		for (ConfigurationNode iconf : config.node("item-configs").childrenList()){
			configs.add(iconf.get(ItemConfig.TOKEN));
		}

		this.updateIntervall = config.node("update-intervall").getLong(20);
		this.ageFormat = config.node("age-time-format").getString("&7This item is &f%D &7days old.");

		this.itemConfigs.clear();
		this.itemConfigs = configs;
		getLogger().info("Config loaded successfully!");
	}

	private static String formatTimeInterval(long time, String format){
		TimeUnit c = TimeUnit.MILLISECONDS;

		long S = c.toSeconds(time);
		long M = c.toMinutes(time);
		long H = c.toHours(time);
		long D = c.toDays(time);

		long s = c.toSeconds(time - TimeUnit.MINUTES.toMillis(M));
		long m = c.toMinutes(time - TimeUnit.HOURS.toMillis(H));
		long h = c.toHours(time - TimeUnit.DAYS.toMillis(D));

		String r = format;
		r = r.replace("%S", "" + S);
		r = r.replace("%M", "" + M);
		r = r.replace("%H", "" + H);
		r = r.replace("%D", "" + D);
		r = r.replace("%s", (s > 9 ? "" : "0") + s);
		r = r.replace("%m", (m > 9 ? "" : "0") + m);
		r = r.replace("%h", (h > 9 ? "" : "0") + h);

		return r;
	}

}
