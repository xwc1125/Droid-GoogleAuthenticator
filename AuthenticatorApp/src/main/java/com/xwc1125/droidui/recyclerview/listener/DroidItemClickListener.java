package com.xwc1125.droidui.recyclerview.listener;

import android.view.View;

/**
 * item点击事件
 * <p>
 * Created by xwc1125 on 2017/4/27.
 */
public interface DroidItemClickListener {
    /**
     * item点击事件
     *
     * @param view
     * @param postion 位置
     */
    void onItemClick(View view, int postion);
}
