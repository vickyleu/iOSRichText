@file:OptIn(ExperimentalForeignApi::class)

package org.uooc.richtext

import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastMapNotNull
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import uooc.DTCoreText.DTHTMLElement
import uooc.DTCoreText.DTTextAttachment
import uooc.DTCoreText.DTTextHTMLElement
import uooc.DTCoreText.UoocTagHandler

class MathHandler(private val maxWidth: Float, private val density: Density) :
    UoocTagHandler("span") {

    private var findMathTex = false

    @OptIn(ExperimentalForeignApi::class)
    override fun handleStartTag(currentTag: DTHTMLElement) {
        currentTag.attributeForKey("class")?.let {
            if (it == "math-tex") {
                findMathTex = true
            }
        }
    }

    override fun handleEndTag(currentTag: DTHTMLElement) {
        if (findMathTex) {
            val list = (currentTag.childNodes as? List<DTTextHTMLElement>) ?: emptyList()
            val mathTex = (list.fastMapNotNull {
                it.text()?.trim()
            }.firstOrNull {
                it.startsWith("\\(") && it.endsWith("\\)")
            } ?: "").trim().removeSurrounding("\\(", "\\)")

            mathTex.let {
                if (mathTex.isNotEmpty()) {
                    currentTag.textAttachment = MathTextAttachment(it, currentTag, null).apply {
                        this.originalSize =
                            CGSizeMake(maxWidth.toDouble(), maxWidth.toDouble() * 0.2f)
                    }
                    currentTag.paragraphStyle?.apply {
                        this.setParagraphSpacing(maxWidth.toDouble() * 0.2f)
                    }
                }
            }
            findMathTex = false
        }
    }
}

class MathTextAttachment : DTTextAttachment {
    var latex: String = ""
        private set

    constructor(latex: String, element: DTHTMLElement?, options: Map<Any?, *>?) : super(
        element,
        options
    ) {
        this.latex = latex
    }

    @Deprecated(
        "Use constructor instead",
        replaceWith = ReplaceWith("DTTextAttachment(contentData, ofType)"),
        level = DeprecationLevel.ERROR
    )
    override fun initWithData(contentData: NSData?, ofType: String?): DTTextAttachment {
        return this
    }

    private constructor(data: NSData?, ofType: String?) : super(data, ofType)

}