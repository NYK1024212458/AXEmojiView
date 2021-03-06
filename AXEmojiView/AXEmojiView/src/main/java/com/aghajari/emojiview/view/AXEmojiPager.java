package com.aghajari.emojiview.view;

import android.content.Context;
import android.graphics.Rect;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.aghajari.emojiview.AXEmojiManager;
import com.aghajari.emojiview.listener.OnEmojiPagerPageChanged;
import com.aghajari.emojiview.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AXEmojiPager extends AXEmojiLayout {
    boolean isShowing=false;
    boolean footer;

    public AXEmojiPager(Context context) {
        super(context);
        init();
    }

    public AXEmojiPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AXEmojiPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    OnEmojiPagerPageChanged pageListener = null;
    public void setOnEmojiPageChangedListener(OnEmojiPagerPageChanged listener){
        this.pageListener = listener;
        if (pageListener!=null && vp!=null && getPagesCount()>0)
            pageListener.onPageChanged(this,pages.get(vp.getCurrentItem()).base,vp.getCurrentItem());
    }

    ViewPager vp;
    private void init(){
        footer = AXEmojiManager.getEmojiViewTheme().isFooterEnabled();

       vp = new ViewPager(getContext()){
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                if (viewPagerTouchEnabled){
                    return  super.onInterceptTouchEvent(event);
                }else {
                    return false;
                }
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (viewPagerTouchEnabled){
                    return  super.onTouchEvent(event);
                }else {
                    return false;
                }
            }
        };

       vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
           @Override
           public void onPageScrolled(int i, float v, int i1) {

           }

           @Override
           public void onPageSelected(int i) {
               pages.get(i).base.onShow();
               if (footer) {
                   scrollListener.show();
                   footerView.setPageIndex(i);
               }
               if (pageListener!=null) pageListener.onPageChanged(AXEmojiPager.this,pages.get(i).base,i);
           }

           @Override
           public void onPageScrollStateChanged(int i) {

           }
       });
    }

    List<AXPage> pages = new ArrayList<AXPage>();
    public void addPage (AXEmojiBase parent,@DrawableRes int iconRes){
        if (vp.getAdapter()!=null) return;
        pages.add(new AXPage(parent,iconRes));
    }
    public void removePage (int index){
        pages.remove(index);
    }
    public AXEmojiBase getPage(int index){
        return pages.get(index).base;
    }
    public @DrawableRes int getPageIcon(int index){
        return pages.get(index).icon;
    }

    public int getPagesCount(){
        return  pages.size();
    }


    private class AXPage{
        AXEmojiBase base;
        int icon;
        public AXPage(AXEmojiBase base,@DrawableRes  int icon){
            this.base = base;
            this.icon = icon;
        }
    }

    public AXFooterParallax scrollListener;

    AXFooterView footerView;

    int Left = -1;

    /**
     * add footer left view
     * NOTE : You should add left icon before call emojiPager.show();
     * @param res
     */
    public void setLeftIcon (@DrawableRes int res){
        Left = res;
    }

    boolean viewPagerTouchEnabled = false;

    public void setSwipeWithFingerEnabled(boolean enabled){
        viewPagerTouchEnabled = enabled;
    }

    public void show(){
        if (isShowing) return;
        isShowing=true;
        if (vp.getParent()!=null){
            ((ViewGroup) vp.getParent()).removeView(vp);
        }
        this.addView(vp,new AXEmojiLayout.LayoutParams());

        vp.setAdapter(new PagerAdapter() {
            public Object instantiateItem(ViewGroup collection, int position) {
                AXEmojiBase base = pages.get(position).base;
                collection.addView(base);
                return  base;
            }

            @Override
            public int getCount() {
                return pages.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view.equals(o);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });

        pages.get(0).base.onShow();

        if (footer) {
            if (footerView != null && footerView.getParent() != null) {
                ((ViewGroup) footerView.getParent()).removeView(footerView);
            }
            footerView = new AXFooterView(getContext(), this, Left);
            footerView.setEditText(editText);
            this.addView(footerView, new AXEmojiLayout.LayoutParams(0, 0, -1, Utils.dpToPx(getContext(), 44)));
        }else if (customFooter!=null){
            if (customFooter != null && customFooter.getParent() != null) {
                ((ViewGroup) customFooter.getParent()).removeView(customFooter);
            }
            this.addView(customFooter,customFooter.getLayoutParams());
        }

        if (footer || (customFooter != null && parallax)) {
            final int f;
            if (customFooter!=null){
                f = (customFooter.getLayoutParams().height + ((AXEmojiLayout.LayoutParams)customFooter.getLayoutParams()).bottomMargin);
            }else{
                f = Utils.dpToPx(getContext(), 44);
            }
            scrollListener = new AXFooterParallax((footer ? footerView : customFooter), f, -1);
            scrollListener.setDuration(f);
            scrollListener.setIDLEHideSize(scrollListener.getDuration() / 2);
            scrollListener.setMinComputeScrollOffset(f);
            scrollListener.setScrollSpeed((long) 1.2);
            scrollListener.setChangeOnIDLEState(true);
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).base.setScrollListener(scrollListener);
            }

            ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int i, float v, int i1) {
                }

                @Override
                public void onPageSelected(int i) {
                    scrollListener.show();
                }

                @Override
                public void onPageScrollStateChanged(int i) {
                }
            };
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).base.setPageChanged(pageChangeListener);
            }
        }

        if (footer || customFooter!=null){
            final int footerHeight;
            if (customFooter!=null){
                footerHeight = customFooter.getLayoutParams().height + (((AXEmojiLayout.LayoutParams)customFooter.getLayoutParams()).bottomMargin*2);
            }else{
                footerHeight = Utils.dp(getContext(),44);
            }

            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).base.addItemDecoration(new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        super.getItemOffsets(outRect, view, parent, state);
                        if (view == null || outRect == null || parent == null) return;
                        int position = parent.getChildAdapterPosition(view);
                        int max = parent.getAdapter().getItemCount();
                        int spanCount = 1;
                        if (parent.getLayoutManager() instanceof GridLayoutManager) {
                            spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();

                            if (max % spanCount == 0) {
                                if ((parent.getAdapter().getItemCount() - position) > spanCount) {
                                    outRect.bottom = 0;
                                } else {
                                    outRect.bottom = footerHeight;
                                }
                            } else if (max % spanCount >= max - position) {
                                if ((parent.getAdapter().getItemCount() - position) > spanCount) {
                                    outRect.bottom = 0;
                                } else {
                                    outRect.bottom = footerHeight;
                                }
                            }
                        } else if (parent.getLayoutManager() instanceof LinearLayoutManager){
                            if (position == max-1){
                                outRect.bottom = footerHeight;
                            }else{
                                outRect.bottom = 0;
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void dismiss(){
        super.dismiss();
        for (int i=0;i<pages.size();i++){
            pages.get(i).base.dismiss();
        }
    }

    @Override
    public void setEditText(EditText editText){
        super.setEditText(editText);
        if (footerView!=null)   footerView.setEditText(editText);
        for (int i=0;i<pages.size();i++){
            pages.get(i).base.setEditText(editText);
        }
    }

    @Override
    protected void refresh(){
        super.refresh();
        if (footer) {
            scrollListener.show();
        }
        for (int i=0;i<pages.size();i++){
            pages.get(i).base.refresh();
        }
    }

    /**
     * @return emoji view pager
     */
    public ViewPager getViewPager(){
        return vp;
    }

    @Override
    public void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if (footer)
        ((LayoutParams) footerView.getLayoutParams()).top = h - ((LayoutParams) footerView.getLayoutParams()).height;
    }

    OnFooterItemClicked listener = null;
    /**
     * set footer left and right (backspace) view click listener
     * @param listener
     */
    public void setOnFooterItemClicked (OnFooterItemClicked listener){
        this.listener = listener;
    }

    public interface OnFooterItemClicked{
        void onClick(View view,boolean leftIcon);
    }


    @Override
    public void setPageIndex (int index){
        vp.setCurrentItem(index,true);
        pages.get(index).base.onShow();
        if (footer) {
            scrollListener.show();
            footerView.setPageIndex(index);
        }
        if (pageListener!=null) pageListener.onPageChanged(AXEmojiPager.this,pages.get(index).base,index);
    }

    @Override
    public int getPageIndex(){
        return vp.getCurrentItem();
    }



    /**
     * get footer left view.
     */
    public AppCompatImageView getFooterLeftView(){
        return footerView.leftIcon;
    }

    /**
     * get footer right view. (backspace)
     */
    public AppCompatImageView getFooterRightView(){
        return footerView.backSpace;
    }

    @Override
    protected void onShow(){
        super.onShow();
        for (int i=0;i<pages.size();i++){
            pages.get(i).base.onShow();
        }
    }
    
    public void setFooterVisible(boolean enabled) {
		scrollListener.starts(enabled);
		scrollListener.setEnabled(enabled);
    }

    public void setBackspaceEnabled(boolean enabled){
        if (footerView!=null) footerView.backspaceEnabled = enabled;
    }

    View customFooter = null;
    boolean parallax;

    public void setCustomFooter(View view,boolean supportParallax){
        this.parallax = supportParallax;
        this.customFooter = view;
        if (view.getLayoutParams()==null || !(view.getLayoutParams() instanceof AXEmojiLayout.LayoutParams)){
            throw new ClassCastException("footer layoutParams must be an instance of AXEmojiLayout.LayoutParams");
        }
    }
}
