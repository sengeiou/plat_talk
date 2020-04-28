package com.kylindev.totalk.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.kylindev.totalk.view.InterpttNestedAdapter.NestMetadataType;
import com.kylindev.totalk.view.InterpttNestedAdapter.NestPositionMetadata;

public class InterpttNestedListView extends ListView implements OnItemClickListener, OnItemLongClickListener {

    public interface OnNestedChildClickListener {
        public void onNestedChildClick(AdapterView<?> parent, View view, int groupPosition, int childPosition, long id);
    }

    public interface OnNestedChildLongClickListener {
        public void onNestedChildLongClick(AdapterView<?> parent, View view, int groupPosition, int childPosition, long id);
    }

    public interface OnNestedGroupClickListener {
        public void onNestedGroupClick(AdapterView<?> parent, View view, int groupPosition, long id);
    }

    //zcx add
    public interface OnNestedGroupLongClickListener {
        public void onNestedGroupLongClick(AdapterView<?> parent, View view, int groupPosition, long id);
    }

    private InterpttNestedAdapter mNestedAdapter;
    private OnNestedChildClickListener mChildClickListener;
    private OnNestedChildLongClickListener mChildLongClickListener;
    private OnNestedGroupClickListener mGroupClickListener;
    private OnNestedGroupLongClickListener mGroupLongClickListener;

    //private boolean mMaintainPosition;

    public InterpttNestedListView(Context context) {
        this(context, null);
    }

    public InterpttNestedListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnItemClickListener(this);
        //zcx add
        setOnItemLongClickListener(this);
    }

    public void setAdapter(InterpttNestedAdapter adapter) {
        super.setAdapter(adapter);
        mNestedAdapter = adapter;
    }

    public void expandGroup(int groupPosition) {
        mNestedAdapter.expandGroup(groupPosition);
    }

    public void collapseGroup(int groupPosition) {
        mNestedAdapter.collapseGroup(groupPosition);
    }

    public OnNestedChildClickListener getOnChildClickListener() {
        return mChildClickListener;
    }

    public void setOnChildClickListener(
            OnNestedChildClickListener mChildClickListener) {
        this.mChildClickListener = mChildClickListener;
    }

    public OnNestedChildLongClickListener getOnChildLongClickListener() {
        return mChildLongClickListener;
    }

    public void setOnChildLongClickListener(
            OnNestedChildLongClickListener mChildLongClickListener) {
        this.mChildLongClickListener = mChildLongClickListener;
    }

    public OnNestedGroupClickListener getOnGroupClickListener() {
        return mGroupClickListener;
    }

    public void setOnGroupClickListener(
            OnNestedGroupClickListener mGroupClickListener) {
        this.mGroupClickListener = mGroupClickListener;
    }

    public OnNestedGroupLongClickListener getOnGroupLongClickListener() {
        return mGroupLongClickListener;
    }

    public void setOnGroupLongClickListener(
            OnNestedGroupLongClickListener mGroupLongClickListener) {
        this.mGroupLongClickListener = mGroupLongClickListener;
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        if (listener != this)
            throw new RuntimeException(
                    "For InterpttNestedListView, please use the child and group equivalents of setOnItemClickListener.");
        super.setOnItemClickListener(listener);
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (listener != this)
            throw new RuntimeException(
                    "For InterpttNestedListView, please use the child and group equivalents of setOnItemLongClickListener.");
        super.setOnItemLongClickListener(listener);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        NestPositionMetadata metadata = mNestedAdapter.visibleMeta.get(position);
        if (metadata.type == NestMetadataType.META_TYPE_GROUP && mGroupClickListener != null) {
            mGroupClickListener.onNestedGroupClick(parent, view, metadata.groupPosition, id);
        } else if (metadata.type == NestMetadataType.META_TYPE_ITEM && mChildClickListener != null)
            mChildClickListener.onNestedChildClick(parent, view, metadata.groupPosition, metadata.childPosition, id);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        NestPositionMetadata metadata = mNestedAdapter.visibleMeta.get(position);
        if (metadata.type == NestMetadataType.META_TYPE_GROUP && mGroupLongClickListener != null) {
            mGroupLongClickListener.onNestedGroupLongClick(parent, view, metadata.groupPosition, id);
        } else if (metadata.type == NestMetadataType.META_TYPE_ITEM && mChildLongClickListener != null)
            mChildLongClickListener.onNestedChildLongClick(parent, view, metadata.groupPosition, metadata.childPosition, id);

        return true;
    }

}
