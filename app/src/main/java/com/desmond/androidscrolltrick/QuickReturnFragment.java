package com.desmond.androidscrolltrick;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;


/**
 *
 */
public class QuickReturnFragment extends Fragment implements ObservableScrollView.Callbacks {

    public static final String TAG = QuickReturnFragment.class.getSimpleName();

    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;

    private TextView mQuickReturnView;
    private View mPlaceholderView;
    private ObservableScrollView mObservableScrollView;
    private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();
    private int mMinRawY = 0;
    private int mState = STATE_ONSCREEN;
    private int mQuickReturnHeight;
    private int mMaxScrollY;

    public static QuickReturnFragment newInstance() {
        QuickReturnFragment fragment = new QuickReturnFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public QuickReturnFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_quick_return, container, false);

        mObservableScrollView = (ObservableScrollView) rootView.findViewById(R.id.scroll_view);
        mObservableScrollView.setCallbacks(this);

        mQuickReturnView = (TextView) rootView.findViewById(R.id.sticky);
        mQuickReturnView.setText(R.string.quick_return_item);
        mPlaceholderView = rootView.findViewById(R.id.placeholder);

        mObservableScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        onScrollChanged(mObservableScrollView.getScrollY());
                        mMaxScrollY = mObservableScrollView.computeVerticalScrollRange()
                                - mObservableScrollView.getHeight();
                        mQuickReturnHeight = mQuickReturnView.getHeight();
                    }
                });

        return rootView;
    }

    @Override
    public void onScrollChanged(int scrollY) {
        scrollY = Math.min(mMaxScrollY, scrollY);

        mScrollSettleHandler.onScroll(scrollY);

        int rawY = mPlaceholderView.getTop() - scrollY;
        int translationY = 0;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - mQuickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }
        ViewCompat.animate(mQuickReturnView).cancel();
        mQuickReturnView.setTranslationY(translationY + scrollY);
    }

    @Override
    public void onDownMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(false);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        mScrollSettleHandler.setSettleEnabled(true);
        mScrollSettleHandler.onScroll(mObservableScrollView.getScrollY());
    }

    // This class should be made static
    private class ScrollSettleHandler extends Handler {
        private static final int SETTLE_DELAY_MILLIS = 100;

        private int mSettledScrollY = Integer.MIN_VALUE;
        private boolean mSettleEnabled;

        public void onScroll(int scrollY) {
            if (mSettledScrollY != scrollY) {
                // Clear any pending messages and post delayed
                removeMessages(0);
                sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
                mSettledScrollY = scrollY;
            }
        }

        public void setSettleEnabled(boolean settleEnabled) {
            mSettleEnabled = settleEnabled;
        }

        @Override
        public void handleMessage(Message msg) {
            // Handle the scroll settling
            if (STATE_RETURNING == mState && mSettleEnabled) {
                int mDestTranslationY;
                if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
                    mState = STATE_OFFSCREEN;
                    mDestTranslationY = Math.max(
                            mSettledScrollY - mQuickReturnHeight,
                            mPlaceholderView.getTop());
                } else {
                    mDestTranslationY = mSettledScrollY;
                }

                mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight - mDestTranslationY;
                ViewCompat.animate(mQuickReturnView).translationY(mDestTranslationY);
            }
            mSettledScrollY = Integer.MIN_VALUE; // reset
        }
    }
}
