LEFT TODO
--------------

- Convert/package all classes to JAR files
- Practically done: **GUI**
  - Make textboxes big enough to fit text i.e. "Time: joined I..." & question/option textboxes

CHECK
- just check if server is tracking score properly, should be working but never actually tested


Test Cases
- Client leaving/rejoining at each game-state
- Client WIFI goes does
- Client is the only one to poll then leaves/rejoins
- Server disconnecting intermittently 
- Client joins with same username as other connected client
- First client disconnects after polling, second client who buzzed must be considered the "first"


REVIEW AFTER TODO
--------------

- Cases for server to use kill switch on Client

alt idea for ip:port TCP not binding
serverSocket.setReuseAddress(true);
setReuseAddress(true) is the important bit, idk what our socket would be called

Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
