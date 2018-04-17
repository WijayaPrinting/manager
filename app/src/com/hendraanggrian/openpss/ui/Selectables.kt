package com.hendraanggrian.openpss.ui

import javafx.beans.binding.BooleanBinding
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.scene.control.SelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel

/** To avoid repetitive code, this interface contains common operation of selection model. */
interface Selectable<T> {

    val selectionModel: SelectionModel<T>

    val selected: T? get() = selectionModel.selectedItem

    val selectedProperty: ReadOnlyObjectProperty<T> get() = selectionModel.selectedItemProperty()

    val selectedBinding: BooleanBinding get() = selectedProperty.isNotNull
}

/** Some components may have 2 selection models (e.g.: `CustomerController`, `InvoiceController`, etc.). */
interface Selectable2<T> {

    val selectionModel2: SelectionModel<T>

    val selected2: T? get() = selectionModel2.selectedItem

    val selectedProperty2: ReadOnlyObjectProperty<T> get() = selectionModel2.selectedItemProperty()

    val selectedBinding2: BooleanBinding get() = selectedProperty2.isNotNull
}

interface TreeSelectable<T> {

    val selectionModel: TreeTableViewSelectionModel<T>

    val selected: TreeItem<T>? get() = selectionModel.selectedItem

    val selectedProperty: ReadOnlyObjectProperty<TreeItem<T>> get() = selectionModel.selectedItemProperty()

    val selectedBinding: BooleanBinding get() = selectedProperty.isNotNull
}