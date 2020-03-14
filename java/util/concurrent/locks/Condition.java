
package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * 条件将对象监视方法（wait，notify和notifyAll）分解为不同的对象，
 * 从而通过与任意Lock实现结合使用，使每个对象具有多个等待集。
 * Conditions（也成为条件队列或条件变量）提供了一个方法让一个线程停止执行（wait），
 * 直到在某些条件状态为true的时候被别的线程通知。因为对这些共享状态的信息的获取发生在不同
 * 的线程中，所以它必须得到保护，所以该条件会与一个某种形式的锁联系起来。
 * 在一个condition上等待所提供的关键性质是它可以原子性地释放锁，并且暂停当前线程，就像
 * Object.wait方法一样
 *
 * 一个Condition实例本身会绑定一个锁。通过一个锁实例来获取Condition实例时，
 * 用锁的newCondiion()方法
 *
 * 一个Condition的实现可用提供一些区别于对象监视器的方法的行为和语义，比如说可用保证
 * 通知顺序，发出通知的时候不需要持有锁。如果一个实现提供了这些特殊语义，那必须在文档
 * 中说明
 *
 * 注意，Condition实例也是正常的对象，本身也可以作为synchronized声明的目标，也有他们
 * 本身的监视器wait和notification方法，获取Condition实例的监视器锁，或者使用它的
 * 监视器方法，与获取与Condition实例绑定的Lock以及使用它的等待和唤醒方法并没有特殊的联系。
 * 建议为了避免疑惑，永远不要这样使用Condition，他们的内部实现中可能会用到这些用法。
 */
public interface Condition {


    /**
     * 当前线程等待，直到被唤醒或中断
     * 与该Condition实例关联的锁会原子性释放，当前线程不可用，等待调度，直到发生以下4件事情之一：
     * 1. 其它线程调用该Condition实例的signal()方法，且当前线程被选中作为唤醒线程
     * 2. 其它线程调用该Condiiton实例的signalAll()方法
     * 3. 其它线程中断当前线程，且支持“等待中中断”
     * 4. 诡异的唤醒发生
     *
     * 在任何情况下，在本方法return之前，线程必须重新尝试获取与Condition关联的锁。当线程return
     * 保证会持有锁
     *
     * 如果当前线程：
     * 1. 在进入方法时设置了中断状态
     * 2. 支持“等待时中断”且在等待时被中断
     * 则会抛出中断异常，且当前线程的中断状态会被清除。
     * @throws InterruptedException
     */
    void await() throws InterruptedException;

    /**
     * 中断时会继续执行，直到return时才会清除中断状态
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     *
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signalAll();
}
