@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.uooc.richtext

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextClearRect
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextSetStrokeColorWithColor
import platform.CoreGraphics.CGContextStrokeRect
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreText.kCTTextAlignmentCenter
import platform.Foundation.NSAttributedString
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.NSLineBreakByWordWrapping
import platform.UIKit.NSMutableParagraphStyle
import platform.UIKit.NSParagraphStyleAttributeName
import platform.UIKit.NSStringDrawingUsesLineFragmentOrigin
import platform.UIKit.NSTextAlignmentLeft
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.boundingRectWithSize
import platform.UIKit.drawInRect
import platform.UIKit.size
import uooc.DTCoreText.DTHTMLElement
import uooc.DTCoreText.DTImageTextAttachment
import uooc.DTCoreText.DTTextAttachmentHTMLPersistenceProtocol
import uooc.DTCoreText.DTTextHTMLElement
import uooc.DTCoreText.TagHandlerProtocol
import uooc.DTCoreText.UoocTagHandler
import kotlin.collections.set
import kotlin.experimental.ExperimentalNativeApi
import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("DTTableTextAttachment")
/**
 * Table
 */
class DTTableTextAttachment(
    private val maxWidth: Float,
    private val element: DTHTMLElement,
    options: Map<Any?, *>?
) : DTImageTextAttachment(
    element,
    options
), DTTextAttachmentHTMLPersistenceProtocol {
    @Deprecated(
        "Use constructor instead",
        replaceWith = ReplaceWith("DTImageTextAttachment(contentData, ofType)"),
        level = DeprecationLevel.ERROR
    )
    override fun initWithData(contentData: NSData?, ofType: String?): DTImageTextAttachment {
        return this
    }

    var sizeKeep = Rect.Zero

    override fun drawInRect(rect: CValue<CGRect>, context: CGContextRef?) {
        // 确保 context 不为 null
        context?.let {
            val ctx = it
            // 清除矩形区域的背景，使其透明
            CGContextClearRect(ctx, rect)
            // 设置描边颜色为绿色
            CGContextSetLineWidth(ctx, 2.0) // 设置边框宽度
            val rectValue = rect.useContents { this }
            // 绘制矩形的边框
            CGContextStrokeRect(ctx, rect)
            this.childNodes.forEachIndexed { index, attachment ->
                //这里是画 DTTableColumnTextAttachment
                val column = attachment as DTTableColumnTextAttachment
                val r = column.sizeKeep
                column.drawInRect(r.let {
                    CGRectMake(
                        x = it.left.toDouble(),
                        y = it.top.toDouble(),
                        it.size.width.toDouble(),
                        it.size.height.toDouble()
                    )
                }, context)
            }
        } ?: throw IllegalArgumentException("context is null")
    }

    override fun stringByEncodingAsHTML(): String {
        return "<table></table>"
    }

    private val childNodes = mutableListOf<DTImageTextAttachment>()
    fun addAll(childNode: List<Any>) {
        this.childNodes.addAll(childNode.filterIsInstance<DTImageTextAttachment>())
        adjustColumnWidths(maxWidth)
        println("最终的表格大小: ${sizeKeep}")
    }

    @OptIn(ExperimentalNativeApi::class)
    fun adjustColumnWidths(maxWidth: Float) {
        val maxWidthModify =maxWidth-20
        val rowWidths = MutableList(childNodes.size) { 0f }
        val rowHeights = MutableList(childNodes.size) { 0.0 }
        // 计算每列的最大宽度和每行的最大高度
        childNodes.forEachIndexed { colIndex, columnAttachment ->
            val column = columnAttachment as DTTableColumnTextAttachment
            column.childNode.forEachIndexed { rowIndex, rowAttachment ->
                var maxRowHeight = 0.0
                rowAttachment.childNode.let { (_, attributedString) ->
                    val textWidth = attributedString.size().useContents { width }.toFloat()
                    // 记录每列的最大宽度
                    rowWidths[rowIndex] = maxOf(rowWidths[rowIndex], textWidth)
                    // 计算行高
                    val textSize = attributedString.boundingRectWithSize(
                        CGSizeMake(textWidth.toDouble(), Double.MAX_VALUE),
                        NSStringDrawingUsesLineFragmentOrigin,
                        context = null
                    ).useContents { Size(size.width.toFloat(),size.height.toFloat()) }
                    maxRowHeight = maxOf(maxRowHeight, textSize.height.toDouble())
                }
                rowHeights[rowIndex] = maxOf(rowHeights[rowIndex], maxRowHeight)
            }
        }
        val totalWidth = rowWidths.sum()
        val resultRowWidths = if (totalWidth > maxWidthModify) {
            val scaleFactor = maxWidthModify / totalWidth
            rowWidths.map { origin->
                (origin * scaleFactor).let {
                    if(origin>0f)it.coerceAtLeast(0.1f) else it
            } }
        } else {
            rowWidths
        }
        // 打印调整后的列宽度
        println("调整后的列宽度: $resultRowWidths")



        // 重新计算每一行的高度
        childNodes.forEachIndexed { colIndex, columnAttachment ->
            val column = columnAttachment as DTTableColumnTextAttachment
            val resizeRowHeight= mutableMapOf<Int,Double>()
            column.childNode.forEachIndexed { rowIndex, rowAttachment ->
                var maxRowHeight = 0.0
                val adjustedWidth = resultRowWidths[rowIndex]
                rowAttachment.childNode.let { (_, attributedString) ->
                    val textSize = attributedString.boundingRectWithSize(
                        CGSizeMake(adjustedWidth.toDouble(), Double.MAX_VALUE),
                        NSStringDrawingUsesLineFragmentOrigin,
                        context = null
                    ).useContents { Size(size.width.toFloat(),size.height.toFloat()) }
                    val textHeight = textSize.height.toDouble()
                    maxRowHeight = maxOf(maxRowHeight, textHeight)
                    rowHeights[colIndex]=maxRowHeight
                }
            }
        }

        // 打印调整后的行高
        rowHeights.forEachIndexed { index, height ->
            println("第${index + 1}行高度: $height")
        }


        println("总宽度: ${resultRowWidths.sum()}, 最大行高: ${rowHeights.maxOrNull() ?: 0.0}")
        println("-".repeat(25))
        // 更新每个子节点的 rect，使用统一的列宽并设置行高
        childNodes.forEachIndexed { colIndex, columnAttachment ->
            val column = columnAttachment as DTTableColumnTextAttachment
            column.childNode.forEachIndexed { rowIndex, rowAttachment ->
                val rowHeight = rowHeights[rowIndex]
                val adjustedWidth = resultRowWidths[rowIndex]

                // 更新单元格 rect
                rowAttachment.childNode.let { (rect, _) ->
                    rect.setNewRect(Rect(
                        left = rect.tokRect().left,
                        top = rect.tokRect().top,
                        right = rect.tokRect().left + adjustedWidth,
                        bottom = rect.tokRect().top + rowHeight.toFloat()
                    ))
                }

                // 打印每个单元格的宽高
                print("| 宽${adjustedWidth.toInt()},高${rowHeight.toInt()} |")

                // 设置行的宽度和高度
                val rowOrigin = rowAttachment.bounds.useContents { this.origin }
                rowAttachment.bounds =
                    CGRectMake(rowOrigin.x, rowOrigin.y, adjustedWidth.toDouble(), rowHeight)
                rowAttachment.originalSize = CGSizeMake(adjustedWidth.toDouble(), rowHeight)
            }
            println() // 打印换行符
        }
        // 设置列的宽度和高度
        childNodes.forEachIndexed { colIndex, columnAttachment ->
            val column = columnAttachment as DTTableColumnTextAttachment
            val origin = column.bounds.useContents { this.origin }
            // 计算列的总高度
            val columnTotalHeight = rowHeights.sum()
            val columnTotalWidth = column.childNode.map {
                it.bounds.useContents { size.width }
            }.sum()

            column.originalSize = CGSizeMake(columnTotalWidth, columnTotalHeight)
            // 更新列的 bounds
            column.bounds = CGRectMake(origin.x, origin.y, columnTotalWidth, columnTotalHeight)

        }
        // 处理每个单元格的 x 和 y 坐标
        var currentY = 0.0 // 用于跟踪当前的 Y 坐标
        childNodes.forEachIndexed { colIndex, columnAttachment ->
            val column = columnAttachment as DTTableColumnTextAttachment
            var currentX = 0.0 // 用于跟踪当前的 X 坐标
            column.childNode.forEachIndexed { rowIndex, rowAttachment ->
                val rowHeight = rowHeights[rowIndex]
                val adjustedWidth = resultRowWidths[rowIndex]
                // 更新单元格的 rect
                rowAttachment.childNode.let { (rect, _) ->
                    rect.setNewRect(Rect(
                        left = currentX.toFloat(),
                        top = currentY.toFloat(),
                        right = currentX.toFloat() + adjustedWidth,
                        bottom = currentY.toFloat() + rowHeight.toFloat()
                    ))
                }
                rowAttachment.sizeKeep = Rect(
                    left =currentX.toFloat(),
                    top =currentY.toFloat(),
                    right =currentX.toFloat()+adjustedWidth,
                    bottom =currentY.toFloat()+rowHeight.toFloat()
                )
                // 打印单元格的 x, y, 宽度和高度
                currentX += adjustedWidth.toDouble() // 更新 x 坐标
                if (rowIndex == column.childNode.size - 1) {
                    currentY += rowHeight // 更新 y 坐标
                }
                if (rowIndex == column.childNode.size - 1) {
                    //最后一个单元格
                    column.originalSize = CGSizeMake(currentX, rowHeight)
                    column.bounds = CGRectMake(
                        (column.bounds.useContents { this.origin.x }),
                        column.bounds.useContents { this.origin.y }+(currentY-rowHeight).coerceAtLeast(0.0),
                        currentX,
                        rowHeight
                    )

                    column.sizeKeep = Rect(
                        left=column.bounds.useContents { this.origin.x }.toFloat(),
                        top =column.bounds.useContents { this.origin.y }.toFloat(),
                        right = column.bounds.useContents { this.origin.x }.toFloat()+currentX.toFloat(),
                        bottom= column.bounds.useContents { this.origin.y }.toFloat()+rowHeight.toFloat()
                    )
                }
            }

            println("column===>宽被清零了?? ${column.bounds.useContents { this.origin.x }}")
        }
        println("-".repeat(25))

        val calculateSize = childNodes.lastOrNull()?.let {
            val column = it as DTTableColumnTextAttachment
            Size(column.sizeKeep.left+column.sizeKeep.width, column.sizeKeep.top+column.sizeKeep.height)
        } ?: Size(0f, 0f)

        this.originalSize =
            CGSizeMake(calculateSize.width.toDouble(), calculateSize.height.toDouble())
        this.bounds =
            CGRectMake(this.bounds.useContents { origin.x }, this.bounds.useContents { origin.y },
                calculateSize.width.toDouble(), calculateSize.height.toDouble()
            )

        this.sizeKeep = Rect(
            left=this.bounds.useContents { origin.x }.toFloat(),
            top = this.bounds.useContents { origin.y }.toFloat(),
            right = this.bounds.useContents { origin.x }.toFloat()+calculateSize.width,
            bottom= this.bounds.useContents { origin.y }.toFloat()+calculateSize.height
        )
    }


    override fun description(): String {
        return "DTTableTextAttachment@${hashCode()}"
    }
}

internal class MutableRect( x: Double,  y: Double,  width: Double,  height: Double) {
    private var rect = Rect(left = x.toFloat(), top = y.toFloat(), right = x.toFloat()+width.toFloat(), bottom = y.toFloat()+height.toFloat())
    fun tokRect(): Rect {
        return rect
    }

    fun setNewRect(rect: Rect) {
        this.rect = rect
    }
    fun toCGRect(): CValue<CGRect> {
        return CGRectMake(rect.left.toDouble(), rect.top.toDouble(), rect.width.toDouble(), rect.height.toDouble())
    }

}

@OptIn(ExperimentalObjCName::class)
@ObjCName("DTTableTextAttachment")
/**
 * TH or TD
 */
class DTTableRowTextAttachment(val flag: String, element: DTHTMLElement, options: Map<Any?, *>?) :
    DTImageTextAttachment(
        element,
        options
    ) {
    @Deprecated(
        "Use constructor instead",
        replaceWith = ReplaceWith("DTImageTextAttachment(contentData, ofType)"),
        level = DeprecationLevel.ERROR
    )
    override fun initWithData(contentData: NSData?, ofType: String?): DTImageTextAttachment {
        return this
    }

    var sizeKeep = Rect.Zero

    internal var childNode = Pair(MutableRect(0.0, 0.0, 0.0, 0.0), NSAttributedString.create(""))

    fun add(attachment: NSAttributedString?) {
        this.childNode = ((attachment ?: return).let { att ->
            val paragraphStyle = NSMutableParagraphStyle()
            paragraphStyle.setLineBreakMode(NSLineBreakByWordWrapping)
            paragraphStyle.setAlignment(NSTextAlignmentLeft)
            // 获取现有的属性并复制为可变字典
            val newAttributes = att.attributesAtIndex(0u, effectiveRange = null).toMutableMap()
            // 添加/替换段落样式
            newAttributes[NSParagraphStyleAttributeName] = paragraphStyle
            // 创建新的 NSAttributedString
            NSAttributedString.create(
                string = att.string,
                attributes = newAttributes
            )
        }.let {
            MutableRect(
                0.0,
                0.0,
                it.size().useContents { width },
                it.size().useContents { height }) to it
        })
    }

    override fun stringByEncodingAsHTML(): String {
        return "<th></th>"
    }

    override fun description(): String {
        return "DTTableRowTextAttachment@${hashCode()}"
    }

    override fun drawInRect(rect: CValue<CGRect>, context: CGContextRef?) {
        // 确保 context 不为 null
        context?.let {
            it.usePinned { pinnedContext ->
                val ctx = pinnedContext.get()
                // 清除矩形区域的背景，使其透明
                // 设置描边颜色为绿色
                CGContextSetStrokeColorWithColor(ctx, UIColor.blackColor.CGColor)
                CGContextSetLineWidth(ctx, 2.0) // 设置边框宽度
                val rectValue = rect.useContents { this }
                // 绘制矩形的边框
                CGContextStrokeRect(ctx, rect)
                childNode.let { (rect, attributedString) ->
                    //这里是画 NSTextAttachment的
                    val textRectKotlin = rect.tokRect()
                    // 通过Context绘制文本
                    // 保存当前上下文状态
                    /*CGContextSaveGState(ctx)
                    // 平移 context 到文字的位置
                    CGContextTranslateCTM(
                        ctx,
                        textRectKotlin.left.toDouble(),
                        textRectKotlin.top.toDouble()
                    )*/
                    // 获取并绘制 attributedString
                    println(
                        "绘制文本: ${
                            textRectKotlin.let {
                                "x: ${it.left}, y: ${it.top}, width: ${it.size.width}, height: ${it.size.height}"
                            }
                        }  ${attributedString.string}"
                    )
                    attributedString.drawInRect(
                        rect.toCGRect()
                    )
                    // 恢复上下文状态
//                    CGContextRestoreGState(ctx)
                }
            }
        } ?: throw IllegalArgumentException("context is null")
    }
}


@OptIn(ExperimentalObjCName::class)
@ObjCName("DTTableTextAttachment")
/**
 * TR
 */
class DTTableColumnTextAttachment(element: DTHTMLElement, options: Map<Any?, *>?) :
    DTImageTextAttachment(
        element,
        options
    ) {
    @Deprecated(
        "Use constructor instead",
        replaceWith = ReplaceWith("DTImageTextAttachment(contentData, ofType)"),
        level = DeprecationLevel.ERROR
    )
    override fun initWithData(contentData: NSData?, ofType: String?): DTImageTextAttachment {
        return this
    }

    var sizeKeep = Rect.Zero

    internal val childNode = mutableListOf<DTTableRowTextAttachment>()
    fun addAll(rowAttachments: List<DTTableRowTextAttachment>?) {
        childNode.addAll(rowAttachments ?: return)
    }

    //    private val childNodes = mutableListOf<DTTableRowTextAttachment>()
    override fun stringByEncodingAsHTML(): String {
        return "<tr></tr>"
    }

    override fun drawInRect(rect: CValue<CGRect>, context: CGContextRef?) {
        // 确保 context 不为 null
        context?.let {
            it.usePinned { pinnedContext ->
                val ctx = pinnedContext.get()

                // 设置描边颜色为绿色
                CGContextSetStrokeColorWithColor(ctx, UIColor.blackColor.CGColor)
                CGContextSetLineWidth(ctx, 2.0) // 设置边框宽度

                childNode.forEachIndexed { index, attachment ->
                    //这里是画 DTTableRowTextAttachment的
                    val rowRect = attachment.sizeKeep
                    // 通过Context绘制文本
                    // 保存当前上下文状态
//                    CGContextSaveGState(ctx)
//                    // 平移 context 到文字的位置
//                    CGContextTranslateCTM(ctx, rowRect.left.toDouble(), rowRect.top.toDouble())

                    //这里是画 DTTableRowTextAttachment的
                    attachment.drawInRect(
                        CGRectMake(
                            x = rowRect.left.toDouble(),
                            y = rowRect.top.toDouble(),
                            rowRect.width.toDouble(),
                            rowRect.height.toDouble(),
                        ), context
                    )
                    // 恢复上下文状态
//                    CGContextRestoreGState(ctx)
                }
            }
        } ?: throw IllegalArgumentException("context is null")
    }

    override fun description(): String {
        return "DTTableColumnTextAttachment@${hashCode()}"
    }
}


class TableHandler(private val maxWidth: Float, private val density: Density) :
    UoocTagHandler("table") {

    private val trHandler = TrHandler()

    @OptIn(ExperimentalForeignApi::class)
    fun allHandlers(): List<TagHandlerProtocol> {
        return listOf(trHandler, this, *trHandler.childHandlers().toTypedArray())
    }

    private lateinit var tableAttachment: DTTableTextAttachment

    override fun handleStartTag(currentTag: DTHTMLElement) {
        tableAttachment = DTTableTextAttachment(maxWidth, currentTag, currentTag.attributes)
        currentTag.textAttachment = tableAttachment
    }

    override fun handleEndTag(currentTag: DTHTMLElement) {
        currentTag.setMargins(UIEdgeInsetsMake(10.0, 10.0, 10.0, 10.0))
        currentTag.paragraphStyle!!.apply {
            this.alignment = kCTTextAlignmentCenter
            this.headIndent = 10.0
        }
        tableAttachment.addAll((trHandler.attachment() as? List<Any>) ?: emptyList())
    }


    override fun description(): String {
        return "TableHandler@${hashCode()}"
    }

    override fun attachment(): Any? {
        return tableAttachment
    }

}


private class TrHandler : UoocTagHandler("tr") {
    private val thHandler = ThHandler()
    private val tdHandler = TdHandler()

    fun childHandlers(): List<TagHandlerProtocol> {
        return listOf(thHandler, tdHandler)
    }

    private val attachments = mutableListOf<DTTableColumnTextAttachment>()
    private val map = mutableMapOf<DTHTMLElement, DTTableColumnTextAttachment>()
    override fun handleStartTag(currentTag: DTHTMLElement) {
        map[currentTag] = DTTableColumnTextAttachment(currentTag, currentTag.attributes)
    }

    override fun handleEndTag(currentTag: DTHTMLElement) {
        currentTag.textAttachment = (map[currentTag] ?: return).apply {
            thHandler.attachment()?.let {
                addAll(it as List<DTTableRowTextAttachment>)
            }
            thHandler.clean()
            tdHandler.attachment()?.let {
                addAll(it as List<DTTableRowTextAttachment>)
            }
            tdHandler.clean()
            attachments.add(this)
        }
    }

    override fun description(): String {
        return "TrHandler@${hashCode()}"
    }

    override fun attachment(): Any? {
        return attachments
    }
}

private class ThHandler : UoocTagHandler("th") {
    private val attachments = mutableListOf<DTTableRowTextAttachment>()
    private val map = mutableMapOf<DTHTMLElement, DTTableRowTextAttachment>()
    override fun handleStartTag(currentTag: DTHTMLElement) {
        map[currentTag] = DTTableRowTextAttachment("th", currentTag, currentTag.attributes)
    }

    override fun handleEndTag(currentTag: DTHTMLElement) {
        currentTag.textAttachment = (map[currentTag] ?: return).apply {
            val list = currentTag.childNodes?.filterIsInstance<DTTextHTMLElement>() ?: emptyList()
            list.forEach {
                this.add(it.attributedString())
            }
            attachments.add(this)
        }
    }

    override fun description(): String {
        return "ThHandler@${hashCode()}"
    }

    override fun attachment(): Any? {
        return attachments
    }

    fun clean() {
        attachments.clear()
    }
}

private class TdHandler : UoocTagHandler("td") {
    private val attachments = mutableListOf<DTTableRowTextAttachment>()
    private val map = mutableMapOf<DTHTMLElement, DTTableRowTextAttachment>()
    override fun handleStartTag(currentTag: DTHTMLElement) {
        map[currentTag] = DTTableRowTextAttachment("td", currentTag, currentTag.attributes)
    }

    override fun handleEndTag(currentTag: DTHTMLElement) {
        currentTag.textAttachment = (map[currentTag] ?: return).apply {
            val list = currentTag.childNodes?.filterIsInstance<DTTextHTMLElement>() ?: emptyList()
            list.forEach {
                this.add(it.attributedString())
            }
            attachments.add(this)
        }
    }

    override fun description(): String {
        return "TdHandler@${hashCode()}"
    }

    override fun attachment(): Any? {
        return attachments
    }

    fun clean() {
        attachments.clear()
    }
}
