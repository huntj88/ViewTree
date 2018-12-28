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
    }

    private var rootView: WeakReference<FrameLayout>? = null
    private val modifyStack = Stack<Modify>()

    fun setRootView(rootView: FrameLayout) {
        this.rootView = WeakReference(rootView)
    }

    fun modifyTree(modify: Modify) {
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

        this.modifyStack.push(modify)
    }

    fun pop() {
        when(val modify = this.modifyStack.pop()) {
            is Modify.Replace -> TODO()
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