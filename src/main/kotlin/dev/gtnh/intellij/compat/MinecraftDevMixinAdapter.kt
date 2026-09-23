package dev.gtnh.intellij.compat

/**
 * Compatibility boundary for optional public Minecraft Development Mixin APIs.
 *
 * The installed dependency currently exposes no stable public service needed by patch discovery,
 * so the implementation deliberately uses IntelliJ Java PSI. Future integrations belong here,
 * never as platform-version checks spread through providers.
 */
object MinecraftDevMixinAdapter
