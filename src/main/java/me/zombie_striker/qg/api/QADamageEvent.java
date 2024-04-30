package me.zombie_striker.qg.api;

import me.zombie_striker.qg.guns.Gun;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QADamageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Entity damager;
    private final Entity damaged;
    private final Gun gun;
    private double damage;

    private boolean cancelled;

    public QADamageEvent(Entity damager, Entity damaged, Gun gun, double damage) {
        this.damager = damager;
        this.damaged = damaged;
        this.gun = gun;
        this.damage = damage;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public double getDamage() {
        return damage;
    }

    public Gun getGun() {
        return gun;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public Entity getDamaged() {
        return damaged;
    }

    public Entity getDamager() {
        return damager;
    }

}
