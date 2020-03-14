
public interface Lock {
    /**
     * 获取锁
     * 如果锁处于不可获取状态，当前线程会等待调度，直到成功获取锁
     */
    void lock();

    /**
     * 获取锁，除非当前线程已经被中断
     * 获取锁，如果锁可用，立即返回
     * 如果锁处于不可获取状态，则当前线程不可用，等待调度，直到下面两件事件之一发生：
     * 1. 当前线程获取到锁
     * 2. 其它线程中断了当前线程，且当前线程允许在获取锁时被中断
     * 如果当前线程：
     * 1. 在方法入口时设置了中断状态
     * 2. 其它线程中断了当前线程，且当前线程允许在获取锁时被中断
     * InterruptedException中断异常会被抛出，且当前线程的中断状态会被清除
     * @throws InterruptedException
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 只有在调用时锁可用的状况下才获取锁
     * 获取锁，如果锁可用则立即返回true，如果锁不可用则立即返回false
     * 一个典型的使用情况如下：
     * Lock lokc = ...
     * if(lock.tryLock()){
     *     try{
     *          ...
     *     }finally{
     *         lock.unlock();
     *     }
     * }else{
     *     ...
     * }
     * 这个用法保证了在获取到锁的时候会释放锁，在没有获取到锁的时候也不会尝试去释放锁
     * @return true如果成功获取到锁，否则false
     */
    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁
     * 注意：
     * 一个锁的实现通常会对哪个线程可以释放锁做一些严格的限制（最常见的，只允许锁的持有者释放锁），并且
     * 可能会在违反限制的时候抛出异常。在锁的实现文档中必须指出所有的限制以及异常类型
     */
    void unlock();

    /**
     * 返回一个与该锁实例相关联的Condition实例
     * 在等待condition之前，当前线程必须先持有锁。Condition.await()方法会在等待之前自动释放锁
     * 并且在return之前再去尝试获取锁
     * 注意：
     * Condition实例的具体的操作依赖于锁对象的具体实现，所以其必须在锁实现中详细说明
     * @return
     */
    Condition newCondition();
}
