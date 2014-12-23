package com.tughi.aggregator.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class ReaderViewPager extends ViewPager {

	public ReaderViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int pageMargin = getPageMargin();
		super.onSizeChanged(w - pageMargin, h, oldw - pageMargin, oldh);
	}

}
