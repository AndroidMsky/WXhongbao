package com.air.hongbaoauto.AccessibilityService;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;


public class AirAccessibilityService extends AccessibilityService {

    public static boolean ALL = false;
    private List<AccessibilityNodeInfo> parents;
    private boolean auto = false;
    private int lastbagnum;
    String pubclassName;
    String lastMAIN;
    private boolean WXMAIN = false;

    private boolean enableKeyguard = true;//默认有屏幕锁
    private KeyguardManager km;
    private KeyguardManager.KeyguardLock kl;
    //唤醒屏幕相关
    private PowerManager pm;
    private PowerManager.WakeLock wl = null;
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        parents = new ArrayList<>();

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (auto)
            Log.e("AAAAAAAA", "有事件" + eventType);
        switch (eventType) {
            //当通知栏发生改变时
            case 2048:
                pubclassName = event.getClassName().toString();

                Log.e("AAAAAAAA", "有2048事件" + pubclassName + auto);

                if (!auto && pubclassName.equals("android.widget.TextView") && ALL) {
                    Log.e("AAAAAAAA", "有2048事件被识别" + auto + pubclassName);
                    getLastPacket(1);
                }
                if (auto && WXMAIN) {
                    getLastPacket();
                    auto = false;
                }

                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:

                Log.e("AAAAAAAA", "youtongzhi");
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        Log.e("AAAAAAAA", "youtongzhi"+content);
                        if (content.contains("微信红包")) {
                            if (event.getParcelableData() != null &&
                                    event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    auto = true;
                                    wakeAndUnlock2(true);
                                    pendingIntent.send();
                                    Log.e("AAAAAAAA", "进入微信" + auto + event.getClassName().toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //点击最后一个红包
                    Log.e("AAAAAAAA", "点击红包");
                    if (auto)
                        getLastPacket();
                    auto = false;
                    WXMAIN = true;
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    //开红包6.5.3 be_
                    // 6.3.32bdh
                    Log.e("AAAAAAAA", "开红包");
                    click("com.tencent.mm:id/bdh");
                    auto = false;
                    WXMAIN = false;
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //退出红包
                    Log.e("AAAAAAAA", "退出红包");
                    click("com.tencent.mm:id/gq");
                    WXMAIN = false;

                } else {
                    WXMAIN = false;
                    lastMAIN = className;
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void click(String clickId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void getLastPacket() {

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        Log.e("AAAAAAAA", "当前页面红包数老方法" + parents.size());
        if (parents.size() > 0) {
            parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastbagnum = parents.size();
            parents.clear();
        }
    }

    private void getLastPacket(int c) {

        Log.e("AAAAAAAA", "新方法" + parents.size());
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        Log.e("AAAAAAAA", "last++" + lastbagnum + "当前页面红包数" + parents.size());
        if (parents.size() > 0 && WXMAIN) {
            Log.e("AAAAAAAA", "页面大于O且在微信界面");
            if (lastbagnum < parents.size())
                parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastbagnum = parents.size();
            parents.clear();
        }
    }

    public void recycle(AccessibilityNodeInfo info) {
        try {
            if (info.getChildCount() == 0) {
                if (info.getText() != null) {
                    if ("领取红包".equals(info.getText().toString())) {
                        if (info.isClickable()) {
                            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                        AccessibilityNodeInfo parent = info.getParent();
                        while (parent != null) {
                            if (parent.isClickable()) {
                                parents.add(parent);
                                break;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        recycle(info.getChild(i));
                    }
                }
            }
        } catch (Exception e) {


        }
    }
    private void wakeAndUnlock2(boolean b)
    {
        if(b)
        {
            //获取电源管理器对象
            pm=(PowerManager) getSystemService(Context.POWER_SERVICE);

            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");

            //点亮屏幕
            wl.acquire();

            //得到键盘锁管理器对象
            km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
            kl = km.newKeyguardLock("unLock");

            //解锁
            kl.disableKeyguard();
        }
        else
        {
            //锁屏
            kl.reenableKeyguard();

            //释放wakeLock，关灯
            wl.release();
        }

    }

    @Override
    public void onInterrupt() {

    }
}
