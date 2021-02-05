package com.github.treehollow.utils

import android.content.Context
import android.graphics.Color
import android.text.Spanned
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.MarkwonTheme
import org.commonmark.node.Heading
import org.commonmark.node.Node

class MarkdownBuilder(private val context: Context) {
    fun getMarkdown(text: String): Spanned {
        val markwon = Markwon.builder(context)
            .usePlugins(
                listOf(
                    object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder
                                .codeTextColor(Color.BLACK)
                                .linkColor(Color.CYAN)
                        }

                        fun iterateFindNode(
                            node: Node,
                            callback: (node2: Node) -> Unit,
                            depth: Int,
                            maxDepth: Int
                        ) {
                            if (node.firstChild == null || depth >= maxDepth)
                                return
                            var it = node.firstChild
                            while (it != null) {
                                callback(it)
                                iterateFindNode(it, callback, depth + 1, maxDepth)
                                it = it.next
                            }
                        }

                        override fun beforeRender(node: Node) {
                            iterateFindNode(node, {
                                (it as? Heading)?.apply {
                                    if (level <= 3)
                                        level = 3
                                }
                            }, 0, 5)
                        }
                    },
                    SoftBreakAddsNewLinePlugin.create()
//                    LinkifyPlugin.create(),
                )
            )
            .build()
        return markwon.toMarkdown(text)
    }
}