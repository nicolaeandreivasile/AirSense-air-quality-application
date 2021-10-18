package com.test.bluetoothlowenergyapplication.control.asynchronous.structure;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class BluetoothGattOperationQueue<T> extends PriorityBlockingQueue<T> {

    private final Queue<T> queue;
    private final List<OnBluetoothGattOperationOfferListener<T>> listeners = new LinkedList<>();

    public BluetoothGattOperationQueue(Queue<T> queue,
                                       OnBluetoothGattOperationOfferListener<T> listener) {
        this.queue = queue;
        listeners.add(listener);
    }

    public void registerListener(OnBluetoothGattOperationOfferListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean offer(T t) {
        if (queue.offer(t)) {
            for(OnBluetoothGattOperationOfferListener listener: listeners)
                listener.onBluetoothGattOperationAdd(t);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public T poll() { return queue.poll(); }

    @Override
    public T peek() { return queue.peek(); }

    @Override
    public boolean isEmpty() { return queue.isEmpty(); }

    @Override
    public int size() { return queue.size(); }

    @Override
    public Iterator<T> iterator() { return queue.iterator(); }
}
