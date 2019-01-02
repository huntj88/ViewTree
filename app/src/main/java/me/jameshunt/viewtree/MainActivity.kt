package me.jameshunt.viewtree

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewTree.setRootView(this.rootViewOfViewTree)

        when (ViewTree.shouldResume()) {
            true -> ViewTree.restore()
            false -> setupTree()
        }
    }

    private fun setupTree() {
        val modify1 = ViewTree.Modify.Replace(
            containerViewId = R.id.initialFrameLayout,
            replacement = R.layout.view_top_bottom
        )

        ViewTree.modifyTree(modify1) {

            this.findViewById<Button>(R.id.button).setOnClickListener {
                val modify2 = ViewTree.Modify.Replace(
                    containerViewId = R.id.bottomView,
                    replacement = R.layout.view_form
                )

                ViewTree.modifyTree(modify2) {

                    this.findViewById<Button>(R.id.wrapButton).setOnClickListener {
                        val modify3 = ViewTree.Modify.WrapExisting(
                            viewIdToWrap = R.id.initialFrameLayout,
                            layoutToWrapWith = R.layout.view_wrap,
                            viewIdToPutOld = R.id.frameLayoutWrapper
                        )

                        ViewTree.modifyTree(modify3) {

                            this.findViewById<Button>(R.id.button2).setOnClickListener {
                                val modify4 = ViewTree.Modify.Replace(
                                    containerViewId = R.id.initialFrameLayout,
                                    replacement = R.layout.view_four
                                )

                                ViewTree.modifyTree(modify4) {

                                    this.findViewById<TextView>(R.id.textView2).setOnClickListener {
                                        val modify5 = ViewTree.Modify.WrapExisting(
                                            viewIdToWrap = R.id.initialFrameLayout,
                                            layoutToWrapWith = R.layout.view_wrap,
                                            viewIdToPutOld = R.id.frameLayoutWrapper
                                        )

                                        ViewTree.modifyTree(modify5) {

                                            this.findViewById<Button>(R.id.button3).setOnClickListener {
                                                val modify6 = ViewTree.Modify.Replace(
                                                    containerViewId = R.id.frameLayoutWrapper,
                                                    replacement = R.layout.view_top_bottom
                                                )


                                                ViewTree.modifyTree(modify6)

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        ViewTree.pop()
    }
}
