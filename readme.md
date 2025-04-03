This is our README


LEFT TODO
--------------


Small Changes

- End quiz when out of questions
- Have score server side (ClientInfo)

Big Things

- Kill switch for server
- Client Join and Leave as wanted (needs to retain state)
  OPTIONS
  
  - (Best option) It can be based off IP:<PORT> store the IP and PORT in client info for TCP ONLY (I dont think the client is properly assigning the port we give to TCP)
  - usernames
- Keep list of active server **NEEDS TO KNOW IF CLIENT IS ACTIVE**  (Can check based on TCP connected or not maybe)
  - Alternatively hearbeat system server pings client over tcp
- **GUI**

Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
