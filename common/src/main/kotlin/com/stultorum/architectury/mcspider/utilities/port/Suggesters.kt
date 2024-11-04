package com.stultorum.architectury.mcspider.utilities.port

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.stultorum.architectury.mcspider.McSpider
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture
import net.minecraft.server.command.ServerCommandSource as SCS

class StringSuggester(private val strings: Array<out String>): SuggestionProvider<SCS> {
    override fun getSuggestions(context: CommandContext<SCS>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        strings.filter { CommandSource.shouldSuggest(remaining, it) }.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
    constructor(vararg strings: String): this(strings)
}

class SettingNameSuggester: SuggestionProvider<SCS> {
    override fun getSuggestions(context: CommandContext<SCS>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val remaining = builder.remaining
        val category = StringArgumentType.getString(context, "category")
        if (!McSpider.options.containsKey(category)) return Suggestions.empty()
        val potential = McSpider.options[category]!!.javaClass.declaredFields.map { it.name }
        potential.filter { CommandSource.shouldSuggest(remaining, it) }.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}