package com.github.takahirom.jetpackcomposemarkdown

import android.util.Log
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.ContentScale
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.ColumnScope.gravity
import androidx.ui.layout.fillMaxSize
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.text.*
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.TextOverflow
import androidx.ui.tooling.preview.Preview
import androidx.ui.tooling.preview.PreviewParameter
import androidx.ui.tooling.preview.PreviewParameterProvider
import androidx.ui.unit.sp
import com.github.takahirom.jetpackcomposemarkdown.ui.JetpackcomposemarkdownTheme
import dev.chrisbanes.accompanist.coil.CoilImageWithCrossfade
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser


@Composable
fun Markdown(text: String, markdown: ASTNode) {
    val builder = AnnotatedString.Builder()
    val images = mutableListOf<Pair<Int, String>>()
    val links = mutableListOf<Pair<IntRange, String>>()
    val treeStringBuilder = StringBuilder()
    fun showTree(node: ASTNode, depth: Int) {
        treeStringBuilder.appendLine(
            " ".repeat(depth) + node.type + " " + node.getTextInNode(text).toString()
                .take(10) + "..."
        )
        node.children.forEach {
            showTree(it, depth = depth + 1)
        }
    }
    showTree(markdown, 0)
//    println(treeStringBuilder.toString().take(3000))
    builder.appendMarkdown(
        markdownText = text,
        node = markdown,
        onInlineContents = { position, link ->
            images.add(position to link)
        },
        onLinkContents = { positionRange, url ->
            links.add(positionRange to url)
        }
    )
    val inlineContents = images.map { (_, link) ->
        link to InlineTextContent(
            Placeholder(
                150.sp,
                150.sp,
                PlaceholderVerticalAlign.TextCenter
            )
        ) {
            CoilImageWithCrossfade(
                data = link,
                modifier = Modifier.fillMaxSize().gravity(Alignment.CenterHorizontally),
                contentScale = ContentScale.Inside,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize().gravity(Alignment.CenterHorizontally)
                    )
                },
                onRequestCompleted = { result ->
                    Log.d("Markdown", "image request result $result")
                }
            )
        }

    }.toMap()
    ClickableText(
        text = builder.toAnnotatedString(),
        inlineContent = inlineContents
    ) { position ->
        Log.d("Markdown", "link clicked:" + links.firstOrNull { it.first.contains(position) })
    }
}

@Composable
fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onClick: (Int) -> Unit,
) {
    val layoutResult = state<TextLayoutResult?> { null }
    val pressIndicator = Modifier.tapGestureFilter { pos ->
        layoutResult.value?.let { layoutResult ->
            onClick(layoutResult.getOffsetForPosition(pos))
        }
    }

    Text(
        text = text,
        modifier = modifier + pressIndicator,
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        inlineContent = inlineContent,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        }
    )
}

fun AnnotatedString.Builder.appendMarkdown(
    markdownText: String,
    node: ASTNode,
    depth: Int = 0,
    onInlineContents: (position: Int, link: String) -> Unit,
    onLinkContents: (positionRange: IntRange, url: String) -> Unit,
): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH -> {
            node.children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                    depth = depth + 1,
                    onInlineContents = onInlineContents,
                    onLinkContents = onLinkContents
                )
            }
        }
        MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.ATX_1 -> {
            withStyle(SpanStyle(fontSize = 24.sp)) {
                node.children.subList(1, node.children.size).forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        onInlineContents = onInlineContents,
                        onLinkContents = onLinkContents
                    )
                }
            }
        }
        MarkdownElementTypes.SETEXT_2, MarkdownElementTypes.ATX_2 -> {
            withStyle(SpanStyle(fontSize = 20.sp)) {
                node.children.subList(1, node.children.size).forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        onInlineContents = onInlineContents,
                        onLinkContents = onLinkContents
                    )
                }
            }
        }
        MarkdownElementTypes.CODE_SPAN -> {
            withStyle(SpanStyle(background = Color.LightGray)) {
                node.children.subList(1, node.children.size - 1)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents
                        )
                    }
            }
        }
        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children
                    .drop(2)
                    .dropLast(2)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents
                        )
                    }
            }
        }
        MarkdownElementTypes.EMPH -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children
                    .drop(1)
                    .dropLast(1)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents
                        )
                    }
            }
        }
        MarkdownElementTypes.CODE_FENCE -> {
            withStyle(SpanStyle(background = Color.Gray)) {
                node.children
                    .drop(1)
                    .dropLast(1)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents
                        )
                    }
            }
        }
        MarkdownElementTypes.IMAGE -> {
            val linkNode = node.children[node.children.size - 1]
            if (linkNode.children.size > 2) {
                val link =
                    linkNode.children[linkNode.children.size - 2].getTextInNode(markdownText)
                onInlineContents(node.startOffset, link.toString())
                appendInlineContent(link.toString(), link.toString())
            }
        }
        MarkdownElementTypes.INLINE_LINK -> {
            val linkDestination =
                node.children.findLast { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?: return this
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!
                .children[1]
            if (linkDestination.children.size > 2) {
                val link =
                    linkDestination.getTextInNode(markdownText).toString()
                val start = this.length
                withStyle(SpanStyle(color = Color.Blue)) {
                    appendMarkdown(
                        markdownText = markdownText,
                        node = linkText,
                        depth = depth + 1,
                        onInlineContents = onInlineContents,
                        onLinkContents = onLinkContents
                    )
                    val end = this.length
                    onLinkContents(start..end, link)
                }
            }
        }
        else -> {
            append(
                text = node.getTextInNode(markdownText).toString()
            )
        }
    }
    return this
}

class MarkdownPreviewProvider : PreviewParameterProvider<Pair<String, ASTNode>> {
    val texts = listOf(
        """
                    # test
                    ## aaaa
                    abc**abc**
                """
    )

    override val values: Sequence<Pair<String, ASTNode>>
        get() {
            return texts.asSequence().map { it.trimIndent() }
                .map { markdownText ->
                    markdownText to MarkdownParser(CommonMarkFlavourDescriptor())
                        .parse(MarkdownElementTypes.MARKDOWN_FILE, markdownText, true)
                }
        }
    override val count: Int
        get() = texts.size
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview(@PreviewParameter(provider = MarkdownPreviewProvider::class) pair: Pair<String, ASTNode>) {
    JetpackcomposemarkdownTheme {
        Markdown(
            pair.first,
            pair.second
        )
    }
}