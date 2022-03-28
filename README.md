# Bringoz
Bringoz Assignment
I made simple crud endpoints. Using Firebase to store the data.  
The create I made so that it receives a json object and not a bunch of parameters. 
The update receives a json object as well with the format containing one of the json fields of the object (personalInformation, workingHours, status) and every field 
must be filled. For instance one cannot update only the address but he has to reenter his name and age.
The delete and retrieve consist of the document id. 
To receive the active users I made a simple get endpoint.
To receive the drivers in certain map bounds, I chose to utilize latitude and longitude points. I saved the users points and when the location is queried I 
locate all of the drivers in this square. The query consists of 8 parameters ordered as follows:
arg1 - top left longitude
arg2 - top left latitude
arg3 - bottom left longitude
arg4 - bottom left latitude
arg5 - bottom right longitude
arg6 - bottom right latitude
arg7 - bottom  left longitude
arg8 - bottom  left latitude
I debated if to use a body or parameters and went with parameters.
To locate the people working in a time slot. I returned all people who are completely working in this time slot and not only partially. I had to take into account 
cases where the worker starts at night and finishes in the morning, I added 2400 to those workers.

I ran tests on every single endpoint using mvc to mock an enviroment. 
To run it you can run it through the tests. There is no way to run the create option from the url line as it accepts a body.
I have a few items in the database now. I think 4, but each test adds more items. 
Thank you for the oppurtunity to learn spring. It was very helpful and hope to use it more.
Natan Ginsberg


