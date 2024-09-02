@file:OptIn(ExperimentalForeignApi::class)

package org.uooc.richtext

import androidx.compose.ui.unit.Density
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import uooc.DTCoreText.DTHTMLElement
import uooc.DTCoreText.DTTextAttachment
import uooc.DTCoreText.UoocTagHandler

class MathHandler(private val maxWidth: Float, private val density: Density) :
    UoocTagHandler("math-tex") {
    override fun handleStartTag(currentTag: DTHTMLElement) {
        currentTag.textAttachment = MathTextAttachment(currentTag.text() ?:"", currentTag, null).apply {
            this.originalSize = CGSizeMake(maxWidth.toDouble(), maxWidth.toDouble()*0.2f)
        }
    }
    override fun handleEndTag(currentTag: DTHTMLElement) {

    }
}

class MathTextAttachment:DTTextAttachment{
     var latex:String = ""
         private set

    constructor(latex:String ,element: DTHTMLElement?, options: Map<Any?, *>?) : super(element, options){
        this.latex = latex
    }
   private constructor(data: NSData?, ofType: String?) : super(data, ofType)

}