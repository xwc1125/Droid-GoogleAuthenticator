package com.xwc1125.droidui.recyclerview.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xwc1125.droidui.recyclerview.listener.DroidItemClickListener;
import com.xwc1125.droidui.recyclerview.listener.DroidItemLongClickListener;
import com.xwc1125.droidui.recyclerview.listener.DroidRecyclerBindViewListener;
import com.xwc1125.droidui.recyclerview.viewholder.DroidRecyclerViewHolder;

import java.util.List;

/**
 * RecyclerView的适配器
 * <p>
 * Created by xwc1125 on 2017/4/27.
 */

public class DroidRecyclerAdapter<T> extends RecyclerView.Adapter<DroidRecyclerViewHolder> {
    private Activity activity;
    private List<T> list;
    private DroidItemClickListener mItemClickListener;
    private DroidItemLongClickListener mItemLongClickListener;
    private int itemLayoutResId;
    private DroidRecyclerBindViewListener bindViewListener;

    /**
     * 初始化适配器
     *
     * @param activity
     * @param list             显示的对象list
     * @param itemLayoutResId  item的layout布局
     * @param bindViewListener 绑定view<br>
     *                         Item item = (Item) list.get(position);<br>
     *                         TextView text1 = (TextView) itemView.findViewById(R.id.text);<br>
     *                         text1.setText(item.desc);<br>
     *                         ImageView img = (ImageView) itemView.findViewById(R.id.img);<br>
     *                         img.setImageResource(item.imgId);<br>
     */
    public DroidRecyclerAdapter(Activity activity, List<T> list, int itemLayoutResId, DroidRecyclerBindViewListener bindViewListener) {
        this.activity = activity;
        this.list = list;
        this.itemLayoutResId = itemLayoutResId;
        this.bindViewListener = bindViewListener;
    }

    @Override
    public DroidRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //绑定一个UI作为Holder 提高性能
        View v = LayoutInflater.from(activity).inflate(itemLayoutResId, parent,false);//必须加上 parent,false，否则只是显示部分
        DroidRecyclerViewHolder holder = new DroidRecyclerViewHolder(v, mItemClickListener, mItemLongClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(DroidRecyclerViewHolder holder, int position) {
        //设置数据
        if (bindViewListener != null) {
            bindViewListener.onBindViewHolder(holder, holder.itemView, list, position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * 设置Item点击监听
     *
     * @param listener
     */
    public void setOnItemClickListener(DroidItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    /**
     * 设置Item长按监听
     *
     * @param listener
     */
    public void setOnItemLongClickListener(DroidItemLongClickListener listener) {
        this.mItemLongClickListener = listener;
    }
}
