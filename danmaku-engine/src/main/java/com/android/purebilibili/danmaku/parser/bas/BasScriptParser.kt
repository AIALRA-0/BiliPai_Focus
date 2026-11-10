package com.android.purebilibili.danmaku.parser.bas

/** Independent implementation of the BAS language, not an evaluator for JavaScript. */
object BasScriptParser {
    fun parse(source: String): BasProgram = Parser(source).parse()

    /**
     * Compiles lifetime metadata with the normal BAS grammar, without retaining visual string
     * values or constructing renderable elements/transitions. Non-timing property, easing,
     * parent and target validation is intentionally omitted.
     */
    fun parseDurationMs(source: String): Long = Parser(source, timingOnly = true).parseDurationMs()

    private val reservedWords = setOf("def", "let", "set", "then", "apply", "clone")
    private val timeComponents = Regex("((?:[0-9]*\\.)?[0-9]+(?:[eE][+-]?[0-9]+)?)(ms|h|m|s)")
    private val textMutableProperties = setOf("x", "y", "scale", "alpha", "color", "rotateX", "rotateY", "rotateZ", "content", "fontSize")
    private val buttonMutableProperties = setOf("x", "y", "text", "fontSize")
    private val pathMutableProperties = setOf("x", "y", "alpha")
    private val defaultAttributes = BasElementType.entries.associateWith(::buildDefaults)
    private val definitionProperties = defaultAttributes.mapValues { (type, attrs) ->
        attrs.keys + "duration" + when (type) {
            BasElementType.TEXT -> setOf("parent", "width", "height")
            BasElementType.BUTTON -> setOf("target")
            BasElementType.PATH -> setOf("d", "viewBox", "width", "height")
        }
    }
    private val textKeys = setOf("content", "text", "fontFamily", "d", "viewBox", "parent")
    private val numericKeys = setOf("x", "y", "zIndex", "scale", "alpha", "color", "anchorX", "anchorY", "fontSize", "bold", "textShadow", "strokeWidth", "strokeColor", "rotateX", "rotateY", "rotateZ", "textColor", "textAlpha", "fillColor", "fillAlpha", "width", "height", "borderColor", "borderAlpha", "borderWidth")

    private data class Definition(val type: BasElementType, val attrs: Map<String, BasValue>, val token: BasToken)
    private data class Template(val type: BasElementType, val defaults: Map<String, BasValue>, val attrs: Map<String, BasValue>)
    private sealed interface SetNode {
        val duration: Long
        data class Unit(val target: String, val attrs: Map<String, BasValue>, override val duration: Long, val easing: String, val order: Int, val location: BasToken) : SetNode
        data class Group(val children: List<SetNode>, val serial: Boolean, val location: BasToken) : SetNode {
            override val duration: Long = if (serial) children.fold(0L) { sum, child -> checkedAdd(sum, child.duration, location) } else children.maxOfOrNull { it.duration } ?: 0L
        }
    }

    private class Parser(source: String, private val timingOnly: Boolean = false) {
        private val lexer = BasLexer(source, retainStrings = !timingOnly)
        private var token = lexer.next()
        private var following = lexer.next()
        private val objects = linkedMapOf<String, Definition>()
        private val templates = linkedMapOf<String, Template>()
        private val sets = mutableListOf<SetNode>()
        private var generated = 0
        private var order = 0

        private fun parseStatements() {
            while (token.kind != BasTokenKind.END) {
                when (token.text) {
                    "def" -> definition()
                    "let" -> binding()
                    "set", "{" -> sets.add(setExpression())
                    else -> fail("Expected def, let or set")
                }
            }
        }

        fun parseDurationMs(): Long {
            parseStatements()
            val ends = mutableMapOf<String, Long>()
            for (node in sets) collectEnds(node, 0L, ends)
            return objects.maxOfOrNull { (name, definition) ->
                definition.attrs["duration"]?.let { time(it, definition.token) } ?: ends[name] ?: 4000L
            } ?: 0L
        }

        private fun collectEnds(node: SetNode, start: Long, ends: MutableMap<String, Long>) {
            when (node) {
                is SetNode.Unit -> {
                    val end = checkedAdd(start, node.duration, node.location)
                    ends[node.target] = maxOf(ends[node.target] ?: 0L, end)
                }
                is SetNode.Group -> {
                    var next = start
                    for (child in node.children) {
                        collectEnds(child, next, ends)
                        if (node.serial) next = checkedAdd(next, child.duration, node.location)
                    }
                }
            }
        }

        fun parse(): BasProgram {
            parseStatements()
            val transitions = mutableListOf<BasTransition>()
            sets.forEachIndexed { group, node -> flatten(node, 0L, group, transitions) }
            val elements = objects.map { (name, definition) ->
                val attrs = defaults(definition.type) + definition.attrs
                val durationValue = attrs["duration"]
                val lifetime = if (durationValue != null) time(durationValue, definition.token) else {
                    transitions.filter { it.elementName == name }.maxOfOrNull { checkedAdd(it.startTimeMs, it.durationMs, definition.token) } ?: 4000L
                }
                val parent = if (definition.type == BasElementType.TEXT) attrs["parent"]?.let { string(it, definition.token) } else null
                val destination = if (definition.type == BasElementType.BUTTON) attrs["target"]?.let { target(it, definition.token) } else null
                BasElement(name, definition.type, attrs, lifetime, parent, destination)
            }
            checkParents(elements)
            val texts = ArrayList<String>()
            for (element in elements) {
                if (element.type != BasElementType.PATH) {
                    texts.add(element.attributes.text(if (element.type == BasElementType.TEXT) "content" else "text"))
                }
            }
            for (transition in transitions) {
                val raw = transition.properties["content"] ?: transition.properties["text"] ?: continue
                val value = if (raw is BasValue.Array) raw.values.firstOrNull() else raw
                when (value) {
                    is BasValue.Text -> texts.add(value.value)
                    is BasValue.Reference -> texts.add(value.name)
                    else -> Unit
                }
            }
            return BasProgram(elements, transitions, elements.maxOfOrNull { it.durationMs } ?: 0L, texts.joinToString("\n"))
        }

        private fun definition() {
            expect("def")
            val typeToken = id()
            val type = elementType(typeToken)
            val name = id().text
            if (accept("(")) {
                val parameters = attributes(")", retainAll = true)
                val attrs = block()
                templates[name] = Template(type, parameters, attrs)
                objects.remove(name)
            } else {
                objects[name] = Definition(type, block(), typeToken)
                templates.remove(name)
            }
            accept(";")
        }

        private fun binding() {
            expect("let")
            val name = id().text
            expect("=")
            val temporary = objectExpression()
            val definition = objects.remove(temporary) ?: fail("Unknown object '$temporary'")
            objects[name] = definition
            templates.remove(name)
            accept(";")
        }

        private fun objectExpression(): String {
            if (accept("(")) {
                val name = objectExpression()
                if (token.text == "{") {
                    val definition = objects.getValue(name)
                    objects[name] = definition.copy(attrs = definition.attrs + block())
                }
                expect(")")
                return name
            }
            val base = id()
            var name: String
            do { name = "@bas_${generated++}" } while (name in objects || name in templates)
            val definition = if (accept("(")) {
                val template = templates[base.text] ?: fail("Unknown template '${base.text}'", base)
                val positional = mutableListOf<BasValue>()
                val named = linkedMapOf<String, BasValue>()
                if (!accept(")")) {
                    while (true) {
                        if (token.kind == BasTokenKind.ID && following.text == "=") {
                            val parameter = id()
                            expect("=")
                            if (!template.defaults.containsKey(parameter.text)) fail("Unknown template parameter '${parameter.text}'", parameter)
                            named[parameter.text] = value(false)
                        } else positional.add(value(false))
                        if (!accept(",")) { expect(")"); break }
                        if (accept(")")) break
                    }
                }
                val parameters = linkedMapOf<String, BasValue>()
                parameters.putAll(named)
                var position = 0
                for ((key, default) in template.defaults) {
                    if (!parameters.containsKey(key)) parameters[key] = if (position < positional.size) positional[position++] else default
                }
                if (position != positional.size) fail("Too many positional template arguments", base)
                // Bind recursively, including object targets and easing arrays, before layout compilation.
                val bound = template.attrs.mapValues { bind(it.value, parameters, base, mutableSetOf()) }
                Definition(template.type, bound, base)
            } else {
                val attrs = block()
                val type = typeOrNull(base.text)
                if (type != null) Definition(type, attrs, base) else {
                    val original = objects[base.text] ?: fail("Unknown object '${base.text}'", base)
                    original.copy(attrs = original.attrs + attrs, token = base)
                }
            }
            objects[name] = definition
            return name
        }

        private fun bind(value: BasValue, parameters: Map<String, BasValue>, location: BasToken, visiting: MutableSet<String>): BasValue = when (value) {
            is BasValue.Reference -> {
                val bound = parameters[value.name] ?: fail("Unknown template parameter '${value.name}'", location)
                if (!visiting.add(value.name)) fail("Cyclic template parameter '${value.name}'", location)
                val result = bind(bound, parameters, location, visiting)
                visiting.remove(value.name)
                result
            }
            is BasValue.Object -> value.copy(attributes = value.attributes.mapValues { bind(it.value, parameters, location, visiting) })
            is BasValue.Array -> value.copy(values = value.values.map { bind(it, parameters, location, visiting) })
            else -> value
        }

        private fun setExpression(): SetNode {
            val location = token
            val children = mutableListOf(setAtom())
            while (accept("then")) children.add(setAtom())
            return if (children.size == 1) children[0] else SetNode.Group(children, true, location)
        }

        private fun setAtom(): SetNode {
            val groupLocation = token
            if (accept("{")) {
                val children = mutableListOf<SetNode>()
                while (token.text != "}") {
                    if (token.kind == BasTokenKind.END) fail("Unterminated parallel group")
                    children.add(setExpression())
                }
                expect("}")
                if (children.isEmpty()) fail("An animation group cannot be empty")
                return SetNode.Group(children, false, groupLocation)
            }
            expect("set")
            val target = if (accept("(")) objectExpression().also { expect(")") } else id().text
            val definition = objects[target] ?: fail("Unknown set object '$target'")
            val location = token
            val attrs = block()
            val durationToken = token
            if (token.kind != BasTokenKind.TIME) fail("Set requires a time duration")
            advance()
            val duration = time(decodeTime(durationToken), durationToken)
            var easing = "linear"
            if (accept(",")) {
                if (token.kind != BasTokenKind.STRING) fail("Expected easing string")
                easing = token.text
                if (!timingOnly) validateEasing(easing, token)
                advance()
            }
            if (timingOnly) return SetNode.Unit(target, emptyMap(), duration, "linear", order++, durationToken)
            val properties = attrs.filterKeys { it in mutableProperties(definition.type) }
            for (value in properties.values) {
                if (value is BasValue.Array) {
                    if (value.values.size != 2 || value.values[1] !is BasValue.Text) fail("Property easing requires [value, \"easing\"]", location)
                    validateEasing((value.values[1] as BasValue.Text).value, location)
                }
            }
            validateProperties(properties, location, animation = true)
            return SetNode.Unit(target, properties, duration, easing, order++, durationToken)
        }

        private fun flatten(node: SetNode, start: Long, group: Int, result: MutableList<BasTransition>) {
            when (node) {
                is SetNode.Unit -> result.add(BasTransition(node.target, node.attrs, start, node.duration, node.easing, node.order, group))
                is SetNode.Group -> {
                    var next = start
                    for (child in node.children) {
                        flatten(child, next, group, result)
                        if (node.serial) next = checkedAdd(next, child.duration, node.location)
                    }
                }
            }
        }

        private fun block(): Map<String, BasValue> { expect("{"); return attributes("}") }
        private fun attributes(close: String, retainAll: Boolean = false): Map<String, BasValue> {
            val result = linkedMapOf<String, BasValue>()
            while (!accept(close)) {
                val key = id()
                expect("=")
                val parsed = value(true)
                if (!timingOnly || retainAll || key.text == "duration") result[key.text] = parsed
                accept(";")
            }
            return result
        }

        private fun value(allowReference: Boolean): BasValue {
            val current = token
            return when (current.kind) {
                BasTokenKind.STRING -> { advance(); BasValue.Text(current.text) }
                BasTokenKind.TIME -> { advance(); decodeTime(current) }
                BasTokenKind.HEX -> {
                    advance()
                    val number = current.text.substring(2).toULongOrNull(16)?.toDouble() ?: fail("Hexadecimal number is too large", current)
                    BasValue.Number(number)
                }
                BasTokenKind.NUMBER -> number(1.0)
                BasTokenKind.ID -> {
                    if (current.text in reservedWords) fail("Reserved word is not a value", current)
                    advance()
                    if (token.text == "{") BasValue.Object(current.text, block())
                    else if (allowReference) BasValue.Reference(current.text)
                    else fail("Arguments require a literal or aggregate value", current)
                }
                BasTokenKind.SYMBOL -> when (current.text) {
                    "+", "-" -> { advance(); if (token.kind != BasTokenKind.NUMBER) fail("Only decimal numbers may have a sign"); number(if (current.text == "-") -1.0 else 1.0) }
                    "[" -> {
                        advance()
                        val items = mutableListOf<BasValue>()
                        if (!accept("]")) {
                            val first = value(false)
                            if (first is BasValue.Array || first is BasValue.Object) fail("First array item must be a primitive", current)
                            items.add(first)
                            while (accept(",")) {
                                val item = value(true)
                                if (item is BasValue.Array || item is BasValue.Object) fail("Array items must be primitive values or identifiers", current)
                                items.add(item)
                            }
                            expect("]")
                        }
                        BasValue.Array(items)
                    }
                    else -> fail("Expected BAS value")
                }
                else -> fail("Expected BAS value")
            }
        }

        private fun number(sign: Double): BasValue {
            val location = token
            val number = location.text.toDoubleOrNull()?.times(sign)?.takeIf(Double::isFinite) ?: fail("Number is not finite", location)
            advance()
            return BasValue.Number(number, if (accept("%")) BasUnit.PERCENT else BasUnit.NUMBER)
        }

        private fun decodeTime(location: BasToken): BasValue.Number {
            var total = 0.0
            for (match in timeComponents.findAll(location.text)) {
                val factor = when (match.groupValues[2]) { "h" -> 3600000.0; "m" -> 60000.0; "s" -> 1000.0; else -> 1.0 }
                total += match.groupValues[1].toDouble() * factor
            }
            if (!total.isFinite() || total >= Long.MAX_VALUE.toDouble()) fail("Time is too large", location)
            return BasValue.Number(total, BasUnit.TIME)
        }

        private fun time(value: BasValue, location: BasToken): Long {
            val number = value as? BasValue.Number ?: fail("Expected a time value", location)
            if (number.unit != BasUnit.TIME || number.value < 0 || number.value >= Long.MAX_VALUE.toDouble()) fail("Expected nonnegative time", location)
            return number.value.toLong()
        }

        private fun string(value: BasValue, location: BasToken): String = when (value) {
            is BasValue.Text -> value.value
            is BasValue.Reference -> value.name
            else -> fail("Expected text or name", location)
        }

        private fun target(value: BasValue, location: BasToken): BasTarget {
            val obj = value as? BasValue.Object ?: fail("Target must be seek, av or bangumi object", location)
            val attrs = obj.attributes
            fun identifier(key: String): Long? {
                val field = attrs[key] ?: return null
                val n = field as? BasValue.Number ?: fail("Target $key must be a number", location)
                if (n.unit != BasUnit.NUMBER || n.value < 0 || n.value >= Long.MAX_VALUE.toDouble() || n.value % 1.0 != 0.0) fail("Target $key must be a nonnegative integer", location)
                return n.value.toLong()
            }
            val time = attrs["time"]?.let { time(it, location) } ?: 0L
            return when (obj.type) {
                "seek" -> BasTarget.Seek(time)
                "av" -> {
                    val aid = identifier("av")
                    val bvid = attrs["bvid"]?.let { string(it, location) }
                    if (aid == null && bvid.isNullOrBlank()) fail("Video target requires av or bvid", location)
                    val page = identifier("page") ?: 1L
                    if (page !in 1..Int.MAX_VALUE.toLong()) fail("Invalid video page", location)
                    BasTarget.Video(aid, bvid, page.toInt(), time)
                }
                "bangumi" -> {
                    val season = identifier("seasonId")
                    val episode = identifier("episodeId")
                    if (season == null && episode == null) fail("Bangumi target requires seasonId or episodeId", location)
                    BasTarget.Bangumi(season, episode, time)
                }
                else -> fail("Unknown target type '${obj.type}'", location)
            }
        }

        private fun checkParents(elements: List<BasElement>) {
            val byName = elements.associateBy { it.name }
            val checked = mutableSetOf<String>()
            val visiting = mutableSetOf<String>()
            fun visit(element: BasElement) {
                if (element.name in checked) return
                if (!visiting.add(element.name)) fail("Cyclic parent '${element.name}'", objects.getValue(element.name).token)
                element.parentName?.let { byName[it] }?.let(::visit)
                visiting.remove(element.name)
                checked.add(element.name)
            }
            for (element in elements) {
                validateProperties(element.attributes, objects.getValue(element.name).token, animation = false,
                    supported = definitionProperties.getValue(element.type))
                visit(element)
            }
        }

        private fun validateProperties(
            attrs: Map<String, BasValue>,
            location: BasToken,
            animation: Boolean,
            supported: Set<String>? = null
        ) {
            for ((key, raw) in attrs) {
                if (supported != null && key !in supported) continue
                val value = if (animation && raw is BasValue.Array) raw.values[0] else raw
                if (key in textKeys && value !is BasValue.Text && value !is BasValue.Reference) fail("$key must be text", location)
                if (key in numericKeys && (value !is BasValue.Number || value.unit == BasUnit.TIME)) fail("$key must be numeric or a percentage", location)
            }
            // Unknown attributes remain typed data, but never become unsupported animation channels.
        }

        private fun validateEasing(value: String, location: BasToken) {
            try { BasEasing.parse(value) } catch (e: IllegalArgumentException) { fail(e.message ?: "Invalid easing", location) }
        }
        private fun elementType(location: BasToken) = typeOrNull(location.text) ?: fail("Unknown element type '${location.text}'", location)
        private fun id(): BasToken {
            if (token.kind != BasTokenKind.ID || token.text in reservedWords) fail("Expected identifier")
            return token.also { advance() }
        }
        private fun advance() { token = following; following = lexer.next() }
        private fun accept(text: String): Boolean = if (token.text == text && token.kind != BasTokenKind.STRING) { advance(); true } else false
        private fun expect(text: String) { if (!accept(text)) fail("Expected '$text'") }
        private fun fail(message: String, location: BasToken = token): Nothing = throw BasParseException(message, location.line, location.column)
    }

    private fun typeOrNull(name: String): BasElementType? = when (name) { "text" -> BasElementType.TEXT; "button" -> BasElementType.BUTTON; "path" -> BasElementType.PATH; else -> null }
    internal fun mutableProperties(type: BasElementType): Set<String> = when (type) {
        BasElementType.TEXT -> textMutableProperties
        BasElementType.BUTTON -> buttonMutableProperties
        BasElementType.PATH -> pathMutableProperties
    }

    private fun defaults(type: BasElementType): Map<String, BasValue> = defaultAttributes.getValue(type)

    private fun buildDefaults(type: BasElementType): Map<String, BasValue> {
        fun n(value: Double) = BasValue.Number(value)
        fun t(value: String) = BasValue.Text(value)
        val common = mapOf("x" to n(0.0), "y" to n(0.0), "zIndex" to n(0.0), "scale" to n(1.0))
        return common + when (type) {
            BasElementType.TEXT -> mapOf("content" to t("请输入内容"), "alpha" to n(1.0), "color" to n(16777215.0), "anchorX" to n(0.0), "anchorY" to n(0.0), "fontSize" to n(25.0), "fontFamily" to t("SimHei"), "bold" to n(1.0), "textShadow" to n(1.0), "strokeWidth" to n(0.0), "strokeColor" to n(16777215.0), "rotateX" to n(0.0), "rotateY" to n(0.0), "rotateZ" to n(0.0))
            BasElementType.BUTTON -> mapOf("text" to t("请输入内容"), "fontSize" to n(25.0), "textColor" to n(0.0), "textAlpha" to n(1.0), "fillColor" to n(16777215.0), "fillAlpha" to n(1.0))
            BasElementType.PATH -> mapOf("alpha" to n(1.0), "borderColor" to n(0.0), "borderAlpha" to n(1.0), "borderWidth" to n(0.0), "fillColor" to n(16777215.0), "fillAlpha" to n(1.0))
        }
    }

    private fun checkedAdd(a: Long, b: Long, location: BasToken): Long {
        if (Long.MAX_VALUE - a < b) throw BasParseException("Animation duration overflow", location.line, location.column)
        return a + b
    }
}
