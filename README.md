LEFT TODO
--------------

- End quiz when out of questions - Partial - still need to process kill/end clients / show end screen or whatever
- Practically done: **GUI**
  - Make textboxes big enough to fit text i.e. "Time: joined I..." & question/option textboxes

- Server-side print after every question the current leaderboard of clients (preferably sorted)

- Do we check if client is alive before selecting it as first client
  - CASE: client buzzes first then dies and does not rejoin before answer selection, client should be skipped and first client should be selected from alive clients
  - concern is we assign the very first client in queue to be the firstclient and then loop thru and check if timestamp is sooner; If only 1 client do we check if alive before sending "ack", we do check isalive on other clients before assigning as first buzz, but do not check if alive if only 1 client buzzed

CHECK
- just check if server is tracking score properly, should be working but never actually tested

REVIEW AFTER TODO
--------------

- Cases for server to use kill switch on Client

alt idea for ip:port TCP not binding
serverSocket.setReuseAddress(true);
setReuseAddress(true) is the important bit, idk what our socket would be called

Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
