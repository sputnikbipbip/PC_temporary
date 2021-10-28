import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



/**Utilizar um (vai ser um campo da class) bloco batch com 1 counter 2 message 3 condição (sempre diferente para cada novo pedido)
 * fazer signal para desloquear o método waitForMessage*/


public class MessageBox<T> {

    private static class Request<T> {
        public T message = null;
        public final Condition condition;
        public int counter = 0;

        public Request(Lock lock) {
            condition = lock.newCondition();
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final Condition notFull = monitor.newCondition();
    private final Condition notEmpty = monitor.newCondition();
    private final NodeLinkedList<Request<T>> requests = new NodeLinkedList<>();
    private final NodeLinkedList<T> messages = new NodeLinkedList<>();
    private final int maxRequests;
    private int requestsInQueue = 0;

    public MessageBox(int maxRequests) {
        this.maxRequests = maxRequests;
    }


    /**
       Blocks the current thread until a message is sent by the method sentToAll

       Possíble ways of termination
            -timeout
            -An objects Options that contains the mensage sent
            -throw of an execption of type InterruptedExcepetion
            (in case the thread is interrupted while waiting)

       After calling the function sendToAll, message must be deleted

       @return the message sent
    */

    public Optional<T> waitForMessage(long timeout) throws InterruptedException {
        monitor.lock();
        try {
            /**In case of having messages in the NodeLinkedList messages we should as fast send it, no need to check timeout*/
            if (!messages.isEmpty()) {
                var message = messages.pull();
                return Optional.of(message.value);
            }
            if (timeout == 0)
                return Optional.empty();
            /**Request is inserted in the list until the time given for timeout is achieved*/
            else {
                long deadline = Timeouts.start(timeout);
                long remaining = Timeouts.remaining(deadline);
                var myrequest = requests.enqueue(new Request<>(monitor));
                while (true) {
                    try {
                        myrequest.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        if (myrequest.value.message != null) {
                            Thread.currentThread().interrupt();
                            return Optional.of(myrequest.value.message);
                        }
                        remaining = Timeouts.remaining(deadline);
                        if (Timeouts.noWait(remaining)) {
                            requests.remove(myrequest);
                            return Optional.of(myrequest.value.message);
                        }
                    }
                }
            }
            /**In any case the request is assumed to be send (lost/sent)
             * TODO: better implementation*/
        } finally {
            monitor.unlock();
        }
    }

    /**@return is the number of threads that received the message E [0,1[
     * */

    public int sendToAll(T message) {
        monitor.lock();
        try {
            /**If list requests is empty message can be directly inserted in requests (fast path) */
            if(!requests.isEmpty()) {
                var request = requests.pull();
                request.value.message = message;
                request.value.condition.signal();



                //not sure about this one
                request.value.counter++;





                /**the only case that requests is empty and need to be signaled*/
                notEmpty.signal();
            /**There is space in the list, we just insert the new message*/
            } else if (requestsInQueue < maxRequests){
                messages.enqueue(message);
            /**List messages is full, 'backpression'
             * we need to wait for a signal from @function waitForMessage */
            } else {




                notFull.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            monitor.unlock();
        }
        throw new UnsupportedOperationException("sendToAll return is not done");
    }

    public void broadcast () {

    }
}
