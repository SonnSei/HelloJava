

package java.util.concurrent.locks;
import sun.misc.Unsafe;

/**
 * 基本的线程阻塞原语，供创建锁和其它的同步类使用
 * 这个类以及所有使用它的线程与一个permit关联，
 * 调用park方法时，如果permit可用，则会立即返回consuming it in the process，否则会阻塞
 * 调用unpark方法时，如果permit处于不可用状态，会使permit可用
 * permits不累加，最多一个
 *
 * park和unpark方法提供了高效的途径去阻塞和唤醒线程，而且不会遇到如下问题：
 * Thread.suspend和Thread.resume方法在以下情况会不可用：
 * 两个线程存在竞争，一个要park，另一个要unpark，it will preserve liveness, due to the permit
 *
 * 另外，park方法在调用线程被中断的时候会返回，并且有timeout的版本，park方法可能在别的时候很诡异的返回
 * 所以，通常需要在循环中调用它来重现检查是否满足return的条件。在这种情况下，park方法就好像一个“忙等”的优化，
 * 不需要像自旋那样浪费那么多时间，但是必须和unpark搭配食用才有效
 *
 * park方法的三种重载中都支持一个blocker参数。这个对象被记录下来，当线程阻塞的时候，一些检测和诊断工具通过
 * 它来确定线程阻塞的原因。（这些工具可能通过getBlocker（Thread）方法来获取这个blocker）
 *
 * 非常建议用这些带blocker参数的方式调用，而不是用最原始的方式
 * 在一个锁的实现中，通常应用blocker的方式是用this
 *
 * 这些方法被设计用来当作创建一个写高层的同步工具的工具，他们本身在同步控制应用中并不是非常有用
 * park方法唯一的设计目标是在以下的构建方式中：
 * while (!canProceed()) { ... LockSupport.park(this); }}
 *
 *

 * 像canProceed或者其它的比park优先的方法并不会引起locking或者blocking，因为一个线程只联系一个permit
 * 所有中间过程的对park的调用都会影响其预期的结果
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}</pre>
 */
public class LockSupport {
    private LockSupport() {} // 禁止创建实例

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        // 尽管是volatile，hotspot并不需要一个写屏障在这里
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * 如果permit当前不可用，则将线程的permit标记为avaliable
     * 如果线程处于park的阻塞状态中，那它将从阻塞状态中恢复
     * 否则，线程的下一次park操作将保证不会发生阻塞。
     *
     * 在目标线程尚未started的时候，该方法不保证会产生任何作用
     *
     * @param 需要被unpark的线程。如果是null，则该方法不会有任何作用
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * 处于线程调度的目的，将当前线程置为不可用，除非permit处于可用状态
     *
     * 如果permit可用，那么线程继续，并且方法立即返回，否则，当前线程不可用，等待线程调度，
     * 直到发生以下三件事情之一：
     * 1. 其余线程把当前线程当作unpark的对象调用
     * 2. 其余线程对当前线程调用Thread.interrupt
     * 3. 玄学return（之前说的park方法可能无理由的return）
     *
     * 该方法并不指出是什么原因导致的return。调用者需要重现检查条件
     * 调用者也可能通过return来检测线程的中断状态
     *
     * @param 负责该线程park的同步对象
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }


    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }


    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * Returns the blocker object supplied to the most recent
     * invocation of a park method that has not yet unblocked, or null
     * if not blocked.  The value returned is just a momentary
     * snapshot -- the thread may have since unblocked or blocked on a
     * different blocker object.
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of three
     * things happens:
     *
     * <ul>
     *
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.
     * Copied from ThreadLocalRandom due to package access restrictions.
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}
