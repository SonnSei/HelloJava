/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;

/**
 * 一个可被线程排他占有的同步器。该类为创建具有所有权信息的锁和相关的同步类器提供了基础。
 * 该类本身并不会管理或者使用这些信息。但是子类和工具类可能会适当的使用一些信息来帮助
 * 管理以及提供检测数据
 */
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {

    private static final long serialVersionUID = 3737899427754241961L;

    /**
     * 空构造器供子类调用
     */
    protected AbstractOwnableSynchronizer() { }

    private transient Thread exclusiveOwnerThread;

    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
