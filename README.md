# play-streak

Streak analytics Dashboard.


## Unicast 

```scala

def stream = withAuth(parse.anyContent) { key => request =>
    val dataStream =  Global.system.actorOf(Props(new DataStream(key)))
    Future {
      val enum = Concurrent.unicast[String](
        channel => dataStream ! Stream(channel),
        dataStream ! Stop,
        (elem, input) => Unit)
      Ok.stream(enum &> EventSource()).as(EVENT_STREAM)
    }
  }
  
```
