JMX Shell Interface (jmxsi)
===========================

JMX Shell Interface (jmxsi) is a command line interface JMX client enabling
to access a local or remote JVM to read and change JMX attributes and to
invoke JMX operations.

It supports getting easily composite attributes (such as HeapMemoryUsage) and
bulk getting/setting/invoking on several objects at a time using * in object
name.

Usage
=====

```
jmxsi command [params...]

commands:
- help
- lsobj url objectname [outputformat]
- lsattr url objectname [attrname] [outputformat]
- lsop url objectname [outputformat]
- get url objectname attrname [outputformat]
- set url objectname attrname value [outputformat]
- invoke url objectname operation [-o outputformat] [operationparams]
 
params:
- url: JMX/RMI URL e.g. "service:jmx:rmi:///jndi/rmi://localhost:42/jmxrmi"
- objectname: JMX object name
        e.g.: "java.lang:type=Memory"
              "org.hornetq:module=Core,type=Acceptor,*"
- outputformat: pattern used for output, with RMI object properties variable
                substitution and special variables substitution (see examples
                and default value for special variables)
        e.g.: "%Domain:type=%type,*", "%name"
     default: "%CanonicalName" for lsobj
              "%CompositeAttribute=%Value" for get and set
              "%CompositeAttribute" for lsattr
              "%Result" for invoke
              "%CompositeAttribute: %Type" for lsattr
              "%Operation: %Type" for lsop
- attributename: attribute name or comma-separated enumeration or *
        e.g.: "HeapMemoryUsage"
              "HeapMemoryUsage,NonHeapMemoryUsage"
              "*"
- value
- operation: operation name and parameters signature
        e.g.: "gc()"
              "getThreadUserTime(long)"
              "foobar(java.lang.String,boolean)"
- operationparams
```

Examples
========

Standard Java Examples
----------------------

Getting all java.lang objects list:
```
$ ./jmxsi lsobj "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" "java.lang:*"
java.lang:type=ClassLoading
java.lang:type=Compilation
java.lang:name=Copy,type=GarbageCollector
java.lang:name=MarkSweepCompact,type=GarbageCollector
java.lang:type=Memory
java.lang:name=CodeCacheManager,type=MemoryManager
java.lang:name=Code Cache,type=MemoryPool
java.lang:name=Eden Space,type=MemoryPool
java.lang:name=Perm Gen,type=MemoryPool
java.lang:name=Survivor Space,type=MemoryPool
java.lang:name=Tenured Gen,type=MemoryPool
java.lang:type=OperatingSystem
java.lang:type=Runtime
java.lang:type=Threading
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

Getting all java Runtime attributes:
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" "java.lang:type=Runtime,*" '*'
BootClassPathSupported=true
VmName=OpenJDK 64-Bit Server VM
VmVendor=Oracle Corporation
VmVersion=24.65-b04
LibraryPath=bin
BootClassPath=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/resources.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/rt.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/sunrsasign.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/jsse.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/jce.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/charsets.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/rhino.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/jfr.jar:/usr/lib/jvm/java-7-openjdk-amd64/jre/classes
StartTime=1429520976615
(...)
$
```

Setting java classloading verbosity to true:
```
$ ./jmxsi set "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'java.lang:type=ClassLoading' Verbose true
Verbose=true
$
```

Listing attributes of java Memory object:
```
$ ./jmxsi lsattr "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'java.lang:type=Memory'
HeapMemoryUsage: javax.management.openmbean.CompositeData
NonHeapMemoryUsage: javax.management.openmbean.CompositeData
ObjectName: javax.management.ObjectName
ObjectPendingFinalizationCount: int
Verbose: boolean
$
```

Listing attributes of all standard java objects:
```
$ ./jmxsi lsattr "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'java.lang:*' '*' '%CanonicalName %Attribute: %Type'
java.lang:type=ClassLoading LoadedClassCount: int
java.lang:type=ClassLoading ObjectName: javax.management.ObjectName
java.lang:type=ClassLoading TotalLoadedClassCount: long
java.lang:type=ClassLoading UnloadedClassCount: long
java.lang:type=ClassLoading Verbose: boolean
java.lang:type=Compilation CompilationTimeMonitoringSupported: boolean
java.lang:type=Compilation Name: java.lang.String
java.lang:type=Compilation ObjectName: javax.management.ObjectName
java.lang:type=Compilation TotalCompilationTime: long
java.lang:name=Copy,type=GarbageCollector CollectionCount: long
java.lang:name=Copy,type=GarbageCollector CollectionTime: long
(...)
$
```

Listing operations of Memory object:
```
$ ./jmxsi lsop "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'java.lang:type=Memory'
gc(): void
$
```

Listing operations of all standard java objects:
```
$ ./jmxsi lsop "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'java.lang:*' '%CanonicalName %Operation: %Type'
java.lang:type=Memory gc(): void
java.lang:name=Code Cache,type=MemoryPool resetPeakUsage(): void
java.lang:name=Eden Space,type=MemoryPool resetPeakUsage(): void
java.lang:name=Perm Gen,type=MemoryPool resetPeakUsage(): void
java.lang:name=Survivor Space,type=MemoryPool resetPeakUsage(): void
(...)
$
```

HornetQ Examples
----------------

Getting HornetQ queues list:
```
$ ./jmxsi lsobj "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' %name
"jms.queue.DLQ"
"jms.queue.ExpiryQueue"
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

Getting 4 attributes for every HornetQ queue:
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' 'MessageCount,MessagesAdded,Paused,Durable' %name.%Attribute=%Value
"jms.queue.DLQ".MessageCount=611
"jms.queue.DLQ".MessagesAdded=611
"jms.queue.DLQ".Paused=false
"jms.queue.DLQ".Durable=true
"jms.queue.ExpiryQueue".MessageCount=0
"jms.queue.ExpiryQueue".MessagesAdded=0
"jms.queue.ExpiryQueue".Paused=false
"jms.queue.ExpiryQueue".Durable=true
```

Counting messages in very HornetQ queue:
```
$ ./jmxsi invoke "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' 'countMessages(java.lang.String)' -o '%name: %Result' ''
"jms.queue.DLQ": 611
"jms.queue.ExpiryQueue": 0
$
```

Pause every HornetQ queue:
```
$ ./jmxsi invoke "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' 'pause()'
null
null
$
```

Remove all the messages from an HornetQ queue ("jms.queue.ExpiryQueue"):
```
$ ./jmxsi invoke "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,name="jms.queue.ExpiryQueue",*' 'removeMessages(java.lang.String)' ''
13
$
```

List all messages in an HornetQ queue ("jms.queue.DLQ"):
```
$ ./jmxsi invoke "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,name="jms.queue.DLQ",*' 'listMessagesAsJSON(java.lang.String)' '' | jsonlint --format
[
(...)
  { "address" : "jms.queue.DLQ",
    "durable" : true,
    "expiration" : 0,
    "messageID" : 830,
    "priority" : 4,
    "timestamp" : 1413553824763,
    "type" : 4,
    "userID" : "ID:8b76bd6c-5604-11e4-8715-5d754c477a25"
  },
  { "address" : "jms.queue.DLQ",
    "durable" : true,
    "expiration" : 0,
    "messageID" : 829,
    "priority" : 4,
    "timestamp" : 1413553824766,
    "type" : 4,
    "userID" : "ID:8b77329d-5604-11e4-8715-5d754c477a25"
  }
]
$
```

Statsd / Graphite metrics feeding from HornetQ JMX attributes
-------------------------------------------------------------

Getting 4 attributes for every HornetQ queue, and formating them using statsd
gauge format in a one-line shell command:
```
$ ./jmxsi get "service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi" 'org.hornetq:module=Core,type=Queue,*' 'MessageCount,MessagesAdded,Paused,Durable' "middleware.hornetq.queue.%name.%Attribute:%Value|g" | sed 's/"jms.queue.//;s/"//;s/true|/1|/;s/false|/0|/'
middleware.hornetq.queue.DLQ.Durable:1|g
middleware.hornetq.queue.DLQ.MessageCount:611|g
middleware.hornetq.queue.DLQ.MessagesAdded:611|g
middleware.hornetq.queue.DLQ.Paused:1|g
middleware.hornetq.queue.ExpiryQueue.Durable:1|g
middleware.hornetq.queue.ExpiryQueue.MessageCount:0|g
middleware.hornetq.queue.ExpiryQueue.MessagesAdded:0|g
middleware.hornetq.queue.ExpiryQueue.Paused:1|g
$
```

This command could even be redirected to statsd through
bash's >/dev/udp/server/port redirection or piped to netcat to directly feed
statsd.

Compilation And Packaging
=========================

JMX Shell Interface is not (yet) available with released packages, however,
provided you've got a Unix box with a JDK, it can very easily be built and
installed that way:

```
git clone https://github.com/g76r/jmxsi.git
cd jmxsi
make
```

There are no dependencies apart from the JVM.

Secondary tools
===============

Some higher level specialized tools can be built over jmxsi for exposing a more
human-friendly interface than JMX for specialized JVM processes such as Tomcat
or HornetQ.

This one is provided along with jmxsi:

hornetqsi
---------

```
USAGE:
  hornetqsi queue list
    list all existing JMS queues
  hornetqsi queue pause <queuenames>
    pause one or several queues, messages will become invisible to consumers
    until the queue is resumed
    e.g. hornetqsi queue pause DLQ,ExpiryQueue
  hornetqsi queue resume <queuenames>
    resume one or several queues
    e.g. hornetqsi queue resume DLQ
  hornetqsi queue create <queuenames>
    create new JMS durable queues
    e.g. hornetqsi queue create foo
  hornetqsi queue destroy <queuenames>
    destroy existing JMS queues
    e.g. hornetqsi queue destroy foo
  hornetqsi queue purge <queuenames>
    remove all messages from given JMS queues
    e.g. hornetqsi queue purge ExpiryQueue
  hornetqsi message list [-f <filter>] <queuenames>
    list all messages currently in given JMS queues
    e.g. hornetqsi queue list DLQ
  hornetqsi message first <queuenames>
    list first message currently in given JMS queues
    e.g. hornetqsi message first DLQ
  hornetqsi message count [-f <filter>] [<queuenames>]
    give current message count in given JMS queues, or in every queue if no
    queue name is specified
    e.g. hornetqsi message count
         hornetqsi message count DLQ -f "_HQ_ORIG_QUEUE = 'jms.queue.FOOBAR'"
  hornetqsi message remove [-f <filter>] <queuenames>
    remove messages from given JMS queues
    e.g. hornetqsi message remove FOOBAR
         hornetqsi message remove DLQ -f "_HQ_ORIG_QUEUE = 'jms.queue.FOOBAR'"
  hornetqsi message move [-f <filter>] <queuenames> <targetqueuename>
    move messages from given JMS queues to another queue
    e.g. hornetqsi message move QUEUE1 QUEUE2
         hornetqsi message move DLQ foo -f "_HQ_ORIG_QUEUE = 'jms.queue.foo'"

ENVIRONMENT:
  environment variable HORNETQ_JMX_URL must be set
    e.g. export HORNETQ_JMX_URL="service:jmx:rmi:///jndi/rmi://localhost:5444/jmxrmi"

FILTERS:
  filters specified with -f option follow JMS selectors syntax, which is mainly
  inspired by SQL
  e.g. "HQPriority = 0", "HQPriority BETWEEN 0 AND 3",
    "HQPriority BETWEEN 0 AND 3 AND foobar IS NULL AND HQSize > 1000",
    "_HQ_ORIG_QUEUE = 'jms.queue.TO_AUDIT'"
  see J2EE and HornetQ doc for a more precise reference:
    http://docs.oracle.com/cd/E19798-01/821-1841/bncer/index.html
    https://docs.jboss.org/hornetq/2.4.0.Final/docs/user-manual/html_single/#filter-expressions
```

