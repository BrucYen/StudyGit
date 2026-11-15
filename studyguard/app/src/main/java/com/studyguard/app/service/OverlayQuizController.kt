package com.studyguard.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.studyguard.app.data.MonitorSettings
import com.studyguard.app.data.QuizQuestion
import com.studyguard.app.ui.overlay.OverlayQuizScreen
import com.studyguard.app.ui.theme.StudyGuardTheme

enum class OverlayMode { QUIZ, DAILY_LIMIT }

data class OverlayPayload(
    val mode: OverlayMode,
    val childName: String,
    val grade: Int,
    val subjects: Set<String>,
    val questionCount: Int,
    val settings: MonitorSettings,
    val questions: List<QuizQuestion>,
    val continuousMinutes: Int,
    val targetMinutes: Long
)

object OverlayQuizController {

    private const val TAG = "OverlayQuiz"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    @Volatile
    var isShowing: Boolean = false
        private set

    fun show(
        context: Context,
        payload: OverlayPayload,
        onQuizPassed: () -> Unit,
        onParentUnlock: () -> Unit,
        onGoLearn: () -> Unit
    ) {
        if (isShowing) return
        if (!Settings.canDrawOverlays(context)) return
        isShowing = true
        val appContext = context.applicationContext
        mainHandler.post {
            try {
                addOverlay(appContext, payload, onQuizPassed, onParentUnlock, onGoLearn)
            } catch (t: Throwable) {
                Log.e(TAG, "添加悬浮窗失败", t)
                isShowing = false
                cleanup()
            }
        }
    }

    fun dismiss() {
        mainHandler.post {
            if (overlayView != null || lifecycleOwner != null) {
                cleanup()
            }
            isShowing = false
        }
    }

    private fun addOverlay(
        context: Context,
        payload: OverlayPayload,
        onQuizPassed: () -> Unit,
        onParentUnlock: () -> Unit,
        onGoLearn: () -> Unit
    ) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val owner = OverlayLifecycleOwner()
        owner.onCreate()

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                StudyGuardTheme {
                    OverlayQuizScreen(
                        payload = payload,
                        onQuizPassed = onQuizPassed,
                        onParentUnlock = onParentUnlock,
                        onGoLearn = onGoLearn
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.55f
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        wm.addView(view, params)
        windowManager = wm
        overlayView = view
        lifecycleOwner = owner
        isShowing = true
    }

    private fun cleanup() {
        val view = overlayView
        val wm = windowManager
        if (view != null && wm != null) {
            runCatching { wm.removeView(view) }
        }
        lifecycleOwner?.onDestroy()
        overlayView = null
        windowManager = null
        lifecycleOwner = null
        isShowing = false
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry
        override val viewModelStore: ViewModelStore = ViewModelStore()

        fun onCreate() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            viewModelStore.clear()
        }
    }
}
