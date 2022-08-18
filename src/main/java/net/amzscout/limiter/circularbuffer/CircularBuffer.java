package net.amzscout.limiter.circularbuffer;

public class CircularBuffer {

    private final long[] array;

    private final int capacity;
    private int size = 0;
    private int head = 0;
    private int tail = -1;

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        array = new long[capacity];
    }

    public void add(long element) throws CircularBufferException {
        var index = (tail + 1) % capacity;
        size++;
        if (size > capacity) {
            throw new CircularBufferException("Buffer Overflow");
        }
        array[index] = element;
        tail++;
    }

    public long peek() throws CircularBufferException {
        if (size == 0) {
            throw new CircularBufferException("Empty Buffer");
        }
        int index = head % capacity;
        return array[index];
    }

    public void replaceFirst(long element) throws CircularBufferException {
        this.delete();
        this.add(element);
    }

    public boolean isFull() {
        return size == capacity;
    }

    private void delete() throws CircularBufferException {
        if (size == 0) {
            throw new CircularBufferException("Empty Buffer");
        }
        head++;
        size--;
    }
}
