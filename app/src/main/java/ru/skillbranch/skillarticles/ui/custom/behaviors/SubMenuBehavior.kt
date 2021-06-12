package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginRight
import ru.skillbranch.skillarticles.ui.custom.ArticleSubmenu
import ru.skillbranch.skillarticles.ui.custom.BottomBar

class SubMenuBehavior : CoordinatorLayout.Behavior<ArticleSubmenu>() {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: ArticleSubmenu,
        dependency: View
    ): Boolean {
        return dependency is BottomBar
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: ArticleSubmenu,
        dependency: View
    ): Boolean {
        return if (child.isOpen && dependency is BottomBar && dependency.translationY >= 0) {
            animate(child, dependency)
            true
        } else false
    }

    private fun animate(child: View, dependency: View) {
        val fraction = dependency.translationY / dependency.minimumHeight
        Log.e("SubMenuBehavior", "fraction: $fraction translationX: ${child.translationX}")
        child.translationX = (child.width + child.marginRight) * fraction
    }
}