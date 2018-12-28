package me.jameshunt.viewtree

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import java.lang.ref.WeakReference
import java.util.*

typealias ViewId = Int
typealias LayoutId = Int

object ViewTree {

    sealed class Modify {
        data class Replace(val containerViewId: ViewId, val replacement: LayoutId) : Modify()
        data class WrapExisting(val viewIdToWrap: ViewId, val layoutToWrapWith: LayoutId, val viewIdToPutOld: ViewId) :
            Modify()

        data class Overlay(val layoutId: LayoutId) : Modify()

        var initAction: () -> Unit = {}
    }

    private var rootView: WeakReference<FrameLayout>? = null
    private val modifyStack = Stack<Modify>()

    fun setRootView(rootView: FrameLayout) {
        this.rootView = WeakReference(rootView)
    }

    fun modifyTree(modify: Modify, pushToStack: Boolean = true, initAction: () -> Unit = modify.initAction) {
        modify.initAction = initAction
        when (modify) {
            is Modify.Replace -> {
                val rootView = this.rootView?.get()!!

                when (val view = rootView.findViewById<View>(modify.containerViewId)) {
                    is FrameLayout -> {
                        when (view.childCount) {
                            0, 1 -> {
                                view.removeAllViews()

                                val layoutToAdd = LayoutInflater
                                    .from(rootView.context)
                                    .inflate(modify.replacement, view, false)

                                view.addView(layoutToAdd)
                            }
                            else -> throw IllegalStateException()
                        }
                    }
                    else -> throw IllegalStateException()
                }
            }
            is Modify.WrapExisting -> {
                val rootView = this.rootView?.get()!!

                when (val view = rootView.findViewById<View>(modify.viewIdToWrap)) {
                    is FrameLayout -> {
                        val childrenViews = (0 until view.childCount).map { view.getChildAt(it) }

                        view.removeAllViews()

                        val wrapperLayout = LayoutInflater
                            .from(rootView.context)
                            .inflate(modify.layoutToWrapWith, view, false)

                        val viewPutOld = wrapperLayout.findViewById<FrameLayout>(modify.viewIdToPutOld)

                        childrenViews.forEach {
                            viewPutOld.addView(it)
                        }

                        view.addView(wrapperLayout)

                    }
                    else -> throw IllegalStateException()
                }
            }
            is Modify.Overlay -> TODO()
        }

        if (pushToStack) {
            this.modifyStack.push(modify)
        }

        initAction()
    }

    fun pop() {
        /**
         * for wrap existing. find a replace going all the way back to the same root, and build view up from there
         */


        when (val modify = this.modifyStack.pop()) {
            is Modify.Replace -> {

                this.modifyStack
                    .reversed()
                    .firstOrNull {
                        when (it) {
                            is Modify.Replace -> it.containerViewId == modify.containerViewId
                            is Modify.WrapExisting -> true //TODO()
                            is Modify.Overlay -> TODO()
                        }
                    }
                    ?.let { firstModify ->
                        when (firstModify) {
                            is Modify.Replace -> modifyTree(firstModify, pushToStack = false)
                            is Modify.WrapExisting -> {

                                //todo: this section is very much a work in progress

                                val startFromHere = this.modifyStack
                                    .reversed()
                                    .filter { it is Modify.Replace }
                                    .map { it as Modify.Replace }
                                    .firstOrNull {
                                        it.containerViewId == firstModify.viewIdToWrap
                                    }

                                (this.modifyStack.indexOf(startFromHere) until this.modifyStack.size)
                                    .map { this.modifyStack[it] }
                                    .forEach {
                                        modifyTree(it, pushToStack = false)
                                    }
                            }
                            is Modify.Overlay -> TODO()
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
            is Modify.Overlay -> TODO()
        }
    }
}