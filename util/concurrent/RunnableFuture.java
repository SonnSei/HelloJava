
package java.util.concurrent;

/**
 * 一个Runnable的Future
 * run方法的成功执行会使得Future也完成，并且允许活得其返回结果
 * @param <V> Future的get方法的返回值的结果
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {
    /**
     * 将该Future填入到其运行结果中，除非任务被取消
     */
    void run();
}
