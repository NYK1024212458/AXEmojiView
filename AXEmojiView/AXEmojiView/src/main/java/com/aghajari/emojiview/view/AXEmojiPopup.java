package com.aghajari.emojiview.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.aghajari.emojiview.AXEmojiManager;
import com.aghajari.emojiview.listener.PopupListener;
import com.aghajari.emojiview.utils.EmojiResultReceiver;
import com.aghajari.emojiview.utils.Utils;


import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.O;

public final class AXEmojiPopup implements EmojiResultReceiver.Receiver, AXPopupInterface {
    static final int MIN_KEYBOARD_HEIGHT = 50;

    final View rootView;
    final Activity context;

    final PopupWindow popupWindow;
    AXEmojiBase content;
    final EditText editText;

    boolean isPendingOpen;
    boolean isKeyboardOpen;

    int keyboardHeight;
    int originalImeOptions = -1;

    final EmojiResultReceiver emojiResultReceiver = new EmojiResultReceiver(new Handler(Looper.getMainLooper()));

    final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override @SuppressWarnings("PMD.CyclomaticComplexity") public void onGlobalLayout() {
            updateKeyboardState();
        }
    };

    final View.OnAttachStateChangeListener onAttachStateChangeListener = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(final View v) {
            start();
        }

        @Override public void onViewDetachedFromWindow(final View v) {
            stop();

            popupWindow.setOnDismissListener(null);

            if (SDK_INT < LOLLIPOP) {
                rootView.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
            }

            rootView.removeOnAttachStateChangeListener(this);
        }
    };

    PopupListener listener= null;

    public void setPopupListener(PopupListener listener){
        this.listener = listener;
    }

    public AXEmojiPopup(final AXEmojiBase content) {
        this.context = Utils.asActivity(content.getContext());
        this.rootView = content.getEditText().getRootView();
        this.editText = content.getEditText();
        this.content = content;
        this.keyboardHeight = Utils.getKeyboardHeight(context,0);
        popupWindow = new PopupWindow(context);

        popupWindow.setContentView(content);
        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(context.getResources(), (Bitmap) null)); // To avoid borders and overdraw.
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override public void onDismiss() {
                if (listener!=null) listener.onDismiss();
            }
        });

        if (content instanceof AXEmojiPager){
            if (!((AXEmojiPager) content).isShowing) ((AXEmojiPager) content).show();
        }

        content.setBackgroundColor(AXEmojiManager.getEmojiViewTheme().getBackgroundColor());

        rootView.addOnAttachStateChangeListener(onAttachStateChangeListener);

        if (keyboardHeight>=MIN_KEYBOARD_HEIGHT){
            popupWindow.setHeight(keyboardHeight);
        }
    }

    public PopupWindow getPopupWindow(){
        return popupWindow;
    }

    void updateKeyboardState() {
        final int keyboardHeight = Utils.getInputMethodHeight(context, rootView);
        this.keyboardHeight = keyboardHeight;

        if (keyboardHeight > Utils.dpToPx(context, MIN_KEYBOARD_HEIGHT)) {
            updateKeyboardStateOpened(keyboardHeight);
        } else {
            updateKeyboardStateClosed();
        }
    }

    void start() {
        if (SDK_INT >= LOLLIPOP) {
            context.getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                int previousOffset;

                @Override public WindowInsets onApplyWindowInsets(final View v, final WindowInsets insets) {
                    final int offset;

                    if (insets.getSystemWindowInsetBottom() < insets.getStableInsetBottom()) {
                        offset = insets.getSystemWindowInsetBottom();
                    } else {
                        offset = insets.getSystemWindowInsetBottom() - insets.getStableInsetBottom();
                    }

                    if (offset != previousOffset || offset == 0) {
                        previousOffset = offset;

                        if (offset > Utils.dpToPx(context, MIN_KEYBOARD_HEIGHT)) {
                            updateKeyboardStateOpened(offset);
                        } else {
                            updateKeyboardStateClosed();
                        }
                    }

                    return context.getWindow().getDecorView().onApplyWindowInsets(insets);
                }
            });
        } else {
            rootView.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
    }

    void stop() {
        dismiss();

        if (SDK_INT >= LOLLIPOP) {
            context.getWindow().getDecorView().setOnApplyWindowInsetsListener(null);
        }
    }

    void updateKeyboardStateOpened(final int keyboardHeight) {
        if (popupWindowHeight<=0){
            popupWindowHeight = keyboardHeight;
        }

        if ( popupWindow.getHeight() != popupWindowHeight) {
            popupWindow.setHeight(popupWindowHeight);
        }

        final int properWidth = Utils.getProperWidth(context);

        if (popupWindow.getWidth() != properWidth) {
            popupWindow.setWidth(properWidth);
        }

        Utils.updateKeyboardHeight(context,keyboardHeight);
        if (!isKeyboardOpen) {
            isKeyboardOpen = true;
            if (listener != null) {
                listener.onKeyboardOpened(keyboardHeight);
            }
        }

        if (isPendingOpen) {
            showAtBottom();
        }
    }

    void updateKeyboardStateClosed() {
        isKeyboardOpen = false;

        if (listener != null) {
            listener.onKeyboardClosed();
        }

        if (isShowing()) {
            dismiss();
        }
    }

    int popupWindowHeight = 0;

    public void toggle() {
        AXEmojiManager.setUsingPopupWindow(true);
        if (!popupWindow.isShowing()) {
            // this is needed because something might have cleared the insets listener
            start();
            show();
        } else {
            dismiss();
        }
    }

    public void show() {
        AXEmojiManager.setUsingPopupWindow(true);
        content.refresh();

        if (Utils.shouldOverrideRegularCondition(context, editText) && originalImeOptions == -1) {
            originalImeOptions = editText.getImeOptions();
        }

        editText.setFocusableInTouchMode(true);
        editText.requestFocus();

        showAtBottomPending();
    }



    private void showAtBottomPending() {
        isPendingOpen = true;

        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (Utils.shouldOverrideRegularCondition(context, editText)) {
            editText.setImeOptions(editText.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            if (inputMethodManager != null) {
                inputMethodManager.restartInput(editText);
            }
        }

        if (inputMethodManager != null) {
            emojiResultReceiver.setReceiver(this);
            inputMethodManager.showSoftInput(editText, InputMethodManager.RESULT_UNCHANGED_SHOWN, emojiResultReceiver);
        }
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    public void dismiss() {
        popupWindow.dismiss();
        content.dismiss();


        emojiResultReceiver.setReceiver(null);

        if (originalImeOptions != -1) {
            editText.setImeOptions(originalImeOptions);
            final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

            if (inputMethodManager != null) {
                inputMethodManager.restartInput(editText);
            }

            if (SDK_INT >= O) {
                final AutofillManager autofillManager = context.getSystemService(AutofillManager.class);
                if (autofillManager != null) {
                    autofillManager.cancel();
                }
            }
        }
    }

    void showAtBottom() {
        isPendingOpen = false;
        if (popupWindow.isShowing()) return;
        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0,0);
        if (listener != null) listener.onShow();
    }

    @Override
    public void onReceiveResult(final int resultCode, final Bundle data) {
        if (resultCode == 0 || resultCode == 1) {
            showAtBottom();
        }
    }

}
