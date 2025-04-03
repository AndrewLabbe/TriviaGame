This is our README


LEFT TODO
--------------


Small Changes

- End quiz when out of questions

Big Things

- Kill switch for server
- **GUI**
- Client does not quit if server quits (add graceful quit if client runs b4 server and errors with try/catch)
- If no users are alive then what?

- If packet out of order and the server recieves it after moving to new question, the server needs to skip that packet because it was for an old question
  - packet should include what question its for
 
- Not allow symbols in username (Especially $, only allowed is a-z A-Z 1-9)
- When the client joins middle of question if it joins late it will recieve gamestate and will not know what the question or if it can poll


alt idea for ip:port TCP not binding
serverSocket.setReuseAddress(true);
setReuseAddress(true) is the important bit, idk what our socket would be called



Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
