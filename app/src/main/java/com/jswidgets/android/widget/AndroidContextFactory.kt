package com.jswidgets.android.widget

import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

class AndroidContextFactory : ContextFactory() {
    override fun makeContext(): Context {
        val cx = super.makeContext()
        cx.optimizationLevel = -1
        cx.languageVersion = Context.VERSION_1_8
        return cx
    }

    override fun hasFeature(cx: Context, featureIndex: Int): Boolean {
        return when (featureIndex) {
            Context.FEATURE_STRICT_VARS,
            Context.FEATURE_STRICT_EVAL,
            Context.FEATURE_STRICT_MODE -> true
            else -> super.hasFeature(cx, featureIndex)
        }
    }
} 