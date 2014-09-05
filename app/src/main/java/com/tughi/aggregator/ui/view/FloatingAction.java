package com.tughi.aggregator.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * An {@link ImageButton} that animates the change of visibility.
 * When initialised, the widget must have the visibility set to {@link #GONE} and
 * {@link #getTranslationX()} and {@link #getTranslationY()} initialized in such a way that the view
 * is not visible.
 */
public class FloatingAction extends ImageButton {

    private final float goneTranslationY;
    private final float goneTranslationX;

    public FloatingAction(Context context, AttributeSet attrs) {
        super(context, attrs);

        int visibility = getVisibility();
        goneTranslationY = visibility == GONE ? getTranslationY() : 0;
        goneTranslationX = visibility == GONE ? getTranslationX() : 0;
    }

    @Override
    public void setVisibility(final int visibility) {
        int currentVisibility = getVisibility();
        if (visibility != currentVisibility) {
            switch (visibility) {
                case VISIBLE:
                    setTranslationX(goneTranslationX);
                    setTranslationY(goneTranslationY);
                    super.setVisibility(VISIBLE);
                    animate()
                            .translationX(0)
                            .translationY(0)
                            .start();
                    break;
                case GONE:
                    setTranslationX(0);
                    setTranslationY(0);
                    animate()
                            .translationX(goneTranslationX)
                            .translationY(goneTranslationY)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    FloatingAction.super.setVisibility(GONE);
                                }
                            })
                            .start();
                    break;
                default:
                    super.setVisibility(INVISIBLE);
            }
        }
    }

}
