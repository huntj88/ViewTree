package me.jameshunt.viewtree

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewTree.setRootView(this.rootViewOfViewTree)

        val modify1 = ViewTree.Modify.Replace(
            containerViewId = R.id.initialFrameLayout,
            replacement = R.layout.view_top_bottom
        )

        ViewTree.modifyTree(modify1)

        this.findViewById<Button>(R.id.button).setOnClickListener {
            val modify2 = ViewTree.Modify.Replace(
                containerViewId = R.id.bottomView,
                replacement = R.layout.view_form
            )

            ViewTree.modifyTree(modify2)

            this.findViewById<Button>(R.id.wrapButton).setOnClickListener {
                val modify3 = ViewTree.Modify.WrapExisting(
                    viewIdToWrap = R.id.initialFrameLayout,
                    layoutToWrapWith = R.layout.view_wrap,
                    viewIdToPutOld = R.id.frameLayoutWrapper
                )

                ViewTree.modifyTree(modify3)


                this.findViewById<Button>(R.id.button3).setOnClickListener {
                    ViewTree.pop()
                }
            }
        }


    }
}
