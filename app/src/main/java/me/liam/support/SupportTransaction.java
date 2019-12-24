package me.liam.support;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.blankj.utilcode.util.ToastUtils;

import java.util.List;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import me.liam.anim.FragmentAnimation;
import me.liam.anim.NoneAnim;
import me.liam.fragmentation.R;
import me.liam.helper.FragmentUtils;
import me.liam.queue.Action;
import me.liam.queue.ActionQueue;

public class SupportTransaction {

    final public static String FRAGMENTATION_CONTAINER_ID = "Fragmentation:ContainerId";
    final public static String FRAGMENTATION_TAG = "Fragmentation:Tag";
    final public static String FRAGMENTATION_SIMPLE_NAME = "Fragmentation:SimpleName";
    final public static String FRAGMENTATION_FULL_NAME = "Fragmentation:FullName";
    final public static String FRAGMENTATION_PLAY_ENTER_ANIM = "Fragmentation:PlayEnterAnim";
    final public static String FRAGMENTATION_INIT_LIST = "Fragmentation:InitList";

    final public static String FRAGMENTATION_ENTER_ANIM_ID = "Fragmentation:EnterAnimId";
    final public static String FRAGMENTATION_EXIT_ANIM_ID = "Fragmentation:ExitAnimId";
    final public static String FRAGMENTATION_POP_ENTER_ANIM_ID = "Fragmentation:PopEnterAnimId";
    final public static String FRAGMENTATION_POP_EXIT_ANIM_ID = "Fragmentation:PopExitAnimId";

    final public static String FRAGMENTATION_SAVED_INSTANCE = "Fragmentation:SavedInstance";

    private ISupportActivity iSupportActivity;

    private FragmentActivity supportActivity;

    private ActionQueue actionQueue;

//    private Context context;

    SupportTransaction(ISupportActivity iSupportActivity) {
        this.iSupportActivity = iSupportActivity;
        this.supportActivity = (FragmentActivity) iSupportActivity;
        actionQueue = new ActionQueue(new Handler(Looper.myLooper()));

    }

    void onBackPressed(FragmentManager fm) {
        if (fm.getFragments().size() > 1){
            pop(fm);
        }else {
            actionQueue.enqueue(new Action() {
                @Override
                public long run() {
                    ActivityCompat.finishAfterTransition(supportActivity);
                    return 0;
                }
            });
        }
    }

    public Bundle getArguments(SupportFragment target){
        if (target.getArguments() == null){
            target.setArguments(new Bundle());
        }
        return target.getArguments();
    }

    private void bindFragmentOptions(SupportFragment target, int containerId, boolean playEnterAnim){
        Bundle args = getArguments(target);
        args.putInt(FRAGMENTATION_CONTAINER_ID, containerId);
        args.putString(FRAGMENTATION_TAG, UUID.randomUUID().toString());
        args.putString(FRAGMENTATION_SIMPLE_NAME, target.getClass().getSimpleName());
        args.putString(FRAGMENTATION_FULL_NAME, target.getClass().getName());
        args.putBoolean(FRAGMENTATION_PLAY_ENTER_ANIM, playEnterAnim);
        args.putBoolean(FRAGMENTATION_INIT_LIST, false);
        args.putBoolean(FRAGMENTATION_SAVED_INSTANCE,false);
    }

    private void supportCommit(FragmentTransaction ft) {
        supportCommit(ft,null);
    }

    private void supportCommit(FragmentTransaction ft,Runnable runnable) {
        if (runnable != null){
            ft.runOnCommit(runnable);
        }
        ft.commitAllowingStateLoss();
    }

    void loadRootFragment(final FragmentManager fm, final int containerId, final SupportFragment to, final FragmentAnimation anim, final boolean playEnterAnim){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                bindFragmentOptions(to,containerId,playEnterAnim);
                to.setFragmentAnimation(anim);
                FragmentTransaction ft = fm.beginTransaction();
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.add(containerId,to);
                supportCommit(ft);
                return 0;
            }
        });
    }

    void loadMultipleRootFragments(final FragmentManager fm, final int containerId, final int showPosition, final SupportFragment... fragments){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                int position = 1;
                FragmentTransaction ft = fm.beginTransaction();
                for (SupportFragment to : fragments){
                    bindFragmentOptions(to,containerId,false);
                    to.setFragmentAnimation(null);
                    ft.add(containerId,to);
                    if (position == showPosition){
                        ft.show(to);
                    }else {
                        ft.hide(to);
                    }
                }
                supportCommit(ft);
                return 0;
            }
        });
    }

    void showHideAllFragment(final FragmentManager fm, final SupportFragment show){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                FragmentTransaction ft = fm.beginTransaction();
                for (SupportFragment f : FragmentUtils.getInManagerFragments(fm)){
                    if (f == show){
                        ft.show(f);
                    }else {
                        ft.hide(f);
                    }
                }
                supportCommit(ft);
                return 0;
            }
        });
    }

    void start(final SupportFragment from, final SupportFragment to){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                FragmentTransaction ft = from.getFragmentManager().beginTransaction();
                bindFragmentOptions(to,from.getContainerId(),true);
                to.setFragmentAnimation(iSupportActivity.getDefaultAnimation());
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.add(to.getContainerId(),to);
                supportCommit(ft);
                return 0;
            }
        });
    }

    void pop(final FragmentManager fm){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                SupportFragment remove = FragmentUtils.getLastFragment(fm);
                if (remove == null) return 0;
                long duration = AnimationUtils.loadAnimation(remove.getContext(),remove.getFragmentAnimation().getExitAnimId()).getDuration();
                FragmentTransaction ft = fm.beginTransaction();
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                ft.remove(remove);
                supportCommit(ft);
                return duration;
            }
        });
    }

    void startWithPop(final SupportFragment from, final SupportFragment to){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                FragmentTransaction ft = from.getFragmentManager().beginTransaction();
                bindFragmentOptions(to,from.getContainerId(),true);
                to.setFragmentAnimation(iSupportActivity.getDefaultAnimation());
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.add(to.getContainerId(),to);
                supportCommit(ft);
                to.setCallBack(new SupportFragmentCallBack(){
                    @Override
                    public void onEnterAnimEnd() {
                        silencePop(from.getFragmentManager(),from);
                    }
                });
                return 0;
            }
        });
    }

    void silencePop(final FragmentManager fm, final SupportFragment... removes){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                FragmentTransaction ft = fm.beginTransaction();
                for (SupportFragment f : removes){
                    ft.remove(f);
                }
                supportCommit(ft);
                return 0;
            }
        });
    }

    void popTo(final FragmentManager fm, final Class cls, final boolean includeTarget){
        actionQueue.enqueue(new Action() {
            @Override
            public long run() {
                SupportFragment remove = FragmentUtils.getLastFragment(fm);
                SupportFragment target = FragmentUtils.findFragmentByClass(fm,cls);
                if (remove == null || target == null) return 0;
                FragmentTransaction ft = fm.beginTransaction();
                int targetIndex = fm.getFragments().indexOf(target);
                int removeIndex = fm.getFragments().indexOf(remove);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                List<Fragment> removeList = fm.getFragments().subList(targetIndex,removeIndex);
                if (!includeTarget){
                    removeList.remove(target);
                }
                for (Fragment f : removeList){
                    if (f instanceof SupportFragment){
                        ft.remove(f);
                    }
                }
                ft.remove(remove);
                supportCommit(ft);
                return 0;
            }
        });
    }
}
