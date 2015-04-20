






Examples
========


Getting HornetQ queues list:
```
$ ./jmxsi lsobj "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' %name
"jms.queue.DLQ"
"jms.queue.ExpiryQueue"
$
```

Getting HeapMemoryUsage attribute (which is a CompositeData object):
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" "java.lang:type=Memory" HeapMemoryUsage
HeapMemoryUsage.committed=129761280
HeapMemoryUsage.init=134217728
HeapMemoryUsage.max=259522560
HeapMemoryUsage.used=48607920
$
```

Getting every Java's type=Memory attributes:
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" "java.lang:type=Memory" '*'
Verbose=false
ObjectPendingFinalizationCount=0
HeapMemoryUsage.committed=129761280
HeapMemoryUsage.init=134217728
HeapMemoryUsage.max=259522560
HeapMemoryUsage.used=28799440
NonHeapMemoryUsage.committed=25821184
NonHeapMemoryUsage.init=24313856
NonHeapMemoryUsage.max=224395264
NonHeapMemoryUsage.used=25460376
ObjectName=java.lang:type=Memory
$
```

Getting queue depth for every HornetQ queue:
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' MessageCount %name.%Attribute=%Value
"jms.queue.DLQ".MessageCount=611
"jms.queue.ExpiryQueue".MessageCount=0
$
```

Getting every attribute for every HornetQ queue:
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' '*' %name.%Attribute=%Value
"jms.queue.DLQ".ID=2
"jms.queue.DLQ".Filter=
"jms.queue.DLQ".Paused=false
"jms.queue.DLQ".Temporary=false
"jms.queue.DLQ".DeadLetterAddress=jms.queue.DLQ
"jms.queue.DLQ".ExpiryAddress=jms.queue.ExpiryQueue
"jms.queue.DLQ".Durable=true
"jms.queue.DLQ".MessageCount=611
"jms.queue.DLQ".DeliveringCount=0
"jms.queue.DLQ".ScheduledCount=0
"jms.queue.DLQ".MessagesAdded=611
"jms.queue.DLQ".ConsumerCount=0
"jms.queue.DLQ".FirstMessageAsJSON=[{"timestamp":1413391251357,"userID":"ID:0629a54f-548a-11e4-a609-c336df93b878","messageID":21,"expiration":0,"address":"jms.queue.DLQ","priority":7,"durable":true,"type":4}]
"jms.queue.DLQ".Address=jms.queue.DLQ
"jms.queue.DLQ".Name=jms.queue.DLQ
"jms.queue.ExpiryQueue".ID=4
"jms.queue.ExpiryQueue".Filter=
"jms.queue.ExpiryQueue".Paused=false
"jms.queue.ExpiryQueue".Temporary=false
"jms.queue.ExpiryQueue".DeadLetterAddress=jms.queue.DLQ
"jms.queue.ExpiryQueue".ExpiryAddress=jms.queue.ExpiryQueue
"jms.queue.ExpiryQueue".Durable=true
"jms.queue.ExpiryQueue".MessageCount=0
"jms.queue.ExpiryQueue".DeliveringCount=0
"jms.queue.ExpiryQueue".ScheduledCount=0
"jms.queue.ExpiryQueue".MessagesAdded=0
"jms.queue.ExpiryQueue".ConsumerCount=0
"jms.queue.ExpiryQueue".FirstMessageAsJSON=[{}]
"jms.queue.ExpiryQueue".Address=jms.queue.ExpiryQueue
"jms.queue.ExpiryQueue".Name=jms.queue.ExpiryQueue
$
```

