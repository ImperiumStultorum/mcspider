package com.stultorum.architectury.mcspider.platform

import net.minecraft.server.MinecraftServer
import java.io.Closeable

// todo this should go in architectury classic blunders if/when it is created

private val serverTimers = mutableListOf<Pair<Long, (MinecraftServer) -> Long>>()

internal fun MinecraftServer.tickTimers() = tickTimers(this, serverTimers)

private fun <T> tickTimers(obj: T, timers: MutableList<Pair<Long, (T) -> Long>>) {
    val iter = timers.listIterator()
    while (iter.hasNext()) {
        val timer = iter.next()
        if (timer.first <= 1) { // if timer is at one, it would invoke this tick since we haven't decreased yet
            val newDelay = timer.second.invoke(obj)
            if (newDelay <= 0) { // if invoke returns one, it wouldn't remove, since it doesn't know we haven't decreased
                iter.remove()
            } else {
                iter.set(Pair(newDelay, timer.second))
            }
        } else {
            iter.set(Pair(timer.first - 1, timer.second))
        }
    }
}

fun runTaskLater(delay: Long, action: (MinecraftServer) -> Unit): Closeable {
    return addTimer(delay) { action.invoke(it); 0L }
}
fun repeatTask(delay: Long, interval: Long, action: (MinecraftServer) -> Unit): Closeable {
    return addTimer(delay) { action.invoke(it); interval }
}
fun addTimer(delay: Long, action: (MinecraftServer) -> Long): Closeable {
    val timer = Pair(delay, action)
    serverTimers.add(timer)
    return Closeable { serverTimers.remove(timer) }
}
