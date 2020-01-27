
package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * 可重入互斥排他锁，与synchronized用的monitor锁有相同的行为和语义，但是具有扩展性
 * 可重入锁由最后成功执行lock操作且尚未释放锁的线程持有。一个线程调用lock方法时，如果没有别的
 * 线程持有锁，那就会立即返回。如果调用的线程已经持有锁，那也会立即返回。这可以通过
 * isHeldByCurrentThread()和getHoldCount()来检查
 *
 * 构造参数接受一个“fairness”参数。如果设置为true，在竞争条件下，锁会倾向于给等待时间最长的线程
 * 否则的话，不保证有任何的获取次序。比起使用默认配置，当程序中多个线程使用公平锁的时候，可能表现出较低的吞吐量，
 * 但在获取锁的时间上变化较小，且不会有线程饥饿。但是要注意，公平锁不保证线程调度的公平。所以，在
 * 使用公平锁的时候，可能有某个线程在其它活动线程并没有在执行或持有锁的时候多次成功的获取到了锁。
 * 同时也要注意，没有超时限制的tryLock（）方法不保证公平锁的设置。当锁可用的时候，即使有其它线程在
 * 等待，它也会成功获取到锁。
 * 建议在lock操作之后紧接着try
 *
 * 除了实现Lock中的方法，本类中还定义了一系列public和protected方法去检测锁的状态。其中的一些方法
 * 只在 instrumentation 和 monitoring 时才有用。
 *
 * 本类的序列化行为和built-in 锁一致：一个反序列化锁处于非锁定状态，在序列化的时候忽略它的状态
 *
 * 本锁支持最多2147483647层递归加锁。如果再加，会抛出Error
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    /**
     * Base of synchronization control for this lock. Subclassed
     * into fair and nonfair versions below. Uses AQS state to
     * represent the number of holds on the lock.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * 表现为Lock.lock()，主要意义是子类快速实现非公平版本
         */
        abstract void lock();

        /**
         * 不公平的tryLock，tryAcquire在子类中实现，但是都需要非公平版本供tryLock方法
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * Sync object for non-fair locks
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * Sync object for fair locks
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获取锁
     * 如果锁没有被其它线程持有，则获取锁并立即返回，将持有锁计数加一
     * 如果当前线程已经持有了锁，计数加一，立即返回
     * 如果锁被其它线程持有，当前线程不可用，等待调度，直到锁获取成功，彼时计数加一
     */
    public void lock() {
        sync.lock();
    }

    /**
     * 获取锁，除非当前线程被中断
     * 如果锁没有被其它线程持有，则获取锁并立即返回，计数器设置成1
     * 如果当前线程已经持有锁，计数器加一，立即返回
     * 如果锁被其它线程持有，当前线程不可用，等待调度，直到一下两件事情之一发生：
     * 1. 当前线程成功获取锁
     * 2. 其余线程中断了当前线程
     *
     * 如果当前线程成功获取到了锁，计数器设置为1
     *
     * 如果当前线程：
     * 1. 在方法入口时设置了中断状态
     * 2. 在获取锁时被中断
     * 会抛出中断异常，并且中断状态清空
     *
     * 在这个实现中，因为这个方法是一个明确的中断点，所以响应更倾向于中断，而非锁的正常获取或冲获取
     * @throws InterruptedException
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     *
     * @return
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 与tryLock不同的是，如果用的是公平锁，只要有别的线程在等待，该方法就不会获取到
     * 如果非要用，这样用：
     * if(lock.tryLock()||lock.tryLock(timeout,unit))
     * @param timeout
     * @param unit
     * @return 如果获取成功了，返回true，获取失败了，包括超时，返回false
     * @throws InterruptedException
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 尝试释放锁
     * 如果当前线程持有锁，计时器减一。如果计数器到了0，锁就会被释放。
     * 如果当前线程不是锁的持有线程，抛出IllegalMonitorStateException
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 返回一个与当前锁一起使用的Condition实例
     * 这个Condition实例支持和在内置锁中使用Object监视器一样的用法（wait，notify，notifyAl）
     * - 当Condition的等待或唤醒方法被调用时，如果没有持有该锁，会抛出抛出IllegalMonitorStateException
     * - 当Condition的等待方法被调用，锁会被释放，在return之前会再次获取，此时的计数器和刚
     * 调用时一致
     * - 如果一个线程在等待的时候被中断，那么等待会终止，抛出中断异常，同时线程的中断状态会被清除
     * - 等待的线程会以FIFO顺序唤醒
     * - 线程从等待状态中return时重新获取锁的顺序与一开始获取锁的顺序一样，在默认参数时没有特别的顺序
     * 但是公平锁的时候会偏向于等待时间较长的线程
     * @return
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程在当前锁上获取的次数
     * 一般测试和debug的时候用
     * @return
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     *
     * @return
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询锁是否被任意线程持有，这个方法是用来监视系统状态的，不是用来做同步的
     * @return
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     *
     * @return
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询是否有线程在等待该锁。注意，cancel操作在任何时候都可能发生，所以，返回true
     * 不保证一定有线程在等待。该方法最初的设计目的是监控系统状态
     * @return
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 查询给出的线程是否在等待该锁注意，cancel操作在任何时候都可能发生，所以，返回true
     * 不保证一定有线程在等待。该方法最初的设计目的是监控系统状态
     * @param thread
     * @return
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 返回大概的数值，设计用来检测系统状态，不是用来同步
     * @return
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 注意返回值并不保证正确性，只是尽力估计。设计给子类用的，方便做更多的扩展
     * @return
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 查询是否有其它线程在给定的Condition上等待，但是注意超时和中断可能随实出现，一个true的返回
     * 并不保证执行唤醒操作会唤醒任何线程
     * @param condition
     * @return
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 估计值
     * 设计目的是检测系统状态，不是同步用
     * @param condition
     * @return
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 注意返回值并不保证正确性，只是尽力估计。设计给子类用的，方便做更多的扩展
     * @param condition
     * @return
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
