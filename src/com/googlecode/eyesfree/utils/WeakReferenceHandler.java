
package com.googlecode.eyesfree.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Convenience class for making a static inner {@link Handler} class that keeps
 * a {@link WeakReference} to its parent class. If the reference is cleared, the
 * {@link WeakReferenceHandler} will stop handling {@link Message}s.
 * <p>
 * Example usage:
 * <pre>
 * private final MyHandler mHandler = new MyHandler(this);
 *
 * private static class MyHandler extends WeakReferenceHandler<MyClass> {
 *     protected void handleMessage(Message msg, MyClass parent) {
 *         parent.onMessageReceived(msg.what, msg.arg1);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @param <T> The handler's parent class.
 */
public abstract class WeakReferenceHandler<T> extends Handler {
    private final WeakReference<T> mParentRef;

    /**
     * Constructs a new {@link WeakReferenceHandler} with a reference to its
     * parent class.
     *
     * @param parent The handler's parent class.
     */
    public WeakReferenceHandler(T parent) {
        mParentRef = new WeakReference<T>(parent);
    }

    /**
     * Constructs a new {@link WeakReferenceHandler} with a reference to its
     * parent class.
     *
     * @param parent The handler's parent class.
     * @param looper The looper.
     */
    public WeakReferenceHandler(T parent, Looper looper) {
        super(looper);
        mParentRef = new WeakReference<T>(parent);
    }

    @Override
    public final void handleMessage(Message msg) {
        final T parent = getParent();

        if (parent == null) {
            return;
        }

        handleMessage(msg, parent);
    }

    /**
     * @return The parent class, or {@code null} if the reference has been
     *         cleared.
     */
    protected T getParent() {
        return mParentRef.get();
    }

    /**
     * Subclasses must implement this to receive messages.
     */
    protected abstract void handleMessage(Message msg, T parent);
}
