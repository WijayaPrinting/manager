package com.hendraanggrian.openpss.ui

import com.hendraanggrian.openpss.FxComponent
import com.hendraanggrian.openpss.FxSetting
import com.hendraanggrian.openpss.OpenPssApplication
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.data.Employee
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.layout.StackPane
import ktfx.getValue
import ktfx.layouts.NodeInvokable
import ktfx.setValue
import java.net.URL
import java.util.Properties
import java.util.ResourceBundle

/** Base class of all controllers. */
@Suppress("LeakingThis")
open class OpenPssController : Initializable, FxComponent {

    private lateinit var _setting: FxSetting
    override val setting: FxSetting
        get() {
            if (!::_setting.isInitialized) {
                _setting = FxSetting()
            }
            return _setting
        }

    override lateinit var resourceBundle: ResourceBundle
    override val valueProperties: Properties = OpenPssApplication::class.java
        .getResourceAsStream(R.value.properties_value)
        .use { stream -> Properties().apply { load(stream) } }

    override lateinit var rootLayout: StackPane
    override lateinit var login: Employee

    private lateinit var extras: MutableMap<String, Any>

    override fun initialize(location: URL, resources: ResourceBundle) {
        this.resourceBundle = resources
        (this as? Refreshable)?.refresh()
    }

    /** Register extra [value] with [key]. */
    fun addExtra(key: String, value: Any): OpenPssController {
        if (!::extras.isInitialized) {
            extras = mutableMapOf()
        }
        extras[key] = value
        return this
    }

    /** Get extra registered with [key], should be executed in platform thread. */
    fun <T : Any> getExtra(key: String): T {
        check(::extras.isInitialized) { "No extras found in this controller." }
        @Suppress("UNCHECKED_CAST") return checkNotNull(extras[key] as T) { "No extra found with that key." }
    }
}

open class ActionController : OpenPssController() {

    private val titleProperty: StringProperty = SimpleStringProperty(null)
    fun titleProperty(): StringProperty = titleProperty
    var title: String? by titleProperty

    val actions = mutableListOf<Node>()

    private val actionInvokable = object : NodeInvokable {
        override fun <R : Node> R.invoke(): R = also { actions += it }
    }

    /** Override this function to add actions. */
    open fun NodeInvokable.onCreateActions() {
    }

    override fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        actionInvokable.onCreateActions()
    }
}