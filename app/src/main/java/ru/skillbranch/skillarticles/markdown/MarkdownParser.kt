package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {

    //group regex
    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

    //result regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+)"
    private const val ORDERED_LIST_ITEM_GROUP = "(^\\d\\. .+$)"
    private const val HEADER_GROUP = "(^#{1,6} .+$)"
    private const val QUOTE_GROUP = "(^> .+$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"
    private const val BLOCK_GROUP = "((?s)(?<!`)`{3}[^`\\s].*?[^`\\s]?`{3}(?!`))"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))"
    private const val IMAGE_GROUP = "(?m)^\\!\\[(.*)\\]\\((\\/assets\\/images\\/.+?\\.jpg)\\s?(\\\".+?\\\")?\\)"

    private const val MARKDOWN_GROUPS =
        "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP|" +
                "$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP|" +
                "$INLINE_GROUP|$LINK_GROUP|$ORDERED_LIST_ITEM_GROUP|$BLOCK_GROUP|$IMAGE_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to element
     */
    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    /**
     * clear markdown text to string
     */
    fun clear(string: String): String? {
        val elements = mutableListOf<Element>()
        val stringBuilder = StringBuilder()
        elements.addAll(findElements(string))
        elements.forEach {
            if(it is Element.Text) {
                stringBuilder.append(it.text)
            } else {
                stringBuilder.append(parseElement(it))
            }
        }
        return stringBuilder.toString()
    }

    private fun parseElement(element: Element): String? {
        val stringBuilder = StringBuilder()
        if (element.elements.isEmpty()) {
            stringBuilder.append(element.text)
        } else {element.elements.forEach {
                stringBuilder.append(parseElement(it))
        }}

        return stringBuilder.toString()
    }

    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            //if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            //found text
            var text: CharSequence

            //groups range for iterate by groups
            val groups = 1..12
            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }

            when (group) {
                -1 -> break@loop
                //UNORDERED LIST
                1 -> {
                    //text without "*."
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    //fin inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)

                    //nest find start from position "endIndex"
                    lastStartIndex = endIndex
                }
                //HEADER
                2 -> {
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length
                    //text without "#"
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)
                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //QUOTE
                3 -> {
                    //text without ">"
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    val subelements = findElements(text)
                    val element = Element.Quote(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //ITALIC
                4 -> {
                    //text without "_*"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subelements = findElements(text)
                    val element = Element.Italic(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //BOLD
                5 -> {
                    //text without "**{}**"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))
                    val subelements = findElements(text)
                    val element = Element.Bold(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //STRIKE
                6 -> {
                    //text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))
                    val subelements = findElements(text)
                    val element = Element.Strike(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //RULE
                7 -> {
                    //text without "***" insert empty char
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //INLINE CODE
                8 -> {
                    //text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subelements = findElements(text)
                    val element = Element.InlineCode(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //LINK
                9 -> {
                    //full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    val (title: String, link:String) = "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured
                    val element = Element.Link(link, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //ORDERED LIST
                10 -> {
                    //text without "1. 2.>"
                    val reg = "^\\d\\.".toRegex().find(string.subSequence(startIndex, endIndex))
                    text = string.subSequence(startIndex.plus(3), endIndex)
                    val subelements = findElements(text)
                    val element = Element.OrderedListItem(reg!!.value, text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //BLOCK CODE
                11 -> {
                    //text without "```{}```"
                    text = string.subSequence(startIndex.plus(3), endIndex.plus(-3))
                    val subelements = findElements(text)
                    val element = Element.BlockCode(text, subelements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //IMAGE
                12 -> {
                    val (alt: String, link:String, title: String?) = "^!\\[(.*)]\\((/assets/images/.+?\\.jpg)\\s?(\".+?\")?".toRegex().find(string.subSequence(startIndex, endIndex))!!.destructured
                    val element = Element.Image(url = link, alt = if (alt.isNotEmpty()) alt else null, text = title.replace("\"",""))
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }
        }

        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }

        return parents
    }
}

data class MarkdownText(val elements: List<Element>)

sealed class Element {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ",
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link:String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Image(
        val url: String,
        val alt: String?,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()
}