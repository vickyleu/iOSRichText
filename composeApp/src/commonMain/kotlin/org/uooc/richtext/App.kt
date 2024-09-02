package org.uooc.richtext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val state = remember {
            mutableStateOf(
                RichTextCompose(
                   """
                <p>&nbsp;爸爸的爸爸叫什么？
                使用textView实现的html table标签和latex公式
                是是是试试看多行高度计算是否正常
                不信你看
                <table>
                                <tr>
                                    <th>Header 1</th>
                                    <th>Header 2</th>
                                </tr>
                                <tr>
                                    <td>Cell 1</td>
                                    <td>Cell 2</td>
                                </tr>
                                <tr>
                                    <td>Cell 3</td>
                                    <td>Cell 4</td>
                                </tr>
                </table>
                <span class="math-tex">\\(\\frac {-b\\pm \\sqrt {{b}^{2}-4ac}} {2a}>\\frac {dy} {dx}\\)</span><p>你们好啊</p></p>
                """.trim()
                )
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            RichTextPlatformView(
                state,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    .requiredHeightIn(min = 10.dp, max = max(state.value.height.value, 20.dp)),
                style = TextStyle(
                    color = MaterialTheme.colors.onSurface,
                    fontSize = 16.sp
                )
            )
        }

    }
}