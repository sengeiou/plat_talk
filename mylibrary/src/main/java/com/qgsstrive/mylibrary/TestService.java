package com.qgsstrive.mylibrary;

import android.content.Context;

import com.alibaba.android.arouter.facade.template.IProvider;

public interface TestService extends IProvider {
    public void sendMsg(String uid, String s);
}
