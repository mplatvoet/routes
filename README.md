#Routes
An experiment in Kotlin for registering and mapping http routes.
>This is by no means production ready
I have the follwing goals:

##Simple API
adding routes like:
```kt
routes.add {
    get("/a/b/**") {
      foo()
    }
    post("/a/d/b/") {
      foo()
    }
}
``` 
##Threadsafe
Adding routes should be threadsafe. So multiple threads should be able to add routes even while already using.

##Fast
Should be as fast as possible, so performance over code beauty (hopefully both)

##Memory efficient
Searching and finding routes should produce as little garbage as possible. 


##