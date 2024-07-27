package me.zombie_striker.qg.guns.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.zombie_striker.qg.QAMain;
import me.zombie_striker.qg.ammo.Ammo;
import me.zombie_striker.qg.api.QADamageEvent;
import me.zombie_striker.qg.api.QAHeadShotEvent;
import me.zombie_striker.qg.api.QAWeaponDamageBlockEvent;
import me.zombie_striker.qg.api.QAWeaponDamageEntityEvent;
import me.zombie_striker.qg.api.QualityArmory;
import me.zombie_striker.qg.boundingbox.AbstractBoundingBox;
import me.zombie_striker.qg.boundingbox.BoundingBoxManager;
import me.zombie_striker.qg.guns.Gun;
import me.zombie_striker.qg.handlers.AimManager;
import me.zombie_striker.qg.handlers.BlockCollisionUtil;
import me.zombie_striker.qg.handlers.BulletWoundHandler;
import me.zombie_striker.qg.handlers.IronsightsHandler;
import me.zombie_striker.qg.handlers.ParticleHandlers;
import me.zombie_striker.qg.handlers.ProtocolLibHandler;
import me.zombie_striker.qg.handlers.Update19OffhandChecker;
import me.zombie_striker.qg.hooks.CoreProtectHook;
import me.zombie_striker.qg.hooks.protection.ProtectionHandler;
import me.zombie_striker.qg.item.CustomBaseObject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import static me.zombie_striker.qg.guns.Gun.updateAmmo;

public class GunUtil {
	public static HashMap<UUID, BukkitTask> rapidfireshooters = new HashMap();
	public static HashMap<UUID, Double> highRecoilCounter = new HashMap();
	protected static HashMap<UUID, Location> AF_locs = new HashMap();
	protected static HashMap<UUID, BukkitTask> AF_tasks = new HashMap();

	public static void shootHandler(Gun g, Player p) {
		shootHandler(g, p, g.getBulletsPerShot());
	}

	public static void shootHandler(Gun g, Player p, int numberOfBullets) {
		double sway = g.getSway() * AimManager.getSway(g, p.getUniqueId());
		if (g.usesCustomProjctiles()) {
			for(int i = 0; i < numberOfBullets; ++i) {
				Vector go = p.getLocation().getDirection().normalize();
				go.add(new Vector(Math.random() * 2.0D * sway - sway, Math.random() * 2.0D * sway - sway, Math.random() * 2.0D * sway - sway)).normalize();
				Vector two = go.clone();
				g.getCustomProjectile().spawn(g, p.getEyeLocation(), p, two);
			}
		} else {
			shootInstantVector(g, p, sway, g.getDamage(), g.getBulletsPerShot(), g.getMaxDistance());
		}

	}

	public static double getTargetedSolidMaxDistance(Vector v, Location start, double maxDistance) {
		Location test = start.clone();
		Block previous = null;

		for(double i = 0.0D; i < maxDistance; i += v.length()) {
			if (test.getBlock() == previous) {
				previous = test.getBlock();
				test.add(v);
			} else {
				if (test.getBlock().getType() != Material.AIR && isSolid(test.getBlock(), test)) {
					return start.distance(test);
				}

				previous = test.getBlock();
				test.add(v);
			}
		}

		return maxDistance;
	}

	public static void shootInstantVector(Gun g, Player p, double sway, double damage, int shots, int range) {
		boolean timingsReport = false;
		long time1 = System.currentTimeMillis();
		long time2 = 0L;
		long time3 = 0L;
		long time4point5 = 0L;
		long time4 = 0L;

		label294:
		for(int i = 0; i < shots; ++i) {
			Location start = p.getEyeLocation().clone();
			start.add(p.getVelocity());
			Vector normalizedDirection = p.getLocation().getDirection().normalize();
			normalizedDirection.add(new Vector(Math.random() * 2.0D * sway - sway, Math.random() * 2.0D * sway - sway, Math.random() * 2.0D * sway - sway));
			normalizedDirection = normalizedDirection.normalize();
			Vector step = normalizedDirection.clone().multiply(QAMain.bulletStep);
			Entity hitTarget = null;
			AbstractBoundingBox hitBox = null;
			Location bulletHitLoc = null;
			double maxDistance = getTargetedSolidMaxDistance(step, start, (double)range);
			double maxEntityDistance = maxDistance;
			double maxEntityDistanceSquared = maxDistance * maxDistance;
			List<Location> blocksThatWillPLAYBreak = new ArrayList();
			List<Block> blocksThatWillBreak = new ArrayList();
			Location centerTest = start.clone().add(normalizedDirection.clone().multiply(maxDistance / 2.0D));
			Iterator var35 = centerTest.getWorld().getNearbyEntities(centerTest, maxDistance / 2.0D, maxDistance / 2.0D, maxDistance / 2.0D).iterator();

			while(true) {
				while(true) {
					Entity e;
					double entityDistanceSquared;
					double entityDistance;
					AbstractBoundingBox box;
					Location bulletLocationTest;
					Player player;
					do {
						do {
							do {
								do {
									do {
										do {
											do {
												if (!var35.hasNext()) {
													time2 = System.currentTimeMillis();
													if (hitTarget != null) {
														if (!(hitTarget instanceof Player) || QualityArmory.allowGunsInRegion(hitTarget.getLocation())) {
															boolean headshot = hitBox.allowsHeadshots() && hitBox.intersectsHead(bulletHitLoc, hitTarget);
															if (headshot) {
																QAMain.DEBUG("Headshot!");
																if (QAMain.headshotPling) {
																	try {
																		p.playSound(p.getLocation(), QAMain.headshot_sound, 2.0F, 1.0F);
																		if (!QAMain.isVersionHigherThan(1, 9)) {
																			try {
																				p.playSound(p.getLocation(), Sound.valueOf("LAVA_POP"), 6.0F, 1.0F);
																			} catch (Exception | Error var54) {
																			}
																		}
																	} catch (Exception | Error var55) {
																		p.playSound(p.getLocation(), Sound.valueOf("LAVA_POP"), 1.0F, 1.0F);
																	}
																}
															}

															boolean negateHeadshot = false;
															boolean bulletProtection = false;
															double damageMAX = damage * (bulletProtection ? 0.1D : 1.0D) * (headshot && !negateHeadshot ? (QAMain.HeadshotOneHit ? 50.0D * g.getHeadshotMultiplier() : g.getHeadshotMultiplier()) : 1.0D);
															QAWeaponDamageEntityEvent shootevent = new QAWeaponDamageEntityEvent(p, g, hitTarget, headshot, damage, bulletProtection);
															Bukkit.getPluginManager().callEvent(shootevent);
															String var10000;
															if (!shootevent.isCancelled()) {
																if (headshot) {
																	QAHeadShotEvent headshotevent = new QAHeadShotEvent(hitTarget, p, g);
																	Bukkit.getPluginManager().callEvent(headshotevent);
																	headshot = !headshotevent.isCancelled();
																}

																Player entityTarget;
																if (hitTarget instanceof Player) {
																	entityTarget = (Player)hitTarget;
																	if (!QAMain.enableArmorIgnore) {
																		try {
																			double defensePoints = 0.0D;
																			double toughness = 0.0D;
																			ItemStack[] var46 = new ItemStack[]{entityTarget.getInventory().getHelmet(), entityTarget.getInventory().getChestplate(), entityTarget.getInventory().getLeggings(), entityTarget.getInventory().getBoots()};
																			int var77 = var46.length;
																			int var48 = 0;

																			while(true) {
																				if (var48 >= var77) {
																					QAMain.DEBUG("Applied armor protection: " + defensePoints);
																					damageMAX /= 1.0D - Math.min(20.0D, Math.max(defensePoints / 5.0D, defensePoints - damageMAX / (toughness / 4.0D + 2.0D))) / 25.0D;
																					break;
																				}

																				ItemStack is = var46[var48];
																				if (is != null) {
																					Collection<AttributeModifier> attributes = is.getItemMeta().getAttributeModifiers(Attribute.GENERIC_ARMOR);
																					Collection<AttributeModifier> toughnessAttributes = is.getItemMeta().getAttributeModifiers(Attribute.GENERIC_ARMOR_TOUGHNESS);
																					Iterator var52;
																					AttributeModifier a;
																					if (attributes != null && !attributes.isEmpty()) {
																						for(var52 = attributes.iterator(); var52.hasNext(); defensePoints += a.getAmount()) {
																							a = (AttributeModifier)var52.next();
																						}
																					}

																					if (toughnessAttributes != null && !toughnessAttributes.isEmpty()) {
																						for(var52 = toughnessAttributes.iterator(); var52.hasNext(); toughness += a.getAmount()) {
																							a = (AttributeModifier)var52.next();
																						}
																					}
																				}

																				++var48;
																			}
																		} catch (Exception | Error var56) {
																			QAMain.DEBUG("An error has occurred: " + var56.getMessage());
																		}
																	}

																	if (!bulletProtection) {
																		BulletWoundHandler.bulletHit((Player)hitTarget, g.getAmmoType() == null ? 1.0D : g.getAmmoType().getPiercingDamage());
																	}
																}

																QADamageEvent damageEvent;
																if (hitTarget instanceof Player) {
																	entityTarget = (Player)hitTarget;
																	((LivingEntity)hitTarget).setNoDamageTicks(0);
																	var10000 = hitTarget.getName();
																	QAMain.DEBUG("Damaging entity " + var10000 + " ( " + ((LivingEntity)hitTarget).getHealth() + "/" + ((LivingEntity)hitTarget).getMaxHealth() + " :" + damageMAX + " DAM)");
																	damageEvent = new QADamageEvent(p, entityTarget, g, damageMAX);
																	Bukkit.getPluginManager().callEvent(damageEvent);
																	if (!damageEvent.isCancelled()) {
																		entityTarget.damage(damageEvent.getDamage() > 0.0D ? damageEvent.getDamage() : 1.0E-5D);
																	}
																}

																if (hitTarget.getPassenger() instanceof Damageable) {
																	QAMain.DEBUG("Found a passenger (" + hitTarget.getPassenger().getName() + "). Damaging it.");
																	QAWeaponDamageEntityEvent passengerShoot = new QAWeaponDamageEntityEvent(p, g, hitTarget.getPassenger(), false, damage, bulletProtection);
																	Bukkit.getPluginManager().callEvent(passengerShoot);
																	if (!passengerShoot.isCancelled()) {
																		damageEvent = new QADamageEvent(p, hitTarget.getPassenger(), g, damageMAX);
																		Bukkit.getPluginManager().callEvent(damageEvent);
																		if (!damageEvent.isCancelled()) {
																			((Damageable)hitTarget.getPassenger()).damage(damageEvent.getDamage());
																		}
																	}
																}
															} else if (hitTarget instanceof LivingEntity) {
																var10000 = hitTarget.getName();
																QAMain.DEBUG("Damaging entity CANCELED " + var10000 + " ( " + ((LivingEntity)hitTarget).getHealth() + "/" + ((LivingEntity)hitTarget).getMaxHealth() + " :" + damageMAX + " DAM)");
															} else {
																QAMain.DEBUG("Damaging entity CANCELED " + hitTarget.getType() + ".");
															}
														}
													} else {
														QAMain.DEBUG("No entities hit.");
													}

													time3 = System.currentTimeMillis();
													if (QAMain.enableBulletTrails) {
														List<Player> nonheard = start.getWorld().getPlayers();
														nonheard.remove(p);
														if (g.useMuzzleSmoke()) {
															ParticleHandlers.spawnMuzzleSmoke(p, start.clone().add(step.clone().multiply(7)));
														}

														double distSqrt = maxEntityDistance;
														Vector stepSmoke = normalizedDirection.clone().multiply(QAMain.smokeSpacing);
														entityDistance = 0.0D;

														label291:
														while(true) {
															if (!(entityDistance < distSqrt)) {
																Iterator var64 = blocksThatWillBreak.iterator();

																while(true) {
																	if (!var64.hasNext()) {
																		break label291;
																	}

																	Block l = (Block)var64.next();
																	int var57 = l.getX();
																	QAMain.DEBUG("Breaking " + var57 + " " + l.getY() + " " + l.getZ() + ": " + l.getType());
																	QAWeaponDamageBlockEvent event = new QAWeaponDamageBlockEvent(p, g, l);
																	Bukkit.getPluginManager().callEvent(event);
																	if (!event.isCancelled()) {
																		if (QAMain.regenDestructableBlocksAfter > 0) {
																			l.setType(Material.AIR);
																		} else {
																			l.breakNaturally();
																		}

																		CoreProtectHook.logBreak(l, p);
																	}
																}
															}

															label288: {
																start.add(stepSmoke);
																if (start.getBlock().getType() != Material.AIR) {
																	boolean solid = isSolid(start.getBlock(), start);
																	QAWeaponDamageBlockEvent blockevent = new QAWeaponDamageBlockEvent(p, g, start.getBlock());
																	Bukkit.getPluginManager().callEvent(blockevent);
																	if (!blockevent.isCancelled()) {
																		if ((solid || isBreakable(start.getBlock(), start)) && !blocksThatWillPLAYBreak.contains(new Location(start.getWorld(), (double)start.getBlockX(), (double)start.getBlockY(), (double)start.getBlockZ()))) {
																			blocksThatWillPLAYBreak.add(new Location(start.getWorld(), (double)start.getBlockX(), (double)start.getBlockY(), (double)start.getBlockZ()));
																		}

																		Block block = start.getBlock();
																		Material type = block.getType();
																		if ((QAMain.destructableBlocks.contains(type) || g.getBreakableMaterials().contains(type)) && ProtectionHandler.canBreak(start)) {
																			blocksThatWillBreak.add(block);
																		}
																	}

																	if (!solid) {
																		break label288;
																	}
																}

																ParticleHandlers.spawnGunParticles(g, start);
															}

															entityDistance += QAMain.smokeSpacing;
														}
													}

													time4point5 = System.currentTimeMillis();
													time4 = System.currentTimeMillis();
													continue label294;
												}

												e = (Entity)var35.next();
											} while(!(e instanceof Damageable));
										} while(QAMain.avoidTypes.contains(e.getType()));
									} while(e == p);
								} while(e == p.getVehicle());
							} while(e == p.getPassenger());

							entityDistanceSquared = e.getLocation().distanceSquared(start);
						} while(entityDistanceSquared >= maxEntityDistanceSquared);

						entityDistance = e.getLocation().distance(start);
						box = BoundingBoxManager.getBoundingBox(e);
						bulletLocationTest = start.clone();
						if (!(e instanceof Player)) {
							break;
						}

						player = (Player)e;
					} while(player.getGameMode() == GameMode.SPECTATOR);

					double checkDistanceMax = box.maximumCheckingDistance(e);
					double startDistance = Math.max(entityDistance - checkDistanceMax, 0.0D);
					if (startDistance > 0.0D) {
						bulletLocationTest.add(normalizedDirection.clone().multiply(startDistance));
					}

					for(double testDistance = startDistance; testDistance < entityDistance + checkDistanceMax; testDistance += step.length()) {
						bulletLocationTest.add(step);
						if (box.intersects(p, bulletLocationTest, e)) {
							bulletHitLoc = bulletLocationTest;
							maxEntityDistance = entityDistance;
							maxEntityDistanceSquared = entityDistanceSquared;
							hitTarget = e;
							hitBox = box;
							break;
						}
					}
				}
			}
		}

		if (timingsReport) {
			System.out.println("time1 = " + time1);
			System.out.println("time2 = " + time2);
			System.out.println("time3 = " + time3);
			System.out.println("time3.5 = " + time4point5);
			System.out.println("time4 = " + time4);
		}

	}

	public static void basicShoot(boolean offhand, Gun g, Player player, double acc) {
		basicShoot(offhand, g, player, acc, 1, false);
	}

	public static void basicShoot(boolean offhand, Gun g, Player player, double acc, boolean holdingRMB) {
		basicShoot(offhand, g, player, acc, 1, holdingRMB);
	}

	public static void basicShoot(boolean offhand, Gun g, Player player, double acc, int times) {
		basicShoot(offhand, g, player, acc, times, false);
	}

	public static void basicShoot(final boolean offhand, final Gun g, final Player player, double acc, int times, final boolean holdingRMB) {
		QAMain.DEBUG("About to shoot!");
		if (g.getChargingHandler() == null || !g.getChargingHandler().isCharging(player) && (g.getReloadingingHandler() == null || !g.getReloadingingHandler().isReloading(player))) {
			if (isDelay(g, player)) {
				QAMain.DEBUG("Shooting canceled due to last shot being too soon.");
			} else {
				g.getLastShotForGun().put(player.getUniqueId(), System.currentTimeMillis());
				if (rapidfireshooters.containsKey(player.getUniqueId())) {
					QAMain.DEBUG("Shooting canceled due to rapid fire being enabled.");
				} else {
					ItemStack firstGunInstance = IronsightsHandler.getItemAiming(player);
					boolean regularshoot = true;
					if (g.getChargingHandler() != null) {
						String var10000 = g.getName();
						QAMain.DEBUG("Charging shoot debug: " + var10000 + " = " + g.getChargingHandler() == null ? "null" : g.getChargingHandler().getName());
						regularshoot = g.getChargingHandler().shoot(g, player, firstGunInstance);
					}

					if (regularshoot) {
						QAMain.DEBUG("Handling shoot and gun damage.");
						shootHandler(g, player);
						playShoot(g, player);
						if (QAMain.enableRecoil) {
							addRecoil(player, g);
						}
					}

					if (g.isAutomatic()) {
						rapidfireshooters.put(player.getUniqueId(), (new BukkitRunnable() {
							final int slotUsed = player.getInventory().getHeldItemSlot();

							public void run() {
								if ((g.getChargingHandler() == null || !g.getChargingHandler().isCharging(player)) && !GunRefillerRunnable.isReloading(player)) {
									if (!holdingRMB && (!g.hasIronSights() || !IronsightsHandler.isAiming(player)) && player.isSneaking() == QAMain.SwapSneakToSingleFire) {
										this.cancel();
										QAMain.DEBUG("Stopping Automatic Firing");
										GunUtil.rapidfireshooters.remove(player.getUniqueId());
									} else {
										ItemStack temp = IronsightsHandler.getItemAiming(player);
										if (QAMain.enableDurability && Gun.getDamage(temp) <= 0) {
											player.playSound(player.getLocation(), WeaponSounds.METALHIT.getSoundName(), 1.0F, 1.0F);
											GunUtil.rapidfireshooters.remove(player.getUniqueId());
											QAMain.DEBUG("Canceld due to weapon durability = " + Gun.getDamage(temp));
											this.cancel();
										} else {
											int amount = Gun.getAmount(player);
											if (holdingRMB && !QAMain.SWAP_TO_LMB_SHOOT && System.currentTimeMillis() - g.getLastTimeRMB(player) > 310L) {
												GunUtil.rapidfireshooters.remove(player.getUniqueId());
												this.cancel();
											} else if ((!QAMain.SWAP_TO_LMB_SHOOT || player.isSneaking() != QAMain.SwapSneakToSingleFire) && this.slotUsed == player.getInventory().getHeldItemSlot() && amount > 0) {
												boolean regularshoot = true;
												if (g.getChargingHandler() != null && !g.getChargingHandler().isCharging(player) && (g.getReloadingingHandler() == null || !g.getReloadingingHandler().isReloading(player))) {
													regularshoot = g.getChargingHandler().shoot(g, player, temp);
													String var10000 = g.getName();
													QAMain.DEBUG("Charging (rapidfire) shoot debug: " + var10000 + " = " + g.getChargingHandler() == null ? "null" : g.getChargingHandler().getName());
												}

												if (regularshoot) {
													GunUtil.shootHandler(g, player);
													GunUtil.playShoot(g, player);
													if (QAMain.enableRecoil) {
														GunUtil.addRecoil(player, g);
													}
												}

												--amount;
												if (amount < 0) {
													amount = 0;
												}

												int slot;
												if (offhand) {
													slot = -1;
												} else {
													slot = player.getInventory().getHeldItemSlot();
												}

												Gun.updateAmmo(g, player.getItemInHand(), amount);												if (QAMain.showAmmoInXPBar) {
													GunUtil.updateXPBar(player, g, amount);
												}

												if (slot == -1) {
													try {
														if (QualityArmory.isIronSights(player.getItemInHand())) {
															player.getInventory().setItemInOffHand(temp);
															QAMain.DEBUG("Sett Offhand because ironsights in main hand");
														} else {
															player.getInventory().setItemInHand(temp);
															QAMain.DEBUG("Set mainhand because ironsights not in main hand");
														}
													} catch (Error var8) {
													}
												} else {
													ItemStack tempCheck = player.getInventory().getItem(slot);
													if (QualityArmory.isIronSights(tempCheck)) {
														CustomBaseObject tempBase = QualityArmory.getCustomItem(Update19OffhandChecker.getItemStackOFfhand(player));
														if (tempBase != null && tempBase == g) {
															Update19OffhandChecker.setOffhand(player, temp);
														}
													} else {
														player.getInventory().setItem(slot, temp);
													}
												}

												QualityArmory.sendHotbarGunAmmoCount(player, g, temp, false);
											} else {
												GunUtil.rapidfireshooters.remove(player.getUniqueId());
												this.cancel();
											}
										}
									}
								} else {
									QAMain.DEBUG("Cancelling rapid fire shoot due to charging or reloading.");
									GunUtil.rapidfireshooters.remove(player.getUniqueId());
									this.cancel();
								}
							}
						}).runTaskTimer(QAMain.getInstance(), (long)(10 / g.getFireRate()), (long)(10 / g.getFireRate())));
					}

					int amount = Gun.getAmount(player) - 1;
					if (amount < 0) {
						amount = 0;
					}

					int slot;
					if (offhand) {
						slot = -1;
					} else {
						slot = player.getInventory().getHeldItemSlot();
					}

					QAMain.DEBUG("Ammo amount: " + amount);
					QAMain.DEBUG("Slot: " + slot);
					if (slot == -1) {
						try {
							if (QualityArmory.isIronSights(player.getItemInHand())) {
								player.getInventory().setItemInOffHand(firstGunInstance);
								QAMain.DEBUG("Sett Offhand because ironsights in main hand");
							} else {
								player.getInventory().setItemInHand(firstGunInstance);
								QAMain.DEBUG("Set mainhand because ironsights not in main hand");
							}
						} catch (Error var12) {
						}
					} else {
						player.getInventory().setItem(slot, firstGunInstance);
					}

					updateAmmo(g, player, amount);
					QAMain.DEBUG("New ammo: " + Gun.getAmount(player));
				}
			}
		}
	}

	public static void updateXPBar(Player player, Gun g, int amount) {
		player.setLevel(amount);
	}

	public static void playShoot(final Gun g, final Player player) {
		g.damageDurability(player);
		(new BukkitRunnable() {
			public void run() {
				try {
					String soundname = null;
					if (g.getWeaponSounds().size() > 1) {
						soundname = (String)g.getWeaponSounds().get(ThreadLocalRandom.current().nextInt(g.getWeaponSounds().size()));
					} else {
						soundname = g.getWeaponSound();
					}

					player.getWorld().playSound(player.getLocation(), soundname, (float)g.getVolume(), 1.0F);
					if (!QAMain.isVersionHigherThan(1, 9)) {
						try {
							player.getWorld().playSound(player.getLocation(), Sound.valueOf("CLICK"), 5.0F, 1.0F);
							player.getWorld().playSound(player.getLocation(), Sound.valueOf("WITHER_SHOOT"), 8.0F, 2.0F);
							player.getWorld().playSound(player.getLocation(), Sound.valueOf("EXPLODE"), 8.0F, 2.0F);
						} catch (Exception | Error var3) {
							player.getWorld().playSound(player.getLocation(), Sound.valueOf("BLOCK_LEVER_CLICK"), 5.0F, 1.0F);
						}
					}
				} catch (Error var4) {
					player.getWorld().playSound(player.getLocation(), Sound.valueOf("CLICK"), 5.0F, 1.0F);
					player.getWorld().playSound(player.getLocation(), Sound.valueOf("WITHER_SHOOT"), 8.0F, 2.0F);
					player.getWorld().playSound(player.getLocation(), Sound.valueOf("EXPLODE"), 8.0F, 2.0F);
				}

			}
		}).runTaskLater(QAMain.getInstance(), 1L);
	}

	public static boolean hasAmmo(Player player, Gun g) {
		return QualityArmory.getAmmoInInventory(player, g.getAmmoType()) > 0;
	}

	public static void basicReload(Gun g, Player player, boolean doNotRemoveAmmo) {
		basicReload(g, player, doNotRemoveAmmo, 1.5D);
	}

	public static void basicReload(Gun g, Player player, boolean doNotRemoveAmmo, double seconds) {
		ItemStack temp = player.getInventory().getItemInHand();
		ItemMeta im = temp.getItemMeta();
		if (Gun.getAmount(player) != g.getMaxBullets()) {
			if (im != null && im.hasDisplayName()) {
				if (im.getLore() == null || !im.getDisplayName().contains(QAMain.S_RELOADING_MESSAGE)) {
					try {
						player.getWorld().playSound(player.getLocation(), WeaponSounds.RELOAD_MAG_OUT.getSoundName(), 1.0F, 1.0F);
					} catch (Error var13) {
						try {
							player.getWorld().playSound(player.getLocation(), Sound.valueOf("CLICK"), 5.0F, 1.0F);
						} catch (Exception | Error var12) {
							player.getWorld().playSound(player.getLocation(), Sound.valueOf("BLOCK_LEVER_CLICK"), 5.0F, 1.0F);
						}
					}

					int slot = player.getInventory().getHeldItemSlot();
					Ammo ammo = g.getAmmoType();
					int initialAmount = Gun.getAmount(player);
					int reloadAmount = doNotRemoveAmmo ? g.getMaxBullets() : Math.min(g.getMaxBullets(), initialAmount + QualityArmory.getAmmoInInventory(player, ammo));
					int subtractAmount = reloadAmount - initialAmount;
					if (g.getReloadingingHandler() != null) {
						seconds = g.getReloadingingHandler().reload(player, g, subtractAmount);
					}

					QAMain.toggleNightvision(player, g, false);
					String var10001 = g.getDisplayName();
					im.setDisplayName(var10001 + QAMain.S_RELOADING_MESSAGE);
					temp.setItemMeta(im);
					player.getInventory().setItem(slot, temp);
					if (QAMain.showAmmoInXPBar) {
						updateXPBar(player, g, 0);
					}

					new GunRefillerRunnable(player, temp, g, slot, initialAmount, reloadAmount, seconds, ammo, subtractAmount, !doNotRemoveAmmo);
				}

			}
		}
	}

	public static void addRecoil(final Player player, final Gun g) {
		if (g.getRecoil() != 0.0D) {
			if (g.getFireRate() >= 4) {
				if (highRecoilCounter.containsKey(player.getUniqueId())) {
					highRecoilCounter.put(player.getUniqueId(), (Double)highRecoilCounter.get(player.getUniqueId()) + g.getRecoil());
				} else {
					highRecoilCounter.put(player.getUniqueId(), g.getRecoil());
					(new BukkitRunnable() {
						public void run() {
							if (QAMain.hasProtocolLib && QAMain.isVersionHigherThan(1, 13) && !QAMain.hasViaVersion) {
								GunUtil.addRecoilWithProtocolLib(player, g, true);
							} else {
								GunUtil.addRecoilWithTeleport(player, g, true);
							}

						}
					}).runTaskLater(QAMain.getInstance(), 3L);
				}
			} else if (QAMain.hasProtocolLib && QAMain.isVersionHigherThan(1, 13)) {
				addRecoilWithProtocolLib(player, g, false);
			} else {
				addRecoilWithTeleport(player, g, false);
			}

		}
	}

	private static void addRecoilWithProtocolLib(Player player, Gun g, boolean useHighRecoil) {
		Vector newDir = player.getLocation().getDirection();
		newDir.normalize().setY(newDir.getY() + (useHighRecoil ? (Double)highRecoilCounter.get(player.getUniqueId()) : g.getRecoil()) / 30.0D).multiply(20);
		if (useHighRecoil) {
			highRecoilCounter.remove(player.getUniqueId());
		}

		ProtocolLibHandler.sendYawChange(player, newDir);
	}

	private static void addRecoilWithTeleport(Player player, Gun g, boolean useHighRecoil) {
		Location tempCur = (Location)QAMain.recoilHelperMovedLocation.get(player.getUniqueId());
		Location current;
		if (tempCur == null) {
			current = player.getLocation();
		} else {
			current = tempCur;
		}

		Vector movementOffset = player.getVelocity().multiply(0.2D);
		if (movementOffset.getY() > -0.1D && movementOffset.getY() < 0.0D) {
			movementOffset.setY(0);
		}

		current.add(movementOffset);
		current.setPitch((float)((double)current.getPitch() - (useHighRecoil ? (Double)highRecoilCounter.get(player.getUniqueId()) : g.getRecoil())));
		if (useHighRecoil) {
			highRecoilCounter.remove(player.getUniqueId());
		}

		Vector temp = player.getVelocity();
		player.teleport(current);
		player.setVelocity(temp);
	}

	public static boolean isBreakable(Block b, Location l) {
		return b.getType().name().contains("GLASS");
	}

	public static boolean isSolid(Block b, Location l) {
		return BlockCollisionUtil.isSolid(b, l);
	}

	@NotNull
	public static String locationToString(@NotNull Location l) {
		double var10000 = l.getX();
		return "X: " + var10000 + " Y: " + l.getY() + " Z: " + l.getZ();
	}

	public static boolean isDelay(Gun g, Player player) {
		int showdelay = (int)(g.getDelayBetweenShotsInSeconds() * 1000.0D);
		return g.getLastShotForGun().containsKey(player.getUniqueId()) && System.currentTimeMillis() - (Long)g.getLastShotForGun().get(player.getUniqueId()) < (long)showdelay;
	}
}