package org.uooc.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RichTextCompose(
    private var text: String,
    val clickImages: (images: List<String>, currentIndex: Int) -> Unit = { _, _ -> },
    val clickUrl: (url: String) -> Unit = {},
) {

    val content: String by lazy {
        HtmlFormater.replaceLatexHtmlContent(text)
    }

    val height: MutableState<Dp> = mutableStateOf(0.dp)
}

@Composable
expect fun RichTextPlatformView(
    state: MutableState<RichTextCompose>,
    style: TextStyle,
    modifier: Modifier
)

