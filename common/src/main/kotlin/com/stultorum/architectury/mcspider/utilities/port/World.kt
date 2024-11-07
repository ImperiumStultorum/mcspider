package com.stultorum.architectury.mcspider.utilities.port

import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun World.playSound(location: Vec3d, event: SoundEvent, category: SoundCategory, volume: Float, pitch: Float, useDistance: Boolean) {
    playSound(location.x, location.y, location.z, event, category, volume, pitch, useDistance)
}