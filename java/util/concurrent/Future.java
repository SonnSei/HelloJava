package java.util.concurrent;

/**
 * 代表一个异步计算的结果。包含的方法主要用来检查计算是否完成、等待计算完成以及获取计算结果
 * 计算完成时候的结果只能通过get方法获取，该方法在必要的时候会阻塞，直到它准备好了
 * cancel方法可以用来取消操作
 * 一些附加的方法可以用来检查任务是否被正常执行完毕或取消
 * 一旦一个计算已经结束，那这个计算就不能被取消
 * 如果你只想用一个Future来检查任务是否是可以被取消的，但是却不关心结果，那么你可以把类型声明成Future<?>并且返回一个null当作任务的执行结果
 */
public interface Future<V> {

    /**
     * 尝试取消一个任务。如果任务已经执行完成，或者任务已经被取消，或者当任务处于某些原因无法被删除时，会返回false，
     * 如果任务尚未开始执行，并且又被成功取消了，那么这个任务将不会再运行
     * 如果任务已经开始执行了，那么mayInterruptIfRunning参数将决定任务的执行线程是否应该中断以尝试取消任务
     *
     * 在这个方法执行完毕后，之后对isDone方法的调用都将返回true。
     * 如果该方法返回true，那么之后对isCancelled方法的调用也将返回true
     *
     * @param mayInterruptIfRunning 如果为true，那任务的执行线程就应该被中断，否则的话，执行中的任务将不会被删除
     * @return 如果任务无法被删除，则返回false，通常是因为已经执行完毕了
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * @return 如果任务在正常结束前被取消了，则返回true
     */
    boolean isCancelled();

    /**
     * 任务完成可能是因为正常的终止、异常或者一个删除行为，在这些情况下，该方法都会返回true
     * @return 如果任务完成了就返回true
     */
    boolean isDone();

    /**
     * 如果有必要的话会等待计算完成，然后获取结果
     * @return 计算结果
     * @throws CancellationException 如果任务被取消
     * @throws ExecutionException 如果计算本身抛出一个异常
     * @throws InterruptedException 如果当前线程在等待时被中断
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * 如果有必要的话，会等待计算完成，但是有最长等待时间限制，之后会试着获取计算结果
     *
     * @param timeout 最长等待时间
     * @param unit 时间单位
     * @return 计算结果
     * @throws CancellationException 如果任务被取消
     * @throws ExecutionException 如果计算本身抛出一个异常
     * @throws InterruptedException 如果当前线程在等待时被中断
     * @throws TimeoutException 等待超时
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
