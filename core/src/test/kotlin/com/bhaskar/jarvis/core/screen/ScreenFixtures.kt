package com.bhaskar.jarvis.core.screen

/** Compact builders for screen trees in tests. Defaults describe a visible, enabled, plain label. */
internal fun label(
    id: String,
    text: String? = null,
    desc: String? = null,
    clickable: Boolean = false,
    enabled: Boolean = true,
    visible: Boolean = true,
    children: List<ScreenNode> = emptyList(),
) = ScreenNode(
    id = id, packageName = "test.app", className = "android.widget.View",
    text = text, contentDescription = desc, clickable = clickable,
    editable = false, focused = false, password = false,
    enabled = enabled, visibleToUser = visible,
    bounds = Bounds(0, 0, 100, 100), children = children,
)

internal fun field(
    id: String,
    focused: Boolean = true,
    password: Boolean = false,
    enabled: Boolean = true,
    text: String? = null,
) = ScreenNode(
    id = id, packageName = "test.app", className = "android.widget.EditText",
    text = text, contentDescription = null, clickable = true,
    editable = true, focused = focused, password = password,
    enabled = enabled, visibleToUser = true,
    bounds = Bounds(0, 0, 100, 40),
)

internal fun screen(vararg children: ScreenNode) = ScreenNode(
    id = "root", packageName = "test.app", className = "android.widget.FrameLayout",
    text = null, contentDescription = null, clickable = false,
    editable = false, focused = false, password = false,
    enabled = true, visibleToUser = true,
    bounds = Bounds(0, 0, 1080, 2400), children = children.toList(),
)
