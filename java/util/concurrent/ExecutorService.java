
package java.util.concurrent;
import java.util.List;
import java.util.Collection;

/**
 * 这个一个Executor，提供了一些方法来管理任务终止以及创建一个<Future>对象去追踪一个或多个异步任务的执行过程
 *
 * 一个ExecutorService对象可以被shut down，这回让它拒绝接收新的任务
 * 这里提供了两个方法去shut down一个ExecutorService：
 * 1. shutdown：允许之前已经提交的任务继续执行
 * 2. shutdownNow：禁止等待的任务执行，并且尝试终止正在执行的任务
 *
 * 处于termination的exetutor：
 * 1. 没有正在执行的任务
 * 2. 没有等待任务
 * 3. 没有新的可被提交的任务
 * 一个没有被使用的ExecutorService应该被shut down 以允许对它进行资源回收
 *
 * submit方法继承了<Executor>中的execute（Runnable）方法，它会创建并返回一个Future对象，这个对象可以用来取消执行或者等待其运行结束
 * invokeAny方法和invokeAll方法提供了对批量执行的最一般性的用法，它会执行一个任务集合，然后等待至少一个，或者全部执行完毕
 * <ExecutorCompletionService>类可以用来为这些方法定义一些个性化变量
 *
 * <Executors>类为该包中的executor services提供了一些工厂方法
 */
public interface ExecutorService extends Executor {


    /**
     * 已经提交的任务会顺序关闭，但是不会接受新的任务
     * 如果已经shut down了，那么再调用一次不会有什么反应
     *
     * 这个方法不会等待之前提交的任务都执行完毕（已经执行的就继续执行，没开始执行的就算了），awaitTermination方法可以做这样的事情
     */
    void shutdown();

    /**
     * 尝试stop所有正在执行的任务，终止等待任务的进程，并且将那些等待执行的任务放到列表里返回
     * 该方法不会等待正在执行的任务终止（terminate是一个正常流程中的状态），awaitTermination方法可以做这些
     *
     * 不保证以最佳方式stop正在执行的任务，比如说，一个典型的实现会通过Thread.interrupt方法来取消任务，
     * 所以，如果一个任务在被中断时没有响应逻辑，那它可能永远都不能正常终止
     * @return
     */
    List<Runnable> shutdownNow();

    /**
     * 判断一个Executor是否已经被shutdown
     */
    boolean isShutdown();

    /**
     * 如果在调用shut down只会所有的任务都执行完毕了，则返回true
     * 注意，只有先调用了shutdown方法或者shutdownNow方法，该方法才可能返回true
     */
    boolean isTerminated();

    /**
     * 阻塞，直到调用一个shutdown之后所有任务执行完毕，或者超时，或者当前线程被中断
     * 如果executor进入了终止状态则返回true
     * 如果在终止状态前超时，则返回false
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 提交一个带有返回值的任务去执行，并且返回一个Future对象表示这个任务的处理中的结果
     * Future的get方法可以获取任务执行结果，如果任务成功执行完毕的话
     *
     * 如果task是null，则会抛出空指针异常
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Runnable是没有泛型的，所以这里传入一个类型参数，其余的和上面那个一样
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 如果成功了，返回的是null
     */
    Future<?> submit(Runnable task);


    /**
     * 执行给定的任务集合，返回一个Future的列表以获取任务的状态以及结果，该集合中的Future的isDone方法都返回true
     * 注意，一个completed的任务可能是正常结束，也可能是抛异常了
     *
     * 如果任务集合在执行过程中被修改了，那这个方法的结果是undefined
     *
     * 如果一个任务在等待的时候被中断，那会抛出InterruptedException，并且所有未完成的任务都会被取消
     * 如果tasks为空或者其中任一元素为空，则抛出NullPointerException
     * 如果任一任务不能被调度，则抛出RejectedExecutionException
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * 与上一个相比，就是增加了时限，在到达时间限制的时候，未完成的任务都会被取消
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 返回一个成功执行完毕的任务的返回值（比如说没有抛异常的）
     * 如果有了正常或者异常返回，没有完成的任务将会被取消
     * 如果任务集合在运行时被更改，那么这个方法的返回值是undefined
     * @param tasks
     * @param <T>
     * @return
     * @throws InterruptedException 如果任务在等待时被中断
     * @throws ExecutionException 如果没有任务成功完成
     * @throws 如果任务无法被调度执行
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * 与上一个方法相比，多了一个时间限制，以及一个TimeoutException
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if tasks, or unit, or any element
     *         task subject to execution is {@code null}
     * @throws 在时间限制内没有任务成功完成
     * @throws ExecutionException if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *         for execution
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
