package com.wijayaprinting

import bindings.`else`
import bindings.`if`
import bindings.then
import com.wijayaprinting.BuildConfig.DEBUG
import com.wijayaprinting.dao.Employee
import com.wijayaprinting.dao.Employees
import com.wijayaprinting.data.Language
import com.wijayaprinting.dialog.AboutDialog
import com.wijayaprinting.io.MySQLFile
import com.wijayaprinting.io.PreferencesFile
import com.wijayaprinting.scene.control.IPField
import com.wijayaprinting.scene.control.IntField
import com.wijayaprinting.scene.control.intField
import com.wijayaprinting.scene.control.ipField
import com.wijayaprinting.utils.*
import io.reactivex.rxkotlin.subscribeBy
import javafx.application.Application
import javafx.application.Platform.exit
import javafx.event.ActionEvent.ACTION
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.ButtonBar.ButtonData.OK_DONE
import javafx.scene.control.ButtonType.CANCEL
import javafx.scene.control.ButtonType.OK
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.Stage
import kotfx.*
import org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX
import org.apache.log4j.BasicConfigurator.configure
import org.jetbrains.exposed.sql.update
import java.awt.Toolkit.getDefaultToolkit
import java.net.URL
import java.util.*

class App : Application(), Resourced {

    companion object {
        lateinit var EMPLOYEE: String
        var FULL_ACCESS: Boolean = false

        @JvmStatic fun main(vararg args: String) = launch(App::class.java, *args)
    }

    override val resources: ResourceBundle = Language.parse(PreferencesFile.language.value).getResources("string")

    override fun init() {
        if (DEBUG) configure()
    }

    override fun start(stage: Stage) {
        stage.icon = Image(R.png.logo_launcher)
        setOSXIcon(getResource(R.png.logo_launcher))
        dialog<Any>(getString(R.string.app_name)) {
            lateinit var employeeField: TextField
            lateinit var passwordField: PasswordField
            lateinit var serverIPField: IPField
            lateinit var serverPortField: IntField
            lateinit var serverUserField: TextField
            lateinit var serverPasswordField: PasswordField
            headerText = getString(R.string.login)
            graphic = ImageView(R.png.ic_launcher)
            isResizable = false
            content = gridPane {
                gap(8)
                label(getString(R.string.language)) col 0 row 0
                choiceBox(Language.listAll()) {
                    maxWidth = Double.MAX_VALUE
                    selectionModel.select(Language.parse(PreferencesFile.language.value))
                    selectionModel.selectedItemProperty().addListener { _, _, language ->
                        PreferencesFile.language.set(language.locale)
                        PreferencesFile.save()
                        forceClose()
                        infoAlert(getString(R.string.language_changed)).showAndWait().ifPresent { exit() }
                    }
                } col 1 row 0 colSpan 2
                label(getString(R.string.employee)) col 0 row 1
                employeeField = textField {
                    promptText = getString(R.string.employee)
                    textProperty() bindBidirectional PreferencesFile.employee
                } col 1 row 1 colSpan 2
                label(getString(R.string.password)) col 0 row 2
                passwordField = passwordField { promptText = getString(R.string.password) } col 1 row 2
                toggleButton {
                    tooltip = kotfx.tooltip(getString(R.string.see_password))
                    graphic = kotfx.imageView { imageProperty() bind (`if`(this@toggleButton.selectedProperty()) then Image(R.png.btn_visibility) `else` Image(R.png.btn_visibility_off)) }
                    passwordField.tooltipProperty() bind bindingOf(passwordField.textProperty(), selectedProperty()) { if (!isSelected) null else tooltip(passwordField.text) }
                } col 2 row 2
            }
            expandableContent = gridPane {
                gap(8)
                label(getString(R.string.server_ip_port)) col 0 row 0
                serverIPField = ipField {
                    promptText = getString(R.string.ip_address)
                    prefWidth = 128.0
                    textProperty() bindBidirectional MySQLFile.ip
                } col 1 row 0
                serverPortField = intField {
                    promptText = getString(R.string.port)
                    prefWidth = 64.0
                    textProperty() bindBidirectional MySQLFile.port
                } col 2 row 0
                label(getString(R.string.server_user)) col 0 row 1
                serverUserField = textField {
                    promptText = getString(R.string.server_user)
                    textProperty() bindBidirectional MySQLFile.user
                } col 1 row 1 colSpan 2
                label(getString(R.string.server_password)) col 0 row 2
                serverPasswordField = passwordField {
                    promptText = getString(R.string.server_password)
                    textProperty() bindBidirectional MySQLFile.password
                } col 1 row 2 colSpan 2
                hbox {
                    alignment = CENTER_RIGHT
                    hyperlink(getString(R.string.test_connection)) {
                        setOnAction {
                            WP.testConnection(serverIPField.text, serverPortField.text, serverUserField.text, serverPasswordField.text)
                                    .multithread()
                                    .subscribeBy({ errorAlert(it.message.toString()).showAndWait() }) { infoAlert(getString(R.string.test_connection_successful)).showAndWait() }
                        }
                    }
                    hyperlink(getString(R.string.about)) { setOnAction { AboutDialog(this@App).showAndWait() } } marginLeft 8
                } col 0 row 3 colSpan 3
            }
            button(CANCEL)
            button(getString(R.string.login), OK_DONE).apply {
                disableProperty() bind (employeeField.textProperty().isEmpty
                        or passwordField.textProperty().isEmpty
                        or not(serverIPField.validProperty)
                        or serverPortField.textProperty().isEmpty
                        or serverUserField.textProperty().isEmpty
                        or serverPasswordField.textProperty().isEmpty)
                addConsumedEventFilter(ACTION) {
                    PreferencesFile.save()
                    MySQLFile.save()
                    WP.login(serverIPField.text, serverPortField.text, serverUserField.text, serverPasswordField.text, employeeField.text, passwordField.text)
                            .multithread()
                            .subscribeBy({ errorAlert(it.message.toString()).showAndWait() }) { employee ->
                                result = employee
                                forceClose()
                            }
                }
            }
            runFX {
                if (employeeField.text.isBlank()) employeeField.requestFocus() else passwordField.requestFocus()
                isExpanded = listOf(serverIPField, serverPortField, serverUserField, serverPasswordField).any { it.text.isBlank() }
                if (DEBUG) {
                    passwordField.text = "123"
                }
            }
        }.showAndWait().filter { it is Employee }.ifPresent { employee ->
            employee as Employee
            EMPLOYEE = employee.id.value
            FULL_ACCESS = employee.fullAccess

            stage.apply {
                title = getString(R.string.app_name)
                scene = getResource(R.fxml.layout_main).loadFXML(resources).pane.toScene()
                minWidth = 960.0
                minHeight = 640.0
            }.show()

            if (employee.firstTimeLogin) dialog<String>(getString(R.string.change_password), getString(R.string.change_password), ImageView(R.png.ic_key)) {
                lateinit var changePasswordField: PasswordField
                lateinit var confirmPasswordField: PasswordField
                content = gridPane {
                    gap(8)
                    label(getString(R.string.password)) col 0 row 0
                    changePasswordField = passwordField { promptText = getString(R.string.password) } col 1 row 0
                    label(getString(R.string.change_password)) col 0 row 1
                    confirmPasswordField = passwordField { promptText = getString(R.string.change_password) } col 1 row 1
                }
                button(CANCEL)
                button(OK).disableProperty() bind (changePasswordField.textProperty().isEmpty
                        or confirmPasswordField.textProperty().isEmpty
                        or (changePasswordField.textProperty() neq confirmPasswordField.textProperty()))
                setResultConverter { if (it == OK) changePasswordField.text else null }
                runFX { changePasswordField.requestFocus() }
            }.showAndWait().filter { it is String }.ifPresent { newPassword ->
                expose {
                    Employees.update({ Employees.id eq EMPLOYEE }) { employee -> employee[password] = newPassword }
                    infoAlert(getString(R.string.change_password_successful)).showAndWait()
                }
            }
        }
    }

    private fun setOSXIcon(url: URL) {
        if (IS_OS_MAC_OSX) Class.forName("com.apple.eawt.Application")
                .newInstance()
                .javaClass
                .getMethod("getApplication")
                .invoke(null)
                .let { application ->
                    application.javaClass
                            .getMethod("setDockIconImage", java.awt.Image::class.java)
                            .invoke(application, getDefaultToolkit().getImage(url))
                }
    }
}