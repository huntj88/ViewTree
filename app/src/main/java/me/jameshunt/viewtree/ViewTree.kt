package me.jameshunt.viewtree

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import java.lang.ref.WeakReference
import java.util.*

typealias ViewId = Int
typealias LayoutId = Int

object ViewTree {

    sealed class Modify {

        data class Replace(
            val containerViewId: ViewId,
            val replacement: LayoutId
        ) : Modify()

        data class WrapExisting(
            val viewIdToWrap: ViewId,
            val layoutToWrapWith: LayoutId,
            val viewIdToPutOld: ViewId
        ) : Modify()

        data class Overlay(
            val containerViewId: ViewId,
            val layoutId: LayoutId
        ) : Modify()

        var initAction: (view: View) -> Unit = {}
    }

    private var rootView: WeakReference<FrameLayout>? = null
    private val modifyStack = Stack<Modify>()

    fun setRootView(rootView: FrameLayout) {
        this.rootView = WeakReference(rootView)
    }

    fun shouldResume() = this.modifyStack.isNotEmpty()

    fun modifyTree(modify: Modify, pushToStack: Boolean = true, initAction: View.() -> Unit = modify.initAction) {
        modify.initAction = initAction
        val rootView = this.rootView?.get()!!

        when (modify) {
            is Modify.Replace -> modify.replace(rootView = rootView)
            is Modify.WrapExisting -> modify.wrap(rootView = rootView)
            is Modify.Overlay -> modify.overlay(rootView = rootView)
        }

        if (pushToStack) {
            this.modifyStack.push(modify)
        }

        initAction(rootView)
    }

    private fun Modify.Replace.replace(rootView: FrameLayout) {
        when (val view = rootView.findViewById<View>(this.containerViewId)) {
            is FrameLayout -> {
                view.removeAllViews()

                val layoutToAdd = LayoutInflater
                    .from(rootView.context)
                    .inflate(this.replacement, view, false)

                view.addView(layoutToAdd)
            }
            else -> throw IllegalStateException()
        }
    }

    private fun Modify.WrapExisting.wrap(rootView: FrameLayout) {
        when (val view = rootView.findViewById<View>(this.viewIdToWrap)) {
            is FrameLayout -> {
                val childrenViews = (0 until view.childCount).map { view.getChildAt(it) }

                view.removeAllViews()

                val wrapperLayout = LayoutInflater
                    .from(rootView.context)
                    .inflate(this.layoutToWrapWith, view, false)

                val viewPutOld = wrapperLayout.findViewById<FrameLayout>(this.viewIdToPutOld)

                childrenViews.forEach {
                    viewPutOld.addView(it)
                }

                view.addView(wrapperLayout)

            }
            else -> throw IllegalStateException()
        }
    }

    private fun Modify.Overlay.overlay(rootView: FrameLayout) {
        val overlayOnto = rootView.findViewById<FrameLayout>(this.containerViewId)

        val centeringView = RelativeLayout(rootView.context).apply {

            val overlay = LayoutInflater
                .from(rootView.context)
                .inflate(this@overlay.layoutId, this, false)
                .apply {
                    this.layoutParams = RelativeLayout.LayoutParams(this.layoutParams).apply {
                        addRule(RelativeLayout.CENTER_IN_PARENT)
                    }
                }

            this.addView(overlay)

        }

        overlayOnto.addView(centeringView)
    }

    fun pop() {
        when (val modify = this.modifyStack.pop()) {
            is Modify.Replace -> {

                this.modifyStack
                    .reversed()
                    .firstOrNull {
                        when (it) {
                            is Modify.Replace -> it.containerViewId == modify.containerViewId
                            is Modify.WrapExisting -> true
                            is Modify.Overlay -> it.containerViewId == modify.containerViewId
                        }
                    }
                    ?.let { firstModify ->
                        when (firstModify) {
                            is Modify.Replace -> applySubsetOfModifies(containerViewId = firstModify.containerViewId)
                            is Modify.WrapExisting -> applySubsetOfModifies(containerViewId = firstModify.viewIdToWrap)
                            is Modify.Overlay -> applySubsetOfModifies(containerViewId = firstModify.containerViewId)
                        }
                    }
                    ?: this.rootView?.get()!!
                        .findViewById<FrameLayout>(modify.containerViewId)
                        .removeAllViews()
            }
            is Modify.WrapExisting -> {
                val rootView = this.rootView?.get()!!

                val container = rootView.findViewById<FrameLayout>(modify.viewIdToPutOld)

                val containerViews = (0 until container.childCount)
                    .map { container.getChildAt(it) }

                container.removeAllViews()

                val originalPlace = rootView.findViewById<FrameLayout>(modify.viewIdToWrap)

                originalPlace.removeAllViews()

                containerViews.forEach {
                    originalPlace.addView(it)
                }
            }
            is Modify.Overlay -> {
                val rootView = this.rootView?.get()!!
                val container = rootView.findViewById<FrameLayout>(modify.containerViewId)

                val overlay = container.getChildAt(container.childCount - 1)

                container.removeView(overlay)
            }
        }
    }

    private fun applySubsetOfModifies(containerViewId: ViewId) {

        // only go back as far as the containerView being replaced

        val startFromHere = this.modifyStack
            .reversed()
            .filter { it is Modify.Replace }
            .map { it as Modify.Replace }
            .firstOrNull {
                it.containerViewId == containerViewId
            }

        (this.modifyStack.indexOf(startFromHere) until this.modifyStack.size)
            .map { this.modifyStack[it] }
            .forEach {
                modifyTree(it, pushToStack = false)
            }
    }

    fun restore() {

        this.rootView?.get()!!.findViewById<FrameLayout>(R.id.initialFrameLayout).removeAllViews()

        this.applySubsetOfModifies(containerViewId = R.id.initialFrameLayout)
    }
}