package net.luna.poketools.controllers;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;


import com.omkarmoghe.pokemap.R;

import net.luna.common.util.ScreenUtils;

/**
 * Created by bintou on 15/10/30.
 * 使用objectanimation更加方便和有效率。
 */
public class AnimationCollector {

    Context mContext;
    float screenWidth;
    float screenHeigh;

    public AnimationCollector(Context context) {
        mContext = context;
        screenWidth = ScreenUtils.widthPixels(mContext);
        screenHeigh = ScreenUtils.heightPixels(mContext);
    }


    public void filterLayoutVisitAnim(View upperView,View layout){
        int heigh = mContext.getResources().getDimensionPixelSize(R.dimen.filter_layout_heigh);
        ObjectAnimator uy = ObjectAnimator.ofFloat(upperView, "TranslationY", 0,-heigh).setDuration(600);
        ObjectAnimator y = ObjectAnimator.ofFloat(layout, "TranslationY", heigh, 0).setDuration(600);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(uy, y);
        animatorSet.start();
    }

    public void filterLayoutGoneAnim(View upperView, final View layout){
        int heigh = mContext.getResources().getDimensionPixelSize(R.dimen.filter_layout_heigh);
        ObjectAnimator uy = ObjectAnimator.ofFloat(upperView, "TranslationY",-heigh, 0).setDuration(600);
        ObjectAnimator y = ObjectAnimator.ofFloat(layout, "TranslationY", 0,heigh).setDuration(600);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(uy, y);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                layout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animatorSet.start();
    }




    public void pointVisiable(final View view) {
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f).setDuration(400);
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).setDuration(400);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(sy, sx);
        animatorSet.start();
    }

    public void registerOpenMenuAnimation(View view) {
        android.animation.ObjectAnimator rotateAnimator = android.animation.ObjectAnimator.ofFloat(view, "rotation", 0, 90);
        rotateAnimator.setDuration(150);
        rotateAnimator.start();
    }

    public void registerOpenMenuAnimationTitle(View view) {
        android.animation.ObjectAnimator rotateAnimator = android.animation.ObjectAnimator.ofFloat(view, "alpha", 0f, 1.0f);
        rotateAnimator.setDuration(150);
        rotateAnimator.start();
    }

    public void registerCloseMenuAnimationTitle(View view) {
        android.animation.ObjectAnimator rotateAnimator = android.animation.ObjectAnimator.ofFloat(view, "alpha", 1.0f, 0f);
        rotateAnimator.setDuration(150);
        rotateAnimator.start();
    }

    public void registerCloseMenuAnimation(View view) {
        android.animation.ObjectAnimator rotateAnimator = android.animation.ObjectAnimator.ofFloat(view, "rotation", 90, 0);
        rotateAnimator.setDuration(150);
        rotateAnimator.start();
    }

}
