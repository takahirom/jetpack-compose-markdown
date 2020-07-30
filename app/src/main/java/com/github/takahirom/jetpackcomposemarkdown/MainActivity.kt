package com.github.takahirom.jetpackcomposemarkdown

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.ui.core.ContextAmbient
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.tooling.preview.Preview
import com.github.takahirom.jetpackcomposemarkdown.ui.JetpackcomposemarkdownTheme
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackcomposemarkdownTheme {
                val context = ContextAmbient.current
                val markdownText = context.assets.open("commonMarkSpec.md").reader().readText()
                val markdown = MarkdownParser(CommonMarkFlavourDescriptor())
                    .parse(MarkdownElementTypes.MARKDOWN_FILE, markdownText, true)
                VerticalScroller {
                    Box {
                        Markdown(markdownText, markdown)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    JetpackcomposemarkdownTheme {
        Greeting("Android")
    }
}