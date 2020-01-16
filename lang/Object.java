package java.lang;

/*
 * 所有类的父类，包括数组，都继承了这些方法
 * 作者是unascribed，无归属的~
 */
public class Object {

    private static native void registerNatives();

    static {
        registerNatives();
    }

    /*
     * 返回运行时这个Object的类对象，这个返回值就是sychronized对象锁作用的对象
     */
    public final native Class<?> getClass();

    /**
     * 返回哈希值，这个方法存在的意义就是支持哈希表
     * 一些通用性的协议：
     * 1. 在一个Java应用中对同一个Object多次调用该方法，如果equals方法中用到的
     *    对象信息没有发生改变的话，那该方法也应该应该返回同一个Integer值
     * 2. 如果两个对象的equals方法判断相等，那么hashCode方法也要返回同一个Integer值
     * 3. 如果两个对象的equals方法不相等，其hashCode值不一定非要不相等，但是你要知道，
     *    如果不相等的对象有不同的hashCode值的话，会提高哈希表的性能
     *
     * 默认返回的是内存地址转换成的Integer，但是并不建议这样用
     */
    public native int hashCode();


    /*
     * 判断两个非空对象的相等性。
     * （可以认为null是任何一个类的实例，但是又是任何一个对象的实例，它可以做为引用类参数，但是不能作为基本类参数
     *   null instanctOf Object 语法上没有错误，返回值是false）
     *
     *  对于任意non-null对象
     *  1. reflexive反身性：x.equals(x)一定为true
     *  2. symmetric对称性：当且仅当x.equals(y)==true时，y.equals(x)==true
     *  3. transitive传递性： x.equals(y)==true and y.equals(z)==true 则 x.equals(z)==true
     *  4. consistent一致性： 如果equals方法中的判断信息没有发生改变的话，多次调用该方法，应该返回相同的结果
     *  5. x.equals(null)应该返回false
     *
     *  该方法通常需要被覆盖
     */
    public boolean equals(Object obj) {
        return (this == obj);
    }

    /*
     * 返回对象的一个副本，但是具体的内容还要根据具体的类。
     *
     * 一些通用性的定义：
     * 1. x.clone() != x 为 true
     * 2. x.clone().getClass() == x.getClass() 为 true
     * 但是这并不是必须的
     *
     * 一些典型的实现是：
     * 1. x.clone().equals(x) 为 true
     * 这也不是必须的
     *
     * 通常来讲，我们会调用super.clone(),如果一个类以及它所有的父类（除了Object）都按照这个约定，
     * 那么x.clone().getClass() == x.getClass() 就成立
     *
     * 通常来讲，该方法的返回值应该独立于调用对象（所谓克隆），为了实现这种独立性，在将返回对象返回前，通常需要
     * 对其内部一个或多个字段进行修改。通常，这意味着需要拷贝任何由【对象构成的内部深层数据结构】所组成的不可变对象，
     * 而且将引用指向这些新的备份（就是对引用类型需要进行深拷贝）
     * 如果一个对象只包含基本数据类型的属性，那通常它是不需要更改从super.clone()返回的对象的
     *
     * 该方法提供了一个“拷贝”行为，如果目标类没有实现Cloneable接口，那么会抛出一个CloneNotSupportedException异常
     * 注意，所有的数组都被认为是实现了Cloneable接口，而且对任何基本类型或引用类型T来说，T[]的clone返回的是T[]类型
     *
     * 然而，这个方法常见了一个目标类的新的实例，其字段与原实例的字段值完全相同，但是，是【浅拷贝】
     *
     * Object类没有实现Cloneable接口，所以，如果对一个Object类实例调用该方法，会有运行时异常(你也调用不了，它是protected的)
     */
    protected native Object clone() throws CloneNotSupportedException;

    /*
     * 实例的类名+@+哈希码的16进制形式
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /*
     * 唤醒在该对象的monitor上等待的一个线程，选择会根据具体情况做。线程通过调用对象的wait方法中的一个来的等待对象的monitor
     * 会唤醒的进程不会立即执行，直到当前线程放弃了对象锁。被唤醒的线程会和其它线程一样去抢占锁，
     * 也就是说，被唤醒的线程在抢占该对象monitor的时候并没有什么优势或者劣势
     *
     * 这个方法应该只能被对象monitor的所有线程调用（否则会抛出IllegalMonitorStateException），而一个线程想要称为对象monitor的所有者，可以通过以下三种方式：
     * 1. 使用sychronized实例锁，锁this
     * 2. 使用sychronized实例锁，锁实例方法
     * 3. 使用sychronized类锁
     *
     * 在同一时间只能有一个线程获取同一个对象的monitor
     */
    public final native void notify();

    /*
     * 唤醒在对象monitor上等待的所有线程，且只有持有锁的线程放弃锁的时候才会执行
     * 被唤醒线程平等的争夺锁
     * 如果调用者不是对象monitor的持有者，抛出IllegalMonitorStateException
     */
    public final native void notifyAll();

    /*
     * 使当前线程等待，直到其余线程调用了notify或者notifyAll方法或者过了指定时长
     * 当前线程必须持有对象的monitor
     *
     * 该方法会让当前线程置身于对象的等待集合当中，然后放弃针对该对象的所有synchronization声明（不再争夺锁），
     * 线程将不参与处理机调度并且进入休眠，直到以下四种情况之一发生：
     * 1. 其余线程调用了对象上的notify方法，而且本线程恰巧为天选之线程被唤醒
     * 2. 其余线程调用了对象上的notifyAll方法
     * 3. 其余线程interrupts该线程（只允许线程自己interrupts自己，所以这里可能是抛异常了。等看完Thread再来看看）TODO
     * 4. 过了指定时间。如果时间参数是0，则表示无等待时间，只等着被唤醒
     *
     * 之后，线程会从等待集合中移除，以公平竞争的方式重新参与线程调度，一旦它获取了对象的控制器，那它在该对象上的所有同步声明都会恢复
     * 也就是说，恢复到wait方法被调用前的状态。然后线程从wait方法中返回，对象已经线程的同步状态与线程调用wait方法时一致
     *
     * 除此之外，线程还可以通过别的方式被唤醒，此之谓“spurious（假的） wakeup”，但是这在实际中很少发生，
     * 应用程序必须保证在出现这种情况时，测试其余条件是否满足，如果不满足，应该继续wait，换句话说，wait应该一致发生在循环中，像
     *
     *     synchronized (obj) {
     *         while (condition does not hold)
     *             obj.wait(timeout);
     *         ... // Perform action appropriate to condition
     *     }
     *
     * 如果当前线程在wait之前或者wait时（任意状态？）被别的线程interrupt，会抛出一个InterruptedException
     * 该异常直到对象的锁状态如之前描述的恢复时才抛出
     *
     * 注意，wait方法只会放弃当前对象上的锁，在wait期间内并不会放弃在其它对象上的锁
     *
     * 该方法的调用者只能是对象monitor的所有者
     *
     * @throws IllegalArgumentException     timeout参数为负
     * @throws IllegalMonitorStateException 如果当前线程不是object的monitor的持有者
     * @throws InterruptedException         如果当前线程在wait前或者wait时被别的线程中断。在抛出异常前，线程的中断状态会改变
     */
    public final native void wait(long timeout) throws InterruptedException;

    /*
     * 和wait(long timeout)基本一样，除了提供更精细的控制
     * 最终的时间参数是1000000*timeout+nanos
     */
    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait(timeout);
    }

    /*
     * 显然和wait(long timeout) 是一个东西
     */
    public final void wait() throws InterruptedException {
        wait(0);
    }

    /*
     * 当垃圾回收器在检测是否存在到对象的引用时调用。子类覆盖这个方法类释放资源或者做一些其它的清理工作
     * 关于该方法的一些通用规定:
     * Java虚拟机做可达性分析判定对象不可达的时候（其余对象的finalize方法里的引用不算），此时finalize方法
     * 可能会有一些行为，可能会导致对象再次可达。该方法的通常目的是在对象要被丢弃是做一些处理工作
     *
     * 子类可能需要覆盖该方法
     *
     * Java并不保证那个线程会调用该方法，但是保证，当finalize方法被调用的时候，调用的线程并不会持有任何用户可见的同步锁
     * 如果一个在finalize方法中有未catch的异常，则该异常会被忽略，而且finalize的执行将会结束
     *
     * finalize方法被调用后，在Java虚拟机再次做可达性分析之前，不会再有任何行为。任何对象的finalize方法只会执行一次
     *
     * 抛出的异常会让finalize方法停止
     */
    protected void finalize() throws Throwable {
    }
}
