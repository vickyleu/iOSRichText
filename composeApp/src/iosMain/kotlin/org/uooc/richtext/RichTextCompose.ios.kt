@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
@file:OptIn(ExperimentalComposeUiApi::class)

package org.uooc.richtext

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.fastMapIndexedNotNull
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.DTFoundation.DTAnimatedGIFFromData
import uooc.DTCoreText.*
//import cocoapods.DTCoreText.DTAttributedLabel
//import cocoapods.DTCoreText.DTAttributedTextContentView
//import cocoapods.DTCoreText.DTAttributedTextContentViewDelegateProtocol
//import cocoapods.DTCoreText.DTCoreTextLayouter
//import cocoapods.DTCoreText.DTHTMLAttributedStringBuilder
//import cocoapods.DTCoreText.DTImageTextAttachment
//import cocoapods.DTCoreText.DTLazyImageView
//import cocoapods.DTCoreText.DTLazyImageViewDelegateProtocol
//import cocoapods.DTCoreText.DTTextAttachment
//import cocoapods.DTCoreText.create
//import cocoapods.DTFoundation.DTAnimatedGIFFromData
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Elements
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeEqualToSize
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGSizeZero
import platform.Foundation.NSAttributedString
import platform.Foundation.NSData
import platform.Foundation.NSMakeRange
import platform.Foundation.NSPredicate
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.containsString
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.isEqualToString
import platform.Foundation.length
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UILabel
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewContentMode
import platform.darwin.NSObject
import kotlin.collections.set
import kotlin.experimental.ExperimentalNativeApi

@OptIn(
    BetaInteropApi::class, ExperimentalNativeApi::class, ExperimentalForeignApi::class,
    ExperimentalForeignApi::class
)
@Composable
actual fun RichTextPlatformView(
    state: MutableState<RichTextCompose>,
    style: TextStyle,
    modifier: Modifier
) {
    val onTapTransformer = remember(key1 = state) { mutableStateOf(Offset.Zero) }
    val images = remember { mutableListOf<String>() }
    var height by remember(key1 = state) { state.value.height }

    val scale = UIScreen.mainScreen.scale.toFloat()
    with(LocalDensity.current) {
        val screenWidth =
            UIScreen.mainScreen.bounds.useContents { this.size.width.toFloat() }.toDp()
        Column(
            modifier,
        ) {
            BoxWithConstraints(
                Modifier
            ) {
                var attrString by remember {
                    val nsString = state.value.content
                    mutableStateOf(nsString)
                }
                LaunchedEffect(Unit) {
                    snapshotFlow { state.value.content }.distinctUntilChanged().collect {
                        val nsString = it
                        attrString = nsString
                    }
                }
                var attributedLabel by remember { mutableStateOf<DTAttributedLabel?>(null) }
                LaunchedEffect(key1 = state) {
                    snapshotFlow { height }
                        .filter {
                            it.isSpecified && it.value.isNaN().not()
                                    && maxWidth.toPx().toDouble().isNaN().not()
                        }
                        .filter { it.value != 0f }
                        .distinctUntilChanged()
                        .collect {
                            val nheight = it.coerceIn(minimumValue = 10.dp, maximumValue = 10000.dp)
                            attributedLabel?.apply {
                                println("新高度:${nheight.value}")
                                setFrame(
                                    CGRectMake(
                                        x = 0.0,
                                        y = 0.0,
                                        width = maxWidth.roundToPx().toDouble().coerceIn(
                                            minimumValue = 10.dp.roundToPx().toDouble(),
                                            maximumValue = screenWidth.roundToPx().toDouble()
                                        ),
                                        height = nheight.roundToPx().toDouble()
                                    )
                                )
                                relayoutText()
                            }
                        }
                }
                val coroutineScope = rememberCoroutineScope()
                var htmlString = remember(attrString) {
                    val html = Ksoup.parse(attrString).outerHtml()
                    val htmlFull = Ksoup.parse(html)
                    htmlFull.head().apply {
                        this.appendElement("meta").apply {
                            this.attr("name", "viewport")
                            this.attr("content", "width=device-width, initial-scale=1.0")
                        }

                        this.appendElement("script").apply {
                            this.appendText(
                                """
                                  <strong>MathJax = {
                                    tex: {
                                      inlineMath: [['${'$'}', '${'$'}'], ['\\(', '\\)']]
                                    }
                                  };</strong>
                              """.trimIndent()
                            )
                        }
                        this.appendElement("script").apply {
                            this.attr("type", "text/javascript")
                            this.attr("id", "MathJax-script")
                            this.attr("async", "")
                            this.attr(
                                "src",
                                "https://cdn.bootcss.com/mathjax/3.0.5/es5/tex-mml-chtml.js"
                            )
                        }

                        this.appendElement("style").apply {
                            this.attr("type", "text/css")
                            this.appendText(
                                """
                                html{
                                    font-size:${style.fontSize.value}px;
                                    width:100%;
                                    height:100%;
                                    margin:0;
                                    padding:0;
                                    text-align:left;
                                }
                                body{
                                    font-size:${style.fontSize.value}px;
                                    width:100%;
                                    height:100%;
                                    margin:0;
                                    padding:0;
                                    text-align:left;
                                }
                               table {
                                   width: 100%;
                                   border-collapse: collapse;
                               }
                               th {
                                   border: 1px solid #dddddd;
                                   text-align: left;
                                   padding: 8px;
                               }
                               td {
                                   border: 1px solid #dddddd;
                                   text-align: left;
                                   padding: 8px;
                               }
                               th {
                                   background-color: #f2f2f2;
                               }
                               tr:nth-child(even) {
                                   background-color: #f9f9f9;
                               }
                               .MathJax {
                                    font-size: 1.9em !important; /* 设置MathJax元素的文字大小 */
                                }
                                img{
                                    display:block;
                                    max-width:100%;
                                    height:auto;
                                }
                            """.trimIndent()
                            )
                        }

                    }
                    htmlFull.body().apply {
                        prependElement("script").apply {
                            this.attr("type", "text/javascript")
                            this.appendText(
                                """
                                window.onload = function(){
                                    var ${"$"}img = document.getElementsByTagName('img');
                                    var maxWidth = ${maxWidth.roundToPx()};
                                    for(var k in  ${"$"}img){
                                        ${"$"}img[k].style.max-width = maxWidth + 'px';
                                        ${"$"}img[k].style.height = auto;
                                    }
                                }
                                """.trimIndent()
                            )
                        }
                        val spans: Elements = this.select("span.math-tex")
                        spans.forEach {
                            val formated = it.text()
                                .replace("\\\\(", "\\(")
                                .replace("\\\\)", "\\)")
                                .replace("\\\\", "\\")
                            if (formated.isNotEmpty()) {
                                it.textNodes().forEach {
                                    it.remove()
                                }
                                it.text(formated + "\n")
                            }
                        }
                    }
                    Ksoup.parse(htmlFull.outerHtml()).outerHtml().let {
                        NSString.create(string = it)
                        /* */
                        /**
                         * //Html->富文本NSAttributedString
                         *//*
                        val htmlString = NSString.create(string = it)
                        val data = htmlString.dataUsingEncoding(NSUTF8StringEncoding)?: NSData.new()!!
                        val attributedString = NSAttributedString.create(data, documentAttributes = null)
                        *//*htmlString.dataUsingEncoding(NSUTF8StringEncoding)!!.let { data ->
                            val options = mutableMapOf<Any?, Any?>()
                            options[DTUseiOS6Attributes] = true.toNSNumber()
                            options[DTDefaultFontSize] = 16.0.toNSNumber()
                            options[DTDefaultFontFamily] = "Helvetica".toNSString()
                            DTHTMLAttributedStringBuilder(
                                hTML = data,
                                options = options,
                                documentAttributes = null
                            ).generatedAttributedString()
                        }*//*
                        *//*NSAttributedString.create(
                            data =
                                .dataUsingEncoding(NSUTF8StringEncoding)!!, options = mapOf(
                                NSDocumentTypeDocumentAttribute to NSHTMLTextDocumentType,
                                NSCharacterEncodingDocumentAttribute to NSUTF8StringEncoding
                            ), documentAttributes = null, error = null
                        )!!*//*

                        attributedString?:NSAttributedString.create(string = "")*/
                    }
                }

                val kvo = remember(attrString) {
                    object : NSObject(), DTAttributedTextContentViewDelegateProtocol,
                        DTLazyImageViewDelegateProtocol {
                        /**
                         * 图片占位
                         */
                        override fun attributedTextContentView(
                            attributedTextContentView: DTAttributedTextContentView?,
                            viewForAttachment: DTTextAttachment?,
                            frame: CValue<CGRect>
                        ): UIView? {
                            val attachment = (viewForAttachment?:return null)
                            val isImage = attachment.isKindOfClass(DTImageTextAttachment.`class`() as ObjCClass)
                            val isTable = attachment is DTTableTextAttachment
                            val isMathTex = attachment is MathTextAttachment
                            println("viewForAttachment:isImage=${attachment}")
                            if (isImage && !isTable) {
                                val imageURL = NSString.create(
                                    format = "%@",
                                    args = arrayOf(viewForAttachment.contentURL)
                                )
                                val imageView = DTLazyImageView(frame = frame)
                                imageView.delegate = this
                                imageView.contentMode =
                                    UIViewContentMode.UIViewContentModeScaleAspectFit
                                imageView.setBackgroundColor(UIColor.whiteColor)
                                imageView.image = viewForAttachment.image
                                imageView.url = viewForAttachment.contentURL
                                imageView.clipsToBounds = true
                                if (imageURL.containsString("gif")) {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            val gifData =
                                                NSData.dataWithContentsOfURL(viewForAttachment.contentURL!!)
                                            withContext(Dispatchers.Main) {
                                                imageView.image = DTAnimatedGIFFromData(gifData)
                                            }
                                        }
                                    }
                                }
                                return imageView

                            }
                            else if(isTable){
                                return TagRandererView(frame = frame, withAttachment = attachment as DTTableTextAttachment)
                            }else if(isMathTex){
                                val math = attachment as MathTextAttachment
                                val latex= math.latex
                                return TagRandererView(frame = frame, withAttachment = attachment as DTTableTextAttachment)
                            } else {
                                println("viewForAttachment:${viewForAttachment}")
                                return null
                            }
                        }

                        /**
                         *  根据a标签，自定义响应按钮，处理点击事件
                         */
                        override fun attributedTextContentView(
                            attributedTextContentView: DTAttributedTextContentView?,
                            viewForLink: NSURL?,
                            identifier: String?,
                            frame: CValue<CGRect>
                        ): UIView {
                            return UILabel().apply {
                                this.text = identifier
                                this.textColor = UIColor.blueColor
                                this.backgroundColor = UIColor.purpleColor
                                this.alpha = 0.5
                                this.setFrame(frame)
                                this.userInteractionEnabled = true
                                this.clipsToBounds = true
                                /*this.addGestureRecognizer(
                                    UITapGestureRecognizer(target = this, action = NSSelectorFromString("tap:"))
                                )*/
                            }
                        }

                        /**
                         * 懒加载获取图片大小
                         */
                        @Suppress("unchecked_cast")
                        override fun lazyImageView(
                            lazyImageView: DTLazyImageView?,
                            didChangeImageSize: CValue<CGSize>
                        ) {
                            val url = lazyImageView?.url ?: return
                            val imageSize = didChangeImageSize.useContents { this }

                            val key = url.absoluteString ?: ""
                            val pred = NSPredicate.predicateWithFormat("contentURL == %@", url)
                            var didUpdate = false
                            val attachments =
                                (attributedLabel?.layoutFrame?.textAttachmentsWithPredicate(pred) as? List<DTTextAttachment>)
                                    ?: emptyList()
                            val noSizeList =
                                attachments.fastMapIndexedNotNull { index, attachment ->
                                    if (CGSizeEqualToSize(
                                            attachment.originalSize,
                                            CGSizeZero.readValue()
                                        )
                                    ) index to attachment
                                    else {
                                        imageSizeMap[key] =
                                            Size(
                                                imageSize.width.toFloat(),
                                                imageSize.height.toFloat()
                                            )
                                        null
                                    }
                                }.toList()
                            if (noSizeList.isEmpty()) return
                            coroutineScope.launch {
                                val duplicateCount = noSizeList.count {
                                    NSString.create(
                                        format = "%@",
                                        args = arrayOf(it.second.contentURL)
                                    ).isEqualToString(key)
                                }
                                withContext(Dispatchers.IO) {
                                    val maxRect =
                                        Rect(0.0f, 0.0f, maxWidth.value, CGFLOAT_HEIGHT_UNKNOWN)
                                    val size = (if (imageSizeMap.containsKey(key).not()) {
                                        configNoSizeImageView(key)
                                    } else {
                                        imageSizeMap[key]
                                    }) ?: (Size(50.0f, 50.0f))
                                    val imgSizeScale = size.height / size.width
                                    val widthPx = maxRect.width
                                    val heightPx = widthPx * imgSizeScale
                                    val newSize = if (size.width > maxRect.width) {
                                        println("图片宽超过最大宽度:${size.width}x${size.height}")
                                         Size(widthPx, heightPx)
                                    } else {
                                        println("图片宽不超过最大宽度:${size.width}x${size.height}")
                                         Size(size.width, size.height)
                                    }
                                    imageSizeMap[key] = newSize

                                    for ((index, attachment) in noSizeList) {
                                        withContext(Dispatchers.Main) {
                                            val cgSize = CGSizeMake(
                                                newSize.width.toDouble(),
                                                newSize.height.toDouble()
                                            )
                                            attachment.displaySize = cgSize
                                            attachment.originalSize = cgSize
                                            val frame = lazyImageView.frame.useContents { this }
                                            lazyImageView.setFrame(
                                                CGRectMake(
                                                    frame.origin.x,
                                                    frame.origin.y,
                                                    cgSize.useContents { this.width },
                                                    cgSize.useContents { this.height })
                                            )
                                            didUpdate = true
                                        }
                                    }
                                    height += (newSize.height.dp)
                                    println("新高度:${height} size.height=${newSize.height}")
                                }
                                if (didUpdate) {
                                    withContext(Dispatchers.Main) {
                                        attributedLabel?.layouter = null
                                        attributedLabel?.relayoutText()
                                    }
                                }
                            }


                        }

                        val imageSizeMap = mutableMapOf<String, Size>()

                        /**
                         * #pragma mark - Delegate：DTAttributedTextContentViewDelegate
                         * 字符串中一些图片没有宽高，懒加载图片之后，在此方法中得到图片宽高
                         * 这个把宽高替换原来的html,然后重新设置富文本
                         *
                         * 修改为获取图片宽高,保存在一个map中,然后设置图片宽高
                         */
                        suspend fun configNoSizeImageView(url: String): Size? {
                           return withContext(Dispatchers.IO) {
                                return@withContext NSData.dataWithContentsOfURL(
                                    NSURL.URLWithString(
                                        URLString = url
                                    )!!
                                )
                                    ?.let {
                                        val image = UIImage.imageWithData(it)
                                        println("下载到图片没? ${image?.size?.useContents { this.let { 
                                            Size(it.width.toFloat(), it.height.toFloat())
                                        } }}")
                                        image?.size?.useContents {
                                            this.let {
                                                Size(it.width.toFloat(), it.height.toFloat())
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
                var viewIsAttached by remember(attrString) { mutableStateOf(false) }



                UIKitView(factory = {
                    val label = DTAttributedLabel(
                        frame = CGRectMake(
                            x = 0.0,
                            y = 0.0,
                            width = maxWidth.roundToPx().coerceIn(
                                minimumValue = 10.dp.roundToPx(),
                                maximumValue = screenWidth.roundToPx()
                            ).toDouble(),
                            height = max(height, 10.dp).roundToPx().toDouble()
                        )
                    )
                    label.autoresizingMask =
                        UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
                    label.backgroundColor = UIColor.redColor
                    label.setDelegate(kvo)
                    label.apply {
                        val maxRect = Rect(0.0f, 0.0f, maxWidth.value, CGFLOAT_HEIGHT_UNKNOWN)
                        val measureHeight = htmlString.getAttributedTextHeightHtml(maxRect,this@with)
                        println("新高度:${measureHeight}")
                        height = max(measureHeight.dp, 10.dp)
                        this.attributedString = htmlString.getAttributedStringWithHtml(maxRect.width,this@with)
                        attributedLabel = this

                    }
                },
                    modifier = Modifier.fillMaxWidth()
                        .requiredHeightIn(
                            min = height.coerceIn(minimumValue = 10.dp, maximumValue = 100000.dp)
                        ),
                    update = { view ->
//                        // 根据子视图内容的固有高度调整父视图的高度
//                        val weakSuperview = WeakReference(view.parent(4) ?: return@UIKitView)
//                        weakSuperview.get()?.apply {
//                            viewIsAttached = true
//                            if (this.checkWildPointer()) return@UIKitView
//                            this.removeControllerColor(coroutineScope)
//                        }
//                        view.removeInteropWrappingViewColor(coroutineScope)
                    }, onRelease = {
                        /*attributedLabel?.removeObserver(kvo, forKeyPath = "layoutFrame")
                        attributedLabel?.removeFromSuperview()
                        attributedLabel = null*/
                    },
                    properties = UIKitInteropProperties(
                        interactionMode = null,
                        isNativeAccessibilityEnabled = true
                    )
                )
            }
        }
    }

}


/**
 * Html转NSAttributedString
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSString.getAttributedStringWithHtml(maxWidth: Float, density: Density): NSAttributedString {
    val data = this.dataUsingEncoding(NSUTF8StringEncoding) ?: NSData.new()!!
    val options = mapOf<Any?,Any?>(NSBaseURLDocumentOption to data)
    val stringBuilder = DTHTMLAttributedStringBuilder(hTML = data,
        options = options,
        documentAttributes = null)
    //  表格处理
    stringBuilder.registerTagHandlers(tagHandlers = TableHandler(maxWidth,density).allHandlers())
    stringBuilder.registerTagHandlers(tagHandlers = listOf(MathHandler(maxWidth,density)))

    val callbackBlock = options[DTWillFlushBlockCallBack] as? DTHTMLAttributedStringBuilderWillFlushCallback
    if(callbackBlock!=null){
        stringBuilder.setWillFlushCallback(callbackBlock)
    }
    val attributedString = stringBuilder.generatedAttributedString()
    return attributedString ?: NSAttributedString.create(string = "")
}

/**
 * 使用HtmlString,和最大左右间距，计算视图的高度
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSString.getAttributedTextHeightHtml(maxRect: Rect, density: Density): Float {
    val attributedString = this.getAttributedStringWithHtml(maxRect.width, density)
    val layouter = DTCoreTextLayouter(attributedString = attributedString)
    val entireString = NSMakeRange(0u, attributedString.length)
    val layoutFrame = layouter.layoutFrameWithRect(
        CGRectMake(
            x = maxRect.left.toDouble(),
            y = maxRect.top.toDouble(),
            width = maxRect.width.toDouble(),
            height = maxRect.height.toDouble()
        ),
        range = entireString
    )

    val measureHeight = layoutFrame?.frame?.useContents { this.size.height.toFloat() } ?: 0.0f
    println("layoutFrame:${measureHeight}")
    return measureHeight
}
