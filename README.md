# Analyse

Symbolic execution of test programs.

## Dipendenze

Il progetto è un progetto maven, che deve essere installato nel sistema.

L'unica dipendenza esterna necessaria è `z3`, che deve essere disponibile nel path di sistema.

## Esecuzione

È necessario clonare [full teaching](https://github.com/OpenVidu/full-teaching) e compilare il backend ed i test program lanciando:

```
mvn -DskipTests=true clean package
mvn test
```

I test program falliranno, ma le classi compilate saranno a quel punto disponibili.

Dopo aver compilato il codice nella cartella `analyse` di questo repo con `mvn package`, lo si può lanciare come:

`java -cp target/analyse-shaded-1.0-SNAPSHOT.jar it.cnr.saks.sisma.testing.Main <path to test classes> <path to SUT classes> [additional path to add in classpath]` 

Ad esempio, supponendo che il progetto fullteaching sia al livello superiore, rispetto alla cartella in cui si è clonato il progetto, si può lanciare:

`java -cp target/analyse-shaded-1.0-SNAPSHOT.jar it.cnr.saks.sisma.testing.Main ../../full-teaching/target/test-classes/ ../../full-teaching/target/classes/ ./target/analyse-shaded-1.0-SNAPSHOT.jar`

Al termine dell'esecuzione, verrà creato un file `inspection.log` in `<path to test classes>` (nell'esempio di prima in `../../full-teaching/target/test-classes/` che elenca i metodi invocati da ciascun programma di test, ad esempio:

```
Class: com.fullteaching.backend.integration.course.CourseControllerTest
	 Method: delteteCourseTest
		<init>:(Ljava/lang/String;I)V from class com/google/gson/FieldNamingPolicy$1
		<init>:(Ljava/lang/String;ILcom/google/gson/FieldNamingPolicy$1;)V from class com/google/gson/FieldNamingPolicy
		<init>:(Ljava/lang/String;I)V from class com/google/gson/FieldNamingPolicy$4
		<init>:(Ljava/lang/String;I)V from class com/google/gson/LongSerializationPolicy$2
		newCourse:(Ljava/lang/String;Lcom/fullteaching/backend/user/User;Ljava/util/Set;)Lcom/fullteaching/backend/course/Course; from class com/fullteaching/backend/utils/CourseTestUtils
		<init>:(Ljava/lang/String;I)V from class com/google/gson/FieldNamingPolicy$5
		<init>:()V from class java/util/AbstractList
		delteteCourseTest:()V from class com/fullteaching/backend/integration/course/CourseControllerTest
		<init>:(Ljava/lang/String;I)V from class com/google/gson/FieldNamingPolicy$2
		<init>:(Ljava/lang/String;I)V from class com/google/gson/FieldNamingPolicy$3
		<init>:(Ljava/lang/String;I)V from class com/google/gson/LongSerializationPolicy$1
		<init>:()V from class java/lang/Object
		course2JsonStr:(Lcom/fullteaching/backend/course/Course;)Ljava/lang/String; from class com/fullteaching/backend/utils/CourseTestUtils
		getAndAddInt:(Ljava/lang/Object;JI)I from class sun/misc/Unsafe
		getAndAdd:(I)I from class java/util/concurrent/atomic/AtomicInteger
		<init>:()V from class com/google/gson/Gson
		<init>:(Ljava/lang/String;I)V from class com/google/gson/FieldNamingPolicy
		<init>:()V from class java/util/ArrayList
		<init>:(Ljava/lang/String;I)V from class com/google/gson/LongSerializationPolicy
		<init>:()V from class java/util/concurrent/ConcurrentHashMap
		nextHashCode:()I from class java/lang/ThreadLocal
		emptyList:()Ljava/util/List; from class java/util/Collections
		<init>:(Ljava/lang/String;I)V from class java/lang/Enum
		<init>:()V from class java/util/AbstractCollection
		<clinit>:()V from class com/google/gson/FieldNamingPolicy
		<init>:(Ljava/lang/String;Ljava/lang/String;Lcom/fullteaching/backend/user/User;)V from class com/fullteaching/backend/course/Course
		<init>:(Ljava/lang/String;ILcom/google/gson/LongSerializationPolicy$1;)V from class com/google/gson/LongSerializationPolicy
		<init>:()V from class java/util/AbstractSet
		add:(Ljava/lang/Object;)Z from class java/util/ArrayList
		emptyMap:()Ljava/util/Map; from class java/util/Collections
		<init>:(Ljava/util/Map;)V from class com/google/gson/internal/ConstructorConstructor
		<init>:()V from class java/util/HashMap
		<init>:()V from class java/util/AbstractMap
		<init>:(Lcom/google/gson/internal/Excluder;Lcom/google/gson/FieldNamingStrategy;Ljava/util/Map;ZZZZZZZLcom/google/gson/LongSerializationPolicy;Ljava/util/List;)V from class com/google/gson/Gson
		<init>:()V from class java/util/HashSet
		<init>:()V from class java/lang/ThreadLocal
		<clinit>:()V from class com/google/gson/LongSerializationPolicy
		createCourseIfNotExist:(Lorg/springframework/test/web/servlet/MockMvc;Lcom/fullteaching/backend/course/Course;Ljavax/servlet/http/HttpSession;)Lcom/fullteaching/backend/course/Course; from class com/fullteaching/backend/utils/CourseTestUtils
		
[...]
```