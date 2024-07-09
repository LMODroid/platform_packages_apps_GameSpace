/*
 * Copyright (C) 2022 crDroid Android Project
 * Copyright (C) 2022 riceDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import java.lang.Runnable;

import io.chaldeaprjkt.gamespace.R;

public class MemoryView extends ProgressBar {

    private ActivityManager mActivityManager;

    private Handler mHandler;
    private MemInfoWorker mWorker;

    public MemoryView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mWorker = new MemInfoWorker();
    }

    /* Hijack this method to detect visibility rather than
     * onVisibilityChanged() because the the latter one can be
     * influenced by more factors, leading to unstable behavior. */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (visibility == VISIBLE)
            mHandler.post(mWorker);
        else
            mHandler.removeCallbacks(mWorker);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setVisibility(VISIBLE);
    }

    @Override
    public void onDetachedFromWindow() {
        setVisibility(GONE);
        super.onDetachedFromWindow();
    }

    private class MemInfoWorker implements Runnable {
        @Override
        public void run() {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            mActivityManager.getMemoryInfo(memoryInfo);
            long usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
            int usedMemoryPercentage = (int) ((usedMemory * 100) / memoryInfo.totalMem);
            setMax(100);
            setProgress(usedMemoryPercentage);
            mHandler.postDelayed(this, 1000);
        }
    }
}