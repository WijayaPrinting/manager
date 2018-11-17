package com.hendraanggrian.openpss.control.dialog

import com.hendraanggrian.openpss.content.Context
import ktfx.layouts.label

class TextDialog(
    context: Context,
    titleId: String,
    content: String = ""
) : Dialog(context, titleId) {

    init {
        label {
            isWrapText = true
            text = content
        }
    }
}