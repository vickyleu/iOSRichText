package org.uooc.richtext

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document

object HtmlFormater {
    fun getFileFromHtmlContent(html:String): Pair<String, ArrayList<Pair<String, String>>> {
        val doc: Document = Ksoup.parse(html = html)
        val outer = doc.toString()
        val docFull: Document = Ksoup.parse(html = outer)
        val fileList = arrayListOf<Pair<String,String>>()
        if(docFull.body().hasChildNodes()){
            docFull.body().children().forEach {
                if(it.hasText()){
                    if(it.childrenSize()==2 && it.children()[0].tagName()=="img" && it.children()[1].tagName()=="a"){
                        val fileName = it.children()[1].text()
                        val href = it.children()[1].attr("href")
                        println("fileName=$fileName  href=$href")
                        fileList.add(Pair(fileName,href))
                        it.remove()
                    }
                }
            }
        }
        return docFull.outerHtml() to fileList
    }

    fun replaceLatexHtmlContent(html:String): String{
        val doc: Document = Ksoup.parse(html = html)
        val outer = doc.toString()
        val docFull: Document = Ksoup.parse(html = outer)
//        if(docFull.body().hasChildNodes()){
//            docFull.body().children().eachTag()
//        }
        return docFull.outerHtml()
    }

//    private fun Elements.eachTag(){
//        this.forEach {
//            if(it.hasText()){
//                if(it.childrenSize()>0){
//                    it.children().eachTag()
//                }else{
//                    println("it.tag()::${it.tag()}")
//                }
//            }
//        }
//    }
}