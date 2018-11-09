package cn.knightxie.recordbutton

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.support.annotation.IntDef
import android.util.AttributeSet
import android.view.View
/**
 * 仿抖音微博视频录制按钮
 *
 * @author Night.X
 * @date 2018/11/8
 */

class RecordButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    /**
     * mOutPaint 外圆画笔
     */
    private var mOutPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /**
     * mOutPaint 内圆画笔
     */
    private var mInPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /**
     * mRectPaint 矩形画笔
     */
    private var mRectPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mOutDrawable: Drawable? = null
    private var mInDrawable:Drawable? = null
    private var mRectDrawable:Drawable? = null

    private var outRadius: Float = 0.toFloat()
    private var inRadius: Float = 0.toFloat()
    private var rectSize: Float = 0.toFloat()
    private var corner: Float = 0.toFloat()

    private var minOutRadius: Float = 0.toFloat()
    private var maxOutRadius: Float = 0.toFloat()
    private var minInRadius: Float = 0.toFloat()
    private var maxInRadius: Float = 0.toFloat()
    private var minRectSize: Float = 0.toFloat()
    private var maxRectSize: Float = 0.toFloat()
    private var minCorner: Float = 0.toFloat()
    private var startOutAlpha: Int = 0xFF
    private var endOutAlpha: Int = 0xFF
    private var startRectAlpha: Int = 0xFF
    private var endRectAlpha: Int = 0xFF

    private var duration = 500
    private var mStartAnimatorSet: AnimatorSet
    private var mEndAnimatorSet: AnimatorSet
    private var mRectF: RectF = RectF()
    private var outAlphaAnim: Boolean = false


    private var outSize: Float = 0.toFloat()
    private var inSize: Float = 0.toFloat()

    private var outAlpha: Int = 0xFF
    private var rectAlpha: Int = 0xFF

    /**
     * onRecordStateChangedListener 状态监听
     */
    private var onRecordStateChangedListener: OnRecordStateChangedListener? = null

    init {
        // 读取配置参数
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.RecordButton, defStyleAttr, 0)
        val count = a.indexCount
        for (i in 0..count) {
            val attr = a.getIndex(i)
            when (attr) {
                R.styleable.RecordButton_min_out_radius -> {
                    minOutRadius = a.getDimension(attr, 0f)
                    outRadius = minOutRadius
                    outSize = outRadius * 2
                }
                R.styleable.RecordButton_max_out_radius -> maxOutRadius = a.getDimension(attr, 0f)
                R.styleable.RecordButton_min_rect_size -> minRectSize = a.getDimension(attr, 0f)
                R.styleable.RecordButton_max_rect_size -> {
                    maxRectSize = a.getDimension(attr, 0f)
                    rectSize = maxRectSize
                    corner = rectSize / 2f
                }
                R.styleable.RecordButton_min_in_radius -> {
                    minInRadius = a.getDimension(attr, 0f)
                    inRadius = minInRadius
                    inSize = inRadius * 2
                }
                R.styleable.RecordButton_max_in_radius -> maxInRadius = a.getDimension(attr, 0f)
                R.styleable.RecordButton_anim_duration -> duration = a.getInt(attr, 500)
                R.styleable.RecordButton_min_corner -> minCorner = a.getDimension(attr, minCorner)
                R.styleable.RecordButton_start_out_alpha -> {
                    startOutAlpha = a.getInt(attr, 0xFF)
                    outAlpha = startOutAlpha
                }
                R.styleable.RecordButton_end_out_alpha -> endOutAlpha = a.getInt(attr, 0xFF)
                R.styleable.RecordButton_out_circle_drawable -> mOutDrawable = a.getDrawable(attr)
                R.styleable.RecordButton_in_circle_drawable -> mInDrawable = a.getDrawable(attr)
                R.styleable.RecordButton_rect_drawable -> mRectDrawable = a.getDrawable(attr)
                R.styleable.RecordButton_out_alpha_anim -> outAlphaAnim = a.getBoolean(attr, false)
                R.styleable.RecordButton_start_rect_alpha -> {
                    startRectAlpha = a.getInt(attr, 0xFF)
                    rectAlpha = startRectAlpha
                }
                R.styleable.RecordButton_end_rect_alpha -> endRectAlpha = a.getInt(attr, 0xFF)
                else -> {
                }
            }
        }
        a.recycle()

        if (mOutDrawable != null) {
            when (mOutDrawable) {
                is BitmapDrawable -> mOutPaint.shader = (mOutDrawable as BitmapDrawable).paint.shader
                is ColorDrawable -> mOutPaint.color = (mOutDrawable as ColorDrawable).color
                is ShapeDrawable -> mOutPaint.set((mOutDrawable as ShapeDrawable).paint)
                is GradientDrawable -> mOutDrawable!!.bounds = Rect()
            }
        }


        if (mInDrawable != null) {
            when (mInDrawable) {
                is BitmapDrawable -> mInPaint.shader = (mInDrawable as BitmapDrawable).paint.shader
                is ColorDrawable -> mInPaint.color = (mInDrawable as ColorDrawable).color
                is ShapeDrawable -> mInPaint.set((mInDrawable as ShapeDrawable).paint)
            }
        }

        if (mRectDrawable != null) {
            when (mRectDrawable) {
                is BitmapDrawable -> mRectPaint.shader = (mRectDrawable as BitmapDrawable).paint.shader
                is ColorDrawable -> mRectPaint.color = (mRectDrawable as ColorDrawable).color
                is ShapeDrawable -> mRectPaint.set((mRectDrawable as ShapeDrawable).paint)
            }
        }


        mRectPaint.style = Paint.Style.FILL
        mRectPaint.alpha = 0
        mInPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        mStartAnimatorSet = AnimatorSet()
        mEndAnimatorSet = AnimatorSet()
        // 启动硬件加速
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val centerX = measuredWidth / 2f
        val centerY = measuredHeight / 2f

        // 1.画外圆
        if (mOutDrawable is GradientDrawable) {
            mOutDrawable!!.alpha = outAlpha
            setDrawableBounds(mOutDrawable!!, width, height, outSize)
            mOutDrawable!!.draw(canvas)
        } else {
            canvas.drawCircle(centerX, centerY, outRadius, mOutPaint)
        }
        // 2.画内圆
        if (mInDrawable is GradientDrawable) {
            setDrawableBounds(mInDrawable!!, width, height, inSize)
            mInDrawable!!.draw(canvas)
        } else {
            canvas.drawCircle(centerX, centerY, inRadius, mInPaint)
        }
        // 3.画矩形
        if (mRectDrawable is GradientDrawable) {
            setDrawableBounds(mRectDrawable!!, width, height, rectSize)
            mRectDrawable!!.alpha = rectAlpha
            (mRectDrawable as GradientDrawable).cornerRadius = corner
            mRectDrawable!!.draw(canvas)
        } else {
            mRectF.left = (measuredWidth - rectSize) / 2f
            mRectF.top = (measuredHeight - rectSize) / 2f
            mRectF.right = (measuredWidth + rectSize) / 2f
            mRectF.bottom = (measuredHeight + rectSize) / 2f
            canvas.drawRoundRect(mRectF, corner, corner, mRectPaint)
        }
    }

    private fun setDrawableBounds(drawable: Drawable, width: Float, height: Float, outSize: Float) {
        drawable.setBounds(
                ((width - outSize) / 2).toInt(),
                ((height - outSize) / 2).toInt(),
                ((width + outSize) / 2).toInt(),
                ((height + outSize) / 2).toInt())
    }

    override fun performClick(): Boolean {
        val result = super.performClick()
        if (mRecordState == STATE_CLICK) {
            stop()
        } else if (mRecordState == STATE_ORIGIN) {
            start()
        }
        mRecordState = mRecordState xor 0x1
        if (onRecordStateChangedListener != null) {
            onRecordStateChangedListener!!.onRecordStateChanged(mRecordState)
        }
        return result
    }

    /**
     * 停止录制动画
     */
    private fun stop() {
        mStartAnimatorSet.cancel()
        val rectCornerAnim = ObjectAnimator.ofFloat(this, "corner",
                corner, maxRectSize / 2f).setDuration(duration.toLong())
        val rectSizeAnim = ObjectAnimator.ofFloat(this, "rectSize",
                rectSize, maxRectSize).setDuration(duration.toLong())
        val outAnim = ObjectAnimator.ofFloat(this, "outRadius",
                outRadius, minOutRadius).setDuration(duration.toLong())
        val inAnim = ObjectAnimator.ofFloat(this, "inRadius",
                inRadius, minInRadius).setDuration(duration.toLong())
        val alphaAnim = ObjectAnimator.ofInt(this, "outAlpha",
                outAlpha, startOutAlpha).setDuration(duration.toLong())
        val rectAlphaAnim = ObjectAnimator.ofInt(this, "rectAlpha",
                rectAlpha, startRectAlpha)
        if (outAlphaAnim) {
            mEndAnimatorSet.playTogether(rectCornerAnim, rectSizeAnim, outAnim, inAnim, alphaAnim, rectAlphaAnim)
        } else {
            mEndAnimatorSet.playTogether(rectCornerAnim, rectSizeAnim, outAnim, inAnim, rectAlphaAnim)
        }
        mEndAnimatorSet.start()
    }

    /**
     * 开始录制动画
     */
    private fun start() {
        val outAnim = ObjectAnimator.ofFloat(this, "outRadius",
                minOutRadius, maxOutRadius).setDuration(duration.toLong())
        val rectSizeAnim = ObjectAnimator.ofFloat(this, "rectSize",
                maxRectSize, minRectSize).setDuration(duration.toLong())
        val rectCornerAnim = ObjectAnimator.ofFloat(this, "corner",
                maxRectSize / 2f, minCorner).setDuration(duration.toLong())
        val rectAlphaAnim = ObjectAnimator.ofInt(this, "rectAlpha",
                startRectAlpha, endRectAlpha)
        val inAnim = ObjectAnimator.ofFloat(this, "inRadius",
                minInRadius, maxInRadius, minInRadius)
                .setDuration((duration * 3).toLong())
        inAnim.repeatCount = ValueAnimator.INFINITE
        val alphaAnim = ObjectAnimator.ofInt(this, "outAlpha",
                startOutAlpha, endOutAlpha, startOutAlpha)
                .setDuration((duration * 3).toLong())
        alphaAnim.repeatCount = ValueAnimator.INFINITE
        if (outAlphaAnim) {
            mStartAnimatorSet.playTogether(outAnim, rectSizeAnim, rectCornerAnim, inAnim, alphaAnim, rectAlphaAnim)
        } else {
            mStartAnimatorSet.playTogether(outAnim, rectSizeAnim, rectCornerAnim, inAnim, rectAlphaAnim)
        }
        mStartAnimatorSet.start()
    }

    fun getOutAlpha(): Int {
        return outAlpha
    }

    fun setOutAlpha(alpha: Int) {
        outAlpha = alpha
        invalidate()
    }

    fun getRectAlpha(): Int {
        return rectAlpha
    }

    fun setRectAlpha(alpha: Int) {
        rectAlpha = alpha
        mRectPaint.alpha = alpha
        invalidate()
    }

    fun getOutRadius(): Float {
        return outRadius
    }

    fun setOutRadius(outRadius: Float) {
        this.outRadius = outRadius
        outSize = outRadius * 2
        invalidate()
    }

    fun getInRadius(): Float {
        return inRadius
    }

    fun setInRadius(inRadius: Float) {
        this.inRadius = inRadius
        inSize = inRadius * 2
        invalidate()
    }

    fun getRectSize(): Float {
        return rectSize
    }

    fun setRectSize(rectSize: Float) {
        this.rectSize = rectSize
        invalidate()
    }

    fun getCorner(): Float {
        return corner
    }

    fun setCorner(corner: Float) {
        this.corner = corner
        invalidate()
    }

    fun setDuration(duration: Int) {
        this.duration = duration
    }

    fun setXfermode(xfermode: Xfermode) {
        this.mInPaint.xfermode = xfermode
    }

    fun getOutSize(): Float {
        return outSize
    }

    fun setOutSize(outSize: Float) {
        this.outSize = outSize
        invalidate()
    }

    fun getInSize(): Float {
        return inSize
    }

    fun setInSize(inSize: Float) {
        this.inSize = inSize
        invalidate()
    }

    fun setOnRecordStateChangedListener(onRecordStateChangedListener: OnRecordStateChangedListener) {
        this.onRecordStateChangedListener = onRecordStateChangedListener
    }

    /**
     * RecordMode 按钮状态
     *
     * @author Night.X
     * @date 2018-11-2 10:38:54
     */
    @IntDef(STATE_ORIGIN, STATE_CLICK)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class RecordState

    companion object {
        /**
         * ORIGIN 按钮为常规状态
         */
        const val STATE_ORIGIN = 0
        /**
         * CLICK 按钮为点击状态
         */
        const val STATE_CLICK = 1
    }

    /**
     * 按钮当前状态，默认为常规状态
     */
    @RecordState
    private var mRecordState = STATE_ORIGIN
}

/**
 * OnRecordStateChangedListener简介
 * 按钮状态监听
 *
 * @author Night.X
 * @date 2018-11-2 13:06:59
 */
interface OnRecordStateChangedListener {
    /**
     * 按钮状态变化回调
     *
     * @param state 当前状态
     */
    fun onRecordStateChanged(@RecordButton.RecordState state: Int)
}