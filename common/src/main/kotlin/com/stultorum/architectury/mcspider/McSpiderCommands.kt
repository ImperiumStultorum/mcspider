package com.stultorum.architectury.mcspider

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.stultorum.architectury.mcspider.spider.BodyPlan
import com.stultorum.architectury.mcspider.utilities.port.SettingNameSuggester
import com.stultorum.architectury.mcspider.utilities.port.StringSuggester
import net.minecraft.text.Text
import net.minecraft.server.command.CommandManager.literal as commandLiteral
import net.minecraft.server.command.ServerCommandSource as SCS

// it should be noted I elected not to do /spider:items because /give should work fine now.
internal val spiderCommand = commandLiteral("spider").requires { it.hasPermissionLevel(2) }
    .then(commandLiteral("toggle")
        .executes { ctx ->
            val enabled = McSpider.instance.toggle()
            ctx.source.sendFeedback({ Text.of("Spider: ${if (enabled) "enabled" else "disabled"}.") }, true)
            return@executes 1
        }
    ).then(commandLiteral("fall")
        .then(argument<SCS, Double>("height", DoubleArgumentType.doubleArg(0.0))
            .executes { ctx ->
                val height = DoubleArgumentType.getDouble(ctx, "height")
                val spider = McSpiderState.spider ?: return@executes 0
                spider.teleport(spider.location.add(y = height))
                ctx.source.sendFeedback({ Text.of("Spider: Teleported up $height") }, true)
                return@executes 1
            }
        )
    ).then(commandLiteral("body")
        .then(argument<SCS, String>("type", StringArgumentType.word())
            .suggests(StringSuggester(McSpider.bodyPlans.keys.toTypedArray()))
            .executes { ctx ->
                val type = StringArgumentType.getString(ctx, "type")
                if (!McSpider.bodyPlans.containsKey(type)) {
                    ctx.source.sendError(Text.of("Spider: Unknown bodyplan $type"))
                    return@executes 0
                }
                setBodyPlan(McSpider.bodyPlans[type]!!)
                ctx.source.sendFeedback({ Text.of("Spider: Body plan updated") }, true)
                return@executes 1
            }
            .then(argument<SCS, Double>("scale", DoubleArgumentType.doubleArg(0.0001))
                .executes { ctx ->
                    val type = StringArgumentType.getString(ctx, "type")
                    val scale = DoubleArgumentType.getDouble(ctx, "scale")
                    if (!McSpider.bodyPlans.containsKey(type)) {
                        ctx.source.sendError(Text.of("Spider: Unknown bodyplan $type"))
                        return@executes 0
                    }
                    setBodyPlan(McSpider.bodyPlans[type]!!, scale)
                    ctx.source.sendFeedback({ Text.of("Spider: Body plan updated") }, true)
                    return@executes 1
                }
                .then(argument<SCS, Int>("segments", IntegerArgumentType.integer(1))
                    .executes { ctx ->
                        val type = StringArgumentType.getString(ctx, "type")
                        val scale = DoubleArgumentType.getDouble(ctx, "scale")
                        val segments = IntegerArgumentType.getInteger(ctx, "segments")
                        if (!McSpider.bodyPlans.containsKey(type)) {
                            ctx.source.sendError(Text.of("Spider: Unknown bodyplan $type"))
                            return@executes 0
                        }
                        setBodyPlan(McSpider.bodyPlans[type]!!, scale, segments)
                        ctx.source.sendFeedback({ Text.of("Spider: Body plan updated") }, true)
                        return@executes 1
                    }
                    .then(argument<SCS, Double>("segmentLength", DoubleArgumentType.doubleArg(0.0001))
                        .executes { ctx ->
                            val type = StringArgumentType.getString(ctx, "type")
                            val scale = DoubleArgumentType.getDouble(ctx, "scale")
                            val segments = IntegerArgumentType.getInteger(ctx, "segments")
                            val segmentLength = DoubleArgumentType.getDouble(ctx, "segmentLength")
                            if (!McSpider.bodyPlans.containsKey(type)) {
                                ctx.source.sendError(Text.of("Spider: Unknown bodyplan $type"))
                                return@executes 0
                            }
                            setBodyPlan(McSpider.bodyPlans[type]!!, scale, segments, segmentLength)
                            ctx.source.sendFeedback({ Text.of("Spider: Body plan updated") }, true)
                            return@executes 1
                        }
                    )
                )
            )
        )
    // TODO this is literal assumption
    ).then(commandLiteral("options")
        .then(argument<SCS, String>("category", StringArgumentType.word())
            .suggests(StringSuggester(McSpider.options.keys.toTypedArray()))
            .then(commandLiteral("reset")
                .executes { ctx ->
                    val category = StringArgumentType.getString(ctx, "category")
                    if (!McSpider.options.containsKey(category)) {
                        ctx.source.sendError(Text.of("Spider: Unknown category \"$category\"!"))
                        return@executes 0
                    }
                    McSpider.options[category] = McSpider.defaultOptions[category]!!()
                    return@executes 1
                }
            ).then(argument<SCS, String>("setting", StringArgumentType.word())
                .suggests(SettingNameSuggester())
                .then(commandLiteral("reset")
                    .executes { ctx ->
                        val category = StringArgumentType.getString(ctx, "category")
                        val setting = StringArgumentType.getString(ctx, "setting")
                        if (!McSpider.options.containsKey(category)) {
                            ctx.source.sendError(Text.of("Spider: Unknown category \"$category\"!"))
                            return@executes 0
                        }
                        try {
                            val categoryObj = McSpider.options[category]!!
                            val field = categoryObj.javaClass.getDeclaredField(setting)
                            field.set(categoryObj, field.get(McSpider.defaultOptions[category]!!()))
                            ctx.source.sendFeedback({ Text.of("Spider: Reset \"$category.$setting\"")}, true)
                            return@executes 1
                        } catch (_: NoSuchFieldException) {
                            ctx.source.sendError(Text.of("Spider: Unknown setting \"$category.$setting\""))
                            return@executes 0
                        }
                    }
                ).then(argument<SCS, String>("value", StringArgumentType.word())
                    .executes { ctx ->
                        val category = StringArgumentType.getString(ctx, "category")
                        val setting = StringArgumentType.getString(ctx, "setting")
                        val value = StringArgumentType.getString(ctx, "value")
                        if (!McSpider.options.containsKey(category)) {
                            ctx.source.sendError(Text.of("Spider: Unknown category \"$category\""))
                            return@executes 0
                        }
                        try {
                            val categoryObj = McSpider.options[category]!!
                            val field = categoryObj.javaClass.getDeclaredField(setting)
                            try {
                                val parser = getParser(field.type)
                                field.set(categoryObj, parser(value))
                                ctx.source.sendFeedback({ Text.of("Spider: \"$category.$setting\" set to \"$value\"") }, true)
                                return@executes 1
                            } catch (_: ClassCastException) {
                                ctx.source.sendError(Text.of("Spider: Could not convert \"$value\" to ${field.type.canonicalName}"))
                                return@executes 0
                            }
                        } catch (_: NoSuchFieldException) {
                            ctx.source.sendError(Text.of("Spider: Unknown setting \"$category.$setting\""))
                            return@executes 0
                        }
                    }
                )
            )
        )
    )

private fun setBodyPlan(constr: (Int, Double) -> BodyPlan, scale: Double = 1.0, segments: Int = 3, segmentLength: Double = 1.0) {
    val oldScale = McSpiderState.bodyPlan.storedScale
    McSpiderState.bodyPlan = constr(segments, segmentLength).apply { scale(scale) }

    McSpiderState.gaitWalk.scale(scale / oldScale)
    McSpiderState.gaitGallop.scale(scale / oldScale)

    val spider = McSpiderState.spider
    if (spider != null) McSpiderState.createSpider(spider.location)

    // TODO config
}

private fun getParser(type: Class<*>): (str: String) -> Any {
    return when (type) {
        Int::class.java -> { str -> str.toInt() }
        Float::class.java -> { str -> str.toFloat() }
        Double::class.java -> { str -> str.toDouble() }
        Boolean::class.java -> { str -> str.toBooleanStrict() }
        else -> throw ClassCastException()
    }
}