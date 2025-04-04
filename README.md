LEFT TODO
--------------

- End quiz when out of questions - Partial - still need to process kill/end clients / show end screen or whatever
- Mostly done: **GUI**
  - Make textboxes big enough to fit text i.e. "Time: joined I..." textbox
  - Show correct answer not just if correct or not to all clients
    - IMPLMENTATION:
    ```
    queueSendMessage("correct answer$<id>")
    ```
    and client side in process method:
    ```
    else if (serverMessage.toLowerCase().startsWith("correct answer")) {
    message.split(//$)[1]
    ...
    }
    ```

REVIEW AFTER TODO
--------------

- Cases for server to use kill switch on Client

alt idea for ip:port TCP not binding
serverSocket.setReuseAddress(true);
setReuseAddress(true) is the important bit, idk what our socket would be called

Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
