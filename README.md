This is our README


LEFT TODO
--------------


Small Changes

- End quiz when out of questions - Partial - still need to process kill/end

Big Things

- Cases for server to use kill switch on Client
- Mostly **GUI**
- Not allow symbols in username (Especially $, only allowed is a-z A-Z 1-9)

* NOTE - The in.ready() has been removed from client side in processResponse(), might throw errors

alt idea for ip:port TCP not binding
serverSocket.setReuseAddress(true);
setReuseAddress(true) is the important bit, idk what our socket would be called



Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
