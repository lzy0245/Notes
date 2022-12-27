package com.example.notes.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.example.notes.R
import java.util.*


/**
 * 音频条效果
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr), Runnable {
    private var mViewWrapper: Array<ViewWrapper?>? = null
    private var barchartCount = 1
    private var barchartWidth = 20
    private var barchartHeight = 0
    private var barcharMarginLeft = 5
    private var barchartDuration = 500
    private var barcharBackColor = 0
    private var startAnimtor = false

    @DrawableRes
    private var myShape = 0

    init {
        init(context, attrs)
        addBarView()
    }

    /**
     * 初始化配置
     *
     * @param context
     * @param attrs
     */
    private fun init(context: Context, attrs: AttributeSet?) {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BarChartView)
        barchartCount = typedArray.getInt(R.styleable.BarChartView_barchartCount, 0)
        barchartWidth = typedArray.getDimensionPixelSize(R.styleable.BarChartView_barchartWidth, 0)
        barchartHeight =
            typedArray.getDimensionPixelSize(R.styleable.BarChartView_barchartHeight, 0)
        barcharMarginLeft =
            typedArray.getDimensionPixelSize(R.styleable.BarChartView_barcharMarginLeft, 0)
        barchartDuration = typedArray.getInt(R.styleable.BarChartView_barchartDuration, 500)
        myShape = typedArray.getResourceId(R.styleable.BarChartView_barchartShape, 0)
        barcharBackColor = typedArray.getColor(R.styleable.BarChartView_barcharBackColor, Color.RED)
        typedArray.recycle()
    }

    /**
     * add View
     */
    private fun addBarView() {
        if (barchartCount <= 0) {
            return
        }
        mViewWrapper = arrayOfNulls(barchartCount)
        var childView: ImageView
        var layoutParams: LayoutParams
        var viewWrapper: ViewWrapper
        for (i in 0 until barchartCount) {
            childView = ImageView(context)
            if (myShape != 0) {
                childView.setBackgroundResource(myShape)
            } else {
                childView.setBackgroundColor(barcharBackColor)
            }
            layoutParams = LayoutParams(barchartWidth, 10)
            layoutParams.setMargins(barcharMarginLeft, 0, 0, 0)
            childView.layoutParams = layoutParams
            addView(childView)
            viewWrapper = ViewWrapper(childView)
            mViewWrapper!![i] = viewWrapper
        }
    }

    /**
     * 开始动画
     */
    fun start() {
        if (mViewWrapper == null || mViewWrapper!!.size <= 0) {
            return
        }
        startAnimtor = true
        val a = Random()
        for (i in mViewWrapper!!.indices) {
            startAnimator(mViewWrapper!![i], a.nextInt(barchartHeight))
        }
        removeCallbacks(this)
        postDelayed(this, barchartDuration.toLong())
    }

    /**
     * 停止动画
     */
    fun stop() {
        startAnimtor = false
        for (i in mViewWrapper!!.indices) {
            startAnimator(mViewWrapper!![i], 10)
        }
    }

    private fun startAnimator(viewWrapper: ViewWrapper?, height: Int) {
        viewWrapper!!.mTarget.clearAnimation()
        ObjectAnimator.ofInt(viewWrapper, "height", height).setDuration(barchartDuration.toLong())
            .start()
    }

    override fun run() {
        if (startAnimtor) {
            start()
        }
    }

    private class ViewWrapper(var mTarget: View) {

        var width: Int
            get() = mTarget.layoutParams.width
            set(width) {
                mTarget.layoutParams.width = width
                mTarget.requestLayout()
            }
        var height: Int
            get() = mTarget.layoutParams.height
            set(height) {
                mTarget.layoutParams.height = height
                mTarget.requestLayout()
            }
    }
}